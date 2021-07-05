/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin.repository

import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.repository.InvalidRepositoryException
import de.dseelp.kotlincord.api.plugins.repository.Repository
import de.dseelp.kotlincord.api.plugins.repository.RepositoryIndex
import de.dseelp.kotlincord.api.plugins.repository.RepositoryManager
import io.ktor.client.*
import io.ktor.client.request.*
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

    private var _name: String = ""
    private var _indexes: Array<RepositoryIndex> = indexes

    override val name: String
        get() = _name
    override val indexes: Array<RepositoryIndex>
        get() = _indexes

    init {
        runBlocking {
            refresh()
        }
    }

    private suspend fun refresh() {
        val data = requestData() ?: return
        _name = data.name
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

    private fun String.check(value: String, exact: Boolean) =
        if (exact) this.equals(value, ignoreCase = true) else this.lowercase().startsWith(value.lowercase())
}