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
import io.ktor.client.features.json.*
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.div

class RepositoryManagerImpl : RepositoryManager {
    val httpClient: HttpClient = HttpClient(CIO) {
        install(JsonFeature)
        expectSuccess = false
    }

    override val repositories: Array<Repository>
        @Suppress("UNCHECKED_CAST")
        get() = _repositories as Array<Repository>

    private var _repositories: Array<RepositoryImpl> = arrayOf()

    private val path = Path("") / "repositories.json"

    override fun addRepository(urlString: String): Repository {
        RepositoryConfig.add(path, urlString)
        reloadRepositories()
        return repositories.first { it.url == urlString }
    }

    override fun addRepository(url: URL): Repository = addRepository(url.toString())

    override fun removeRepository(url: URL) = removeRepository(url.toString())

    override fun removeRepository(urlString: String) {
        if (!RepositoryConfig.load(path).repositories.contains(urlString)) return
        reloadRepositories()
        RepositoryConfig.remove(path, urlString)
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
        val results = repositories.associate { it to it.find(groupId, artifactId, exactGroupId, exactArtifactId) }
        if (results.size > 20) throw IllegalArgumentException("Too many results found!")
        return results
    }

    override fun reloadRepositories() {
        val urls = RepositoryConfig.load(path).repositories.distinct()
        val oldUrls = _repositories.map { it.url }.distinct()
        val newUrls = urls - oldUrls
        if (newUrls.isEmpty()) return
        val new = newUrls.map { RepositoryImpl(httpClient, it) }.filter { it.name != "" && it.name.isNotEmpty() }
        _repositories = (_repositories + new).distinct().toTypedArray()
    }

}
