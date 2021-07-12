/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin.repository

import de.dseelp.kotlincord.api.plugins.repository.Repository
import de.dseelp.kotlincord.api.plugins.repository.RepositoryIndex
import de.dseelp.kotlincord.api.plugins.repository.RepositoryManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.div

class RepositoryManagerImpl : RepositoryManager {
    var mutex: Mutex = Mutex()
    val httpClient: HttpClient = HttpClient(CIO) {
        install(JsonFeature)
        install(HttpTimeout.Feature)
        expectSuccess = false
    }

    override val repositories: Array<Repository>
        @Suppress("UNCHECKED_CAST")
        get() = _repositories as Array<Repository>

    private var _repositories: Array<RepositoryImpl> = arrayOf()

    private val path = Path("") / "repositories.json"

    init {
        runBlocking {
            RepositoryConfig.set(path, RepositoryConfig.load(path).repositories.distinct().toTypedArray())
        }
    }

    override suspend fun addRepository(urlString: String): Repository {
        if (RepositoryConfig.load(path).repositories.contains(urlString)) repositories.first { it.url == urlString }
        RepositoryConfig.add(path, urlString)
        reloadRepositories()
        return repositories.first { it.url == urlString }
    }

    override suspend fun addRepository(url: URL): Repository = addRepository(url.toString())

    override suspend fun removeRepository(url: URL) = removeRepository(url.toString())

    override suspend fun removeRepository(urlString: String) {
        if (!RepositoryConfig.load(path).repositories.contains(urlString)) return
        RepositoryConfig.remove(path, urlString)
        reloadRepositories()
    }

    override suspend fun updateIndexes() {
        repositories.onEach { it.updateIndexes() }
    }

    override fun find(groupId: String, exact: Boolean): Map<Repository, Array<RepositoryIndex>> =
        repositories.associate { it to it.find(groupId, exact) }

    override fun find(
        groupId: String,
        artifactId: String,
        exactGroupId: Boolean,
        exactArtifactId: Boolean
    ): Map<Repository, Array<RepositoryIndex>> {
        val results = repositories.associateWith { it.find(groupId, artifactId, exactGroupId, exactArtifactId) }
        if (results.size > 20) throw IllegalArgumentException("Too many results found!")
        return results
    }

    override suspend fun reloadRepositories() {
        val urls = RepositoryConfig.load(path).repositories.distinct()
        val oldUrls = _repositories.map { it.url }.distinct()
        val newUrls = urls - oldUrls
        if (newUrls.isEmpty()) return
        val new = newUrls.map { RepositoryImpl(httpClient, it) }.filter { it.name != "" && it.name.isNotEmpty() }
        _repositories = (_repositories + new).distinct().filter { urls.contains(it.url) }.toTypedArray()
    }

}
