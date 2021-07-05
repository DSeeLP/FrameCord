/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins.repository

import java.net.URL


interface RepositoryManager {

    val repositories: Array<Repository>
    fun getByName(name: String): Array<Repository> =
        name.lowercase().let { s -> repositories.filter { it.name.lowercase() == s } }.toTypedArray()

    fun getByUrl(url: String): Array<Repository> =
        url.lowercase().let { s -> repositories.filter { it.name.lowercase() == s } }.toTypedArray()

    fun addRepository(url: URL): Repository
    fun addRepository(urlString: String): Repository
    fun removeRepository(url: URL)
    fun removeRepository(urlString: String)

    suspend fun updateIndexes()

    fun find(groupId: String, exact: Boolean = false): Map<Repository, Array<RepositoryIndex>>

    fun find(
        groupId: String,
        artifactId: String,
        exactGroupId: Boolean = true,
        exactArtifactId: Boolean = true
    ): Map<Repository, Array<RepositoryIndex>>

    fun reloadRepositories()

}