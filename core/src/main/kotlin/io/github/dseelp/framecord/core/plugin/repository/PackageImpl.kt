/*
 * Copyright (c) 2021 DSeeLP & FrameCord contributors
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.dseelp.framecord.core.plugin.repository

import com.log4k.e
import com.log4k.i
import io.github.dseelp.framecord.api.Version
import io.github.dseelp.framecord.api.plugins.repository.Package
import io.github.dseelp.framecord.api.plugins.repository.PackageVersion
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import io.github.dseelp.framecord.core.CordBootstrap
import io.github.dseelp.framecord.core.FakePlugin.database
import io.github.dseelp.framecord.core.plugin.repository.data.InstalledPackage
import io.github.dseelp.framecord.core.plugin.repository.data.InstalledPackageDTO
import io.github.dseelp.framecord.core.utils.downloadFile
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

@io.github.dseelp.framecord.api.InternalFrameCordApi
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

            e("There is no version $version of the package $groupId$artifactId", repository.lCfg)
            return
        }
        if (!isCoreVersionSupported(version)) {
            e("The version of the package $groupId$artifactId $version is not supported by the current FrameCord version (${CordBootstrap.version})", repository.lCfg)
            val firstSupported = versions.copyOfRange(versions.indexOf(packageVersion), versions.lastIndex)
                .firstOrNull { isCoreVersionSupported(packageVersion) }
            if (firstSupported == null) {
                e("There is no version of this package for the current FrameCord version (${CordBootstrap.version})", repository.lCfg)
            } else {
                e("The first version that supports this FrameCord version is ${firstSupported.version}", repository.lCfg)
            }
            return
        }
        val lazyFailMessage = { "Failed to install the package $groupId:$artifactId@$version" }
        if (isInstalled) {
            e("${lazyFailMessage()} it is already installed", repository.lCfg)
        }
        val fileName = "$artifactId-$version.jar"
        val filePath = io.github.dseelp.framecord.api.PathQualifiers.PLUGIN_LOCATION / fileName
        if (filePath.exists()) {
            e("${lazyFailMessage.invoke()} the file $filePath already exists!", repository.lCfg)
            return
        }
        val file = filePath.toFile()
        i("Installing package $groupId:$artifactId@$version", repository.lCfg)
        repositoryManager.mutex.lock()
        val resultDeferred = repository.httpClient.get<HttpResponse> {
            timeout {
                requestTimeoutMillis = 1000
                connectTimeoutMillis = 1000
                socketTimeoutMillis = 1000
            }
            url {
                takeFrom(Url(repository.url))
                pathComponents(groupId.replace('.', '/'), artifactId, version.toString(), fileName)
            }
        }.downloadFile(file)
        resultDeferred.invokeOnCompletion {
            if (it == null) if (resultDeferred.getCompleted() < 0) {
                e("Failed to download plugin. HttpStatusCode: ${resultDeferred.getCompleted() * -1L}", repository.lCfg)
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
                i("The plugin $groupId$artifactId@$version was installed!", repository.lCfg)
            }
            else e("${lazyFailMessage()}. Download failed!", it, repository.lCfg)
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
        val filePath = io.github.dseelp.framecord.api.PathQualifiers.PLUGIN_LOCATION / fileName
        if (!filePath.exists()) {
            e("Failed to find plugin file $fileName. It looks like the file was deleted or renamed. Removing from remote installed packages list", repository.lCfg)
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