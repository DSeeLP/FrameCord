/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin.repository

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.PathQualifiers
import de.dseelp.kotlincord.api.Version
import de.dseelp.kotlincord.api.plugins.repository.Package
import de.dseelp.kotlincord.api.plugins.repository.PackageVersion
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.core.CordBootstrap
import de.dseelp.kotlincord.core.FakePlugin.database
import de.dseelp.kotlincord.core.plugin.repository.data.InstalledPackage
import de.dseelp.kotlincord.core.plugin.repository.data.InstalledPackageDTO
import de.dseelp.kotlincord.core.utils.downloadFile
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.inject
import kotlin.io.path.div
import kotlin.io.path.exists

@InternalKotlinCordApi
@Serializable
data class PackageImpl(
    override val groupId: String,
    override val artifactId: String,
    override val authors: String,
    override val versions: Array<PackageVersion>
) : Package<PackageVersion>, CordKoinComponent {
    @Transient
    lateinit var repository: RepositoryImpl

    val repositoryManager: RepositoryManagerImpl by inject()

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun install(version: Version, update: Boolean, returnImmediately: Boolean) {
        if (!this::repository.isInitialized) throw IllegalArgumentException("The repository is not yet initialized!")
        val packageVersion = findVersion(version)
        if (packageVersion == null) {
            repository.log.error("There is no version $version of the package $groupId$artifactId")
            return
        }
        if (!isCoreVersionSupported(version)) {
            repository.log.error("The version of the package $groupId$artifactId $version is not supported by the current KotlinCord version (${CordBootstrap.version})")
            val firstSupported = versions.copyOfRange(versions.indexOf(packageVersion), versions.lastIndex)
                .firstOrNull { isCoreVersionSupported(packageVersion) }
            if (firstSupported == null) {
                repository.log.error("There is no version of this package for the current KotlinCord version (${CordBootstrap.version})")
            } else {
                repository.log.error("The first version that supports this KotlinCord version is ${firstSupported.version}")
            }
        }
        val lazyFailMessage = { "Failed to install the package $groupId:$artifactId@$version" }
        if (isInstalled) {
            repository.log.error("${lazyFailMessage()} it is already installed")
        }
        val fileName = "$artifactId-$version.jar"
        val filePath = PathQualifiers.PLUGIN_LOCATION / fileName
        if (filePath.exists()) {
            repository.log.error("${lazyFailMessage.invoke()} the file $filePath already exists!")
            return
        }
        val file = filePath.toFile()
        repository.log.info("Installing package $groupId:$artifactId@$version")
        repositoryManager.mutex.lock()
        val resultDeferred = repository.httpClient.get<HttpResponse> {
            timeout {
                requestTimeoutMillis = 1000
                connectTimeoutMillis = 1000
                socketTimeoutMillis = 1000
            }
            url {
                takeFrom(Url(repository.url))
                path(groupId.replace('.', '/'), artifactId, version.toString(), fileName)
            }
        }.downloadFile(file)
        resultDeferred.invokeOnCompletion {
            if (it == null) if (resultDeferred.getCompleted() < 0) {
                repository.log.error("Failed to download plugin. HttpStatusCode: ${resultDeferred.getCompleted() * -1L}")
                file.delete()
            } else {
                database {
                    transaction {
                        InstalledPackage.new {
                            groupId = this@PackageImpl.groupId
                            artifactId = this@PackageImpl.artifactId
                            this.version = packageVersion.version
                        }
                    }
                }
                repository.log.info("The plugin $groupId$artifactId@$version was installed!")
            }
            else repository.log.error("${lazyFailMessage()}. Download failed!", it)
            repositoryManager.mutex.unlock()
        }
        if (returnImmediately) return
        resultDeferred.await()
    }

    override suspend fun installLatest() {
        versions.sort()
        install(versions[versions.lastIndex].version)
    }

    override fun uninstall() {
        val installed = getInstalled() ?: return
        val fileName = "$artifactId-${installed.version}.jar"
        val filePath = PathQualifiers.PLUGIN_LOCATION / fileName
        if (!filePath.exists()) {
            repository.log.error("Failed to find plugin file $fileName. It looks like the file was deleted or renamed. Removing from remote installed packages list")
            database {
                transaction {
                    InstalledPackage.findByPackage(this@PackageImpl).first().delete()
                }
            }
        }
    }

    override val isInstalled: Boolean
        get() = getInstalled() != null

    private fun getInstalled(): InstalledPackageDTO? = database {
        transaction {
            InstalledPackage.findByPackage(this@PackageImpl).firstOrNull()?.dto
        }
    }

    override val installedVersion: Version?
        get() = getInstalled()?.version

    override fun isCoreVersionSupported(packageVersion: PackageVersion): Boolean =
        if (packageVersion.supportsLatest) CordBootstrap.version >= packageVersion.minCoreVersion else packageVersion.version in packageVersion.coreVersionRange

    override fun findVersion(version: Version): PackageVersion? = versions.firstOrNull { it.version == version }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PackageImpl) return false

        if (groupId != other.groupId) return false
        if (artifactId != other.artifactId) return false
        if (authors != other.authors) return false
        if (!versions.contentEquals(other.versions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + artifactId.hashCode()
        result = 31 * result + authors.hashCode()
        result = 31 * result + versions.contentHashCode()
        return result
    }
}