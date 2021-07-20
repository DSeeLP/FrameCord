/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin.repository

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.repository.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.net.ConnectException

class RepositoryImpl(
    val httpClient: HttpClient,
    override val url: String,
    indexes: Array<RepositoryIndex> = arrayOf()
) : Repository {

    val log by logger<RepositoryManager>()

    private val parsedUrl = Url(url)

    private lateinit var _meta: RepositoryMeta
    private var _indexes: Array<RepositoryIndex> = indexes

    override val meta: RepositoryMeta
        get() = _meta
    override val indexes: Array<RepositoryIndex>
        get() = _indexes

    init {
        runBlocking {
            refresh()
        }
    }

    private suspend fun refresh() {
        val data = requestData() ?: return
        if (this::_meta.isInitialized && (_meta.version != data.meta.version)) return
        _meta = data.meta
        updateIndexes(data)
    }

    private suspend fun requestData(): RepositoryDTO? {
        try {
            return httpClient.get {
                url {
                    takeFrom(parsedUrl)
                    path("index.json")
                }
            }
        } catch (ex: Throwable) {
            if (ex is ConnectException) {
                log.error("Failed to connect to repository! $url")
            } else throw InvalidRepositoryException("Invalid Repository! $url", ex)
        }
        return null
    }


    override suspend fun updateIndexes() = updateIndexes(requestData())

    fun updateIndexes(data: RepositoryDTO?) {
        if (data == null) return
        @Suppress("UNCHECKED_CAST")
        _indexes = data.packages as Array<RepositoryIndex>
    }

    override fun find(groupId: String, exact: Boolean): Array<RepositoryIndex> =
        (if (exact) indexes.filter { it.groupId == groupId } else indexes.filter { it.groupId.startsWith(groupId) }).toTypedArray()

    override fun find(
        groupId: String,
        artifactId: String,
        exactGroupId: Boolean,
        exactArtifactId: Boolean
    ): Array<RepositoryIndex> =
        indexes.filter { it.groupId.check(groupId, exactGroupId) && it.artifactId.check(artifactId, exactArtifactId) }
            .toTypedArray()

    @OptIn(InternalKotlinCordApi::class)
    override suspend fun toPackage(index: RepositoryIndex): Package<*> {
        val response: HttpResponse = httpClient.get {
            url {
                takeFrom(url)
                path(index.groupId.replace('.', '/'), index.artifactId, "package.json")
            }
        }
        if (response.status == HttpStatusCode.NotFound) {
            throw InvalidRepositoryException("Failed to find package ${index.groupId}:${index.artifactId} in repository $url")
        }
        val catching = kotlin.runCatching { response.receive<PackageImpl>() }
        return catching
            .getOrElse {
                throw InvalidRepositoryException(
                    "Defect package.json for package $index in repository $url",
                    catching.exceptionOrNull()
                )
            }
            .apply { this.repository = this@RepositoryImpl }
    }

    private fun String.check(value: String, exact: Boolean) =
        if (exact) this.equals(value, ignoreCase = true) else this.lowercase().startsWith(value.lowercase())
}