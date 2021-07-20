/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins.repository

import de.dseelp.kotlincord.api.plugins.Plugin
import java.net.URL


interface RepositoryManager {

    val repositories: Array<Repository>
    fun getByName(name: String): Array<Repository> =
        name.lowercase().let { s -> repositories.filter { it.meta.name.lowercase() == s } }.toTypedArray()

    fun getByUrl(url: String): Array<Repository> =
        url.lowercase().let { s -> repositories.filter { it.url.lowercase() == s } }.toTypedArray()

    suspend fun addRepository(url: URL): Repository
    suspend fun addRepository(urlString: String): Repository
    suspend fun removeRepository(url: URL)
    suspend fun removeRepository(urlString: String)

    suspend fun updateIndexes()

    fun find(groupId: String, exact: Boolean = false): Map<Repository, Array<RepositoryIndex>>

    fun find(
        groupId: String,
        artifactId: String,
        exactGroupId: Boolean = true,
        exactArtifactId: Boolean = true
    ): Map<Repository, Array<RepositoryIndex>>

    suspend fun reloadRepositories()

    suspend fun loadRepository(url: String, logErrors: Boolean = true): Repository?

    suspend fun addLoader(plugin: Plugin, loader: RepositoryLoader)
    suspend fun removeLoader(plugin: Plugin, loader: RepositoryLoader)
    suspend fun removeLoaders(plugin: Plugin)
    suspend fun getLoaders(plugin: Plugin): Array<RepositoryLoader>
    suspend fun getLoaderByType(type: String): RepositoryLoader
    suspend fun getLoaderByTypeOrNull(type: String): RepositoryLoader?

}