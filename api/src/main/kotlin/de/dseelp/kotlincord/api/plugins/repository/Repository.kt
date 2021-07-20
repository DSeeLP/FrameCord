/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins.repository

interface Repository {
    val url: String
    val meta: RepositoryMeta
    val indexes: Array<RepositoryIndex>
    suspend fun updateIndexes()

    fun find(groupId: String, exact: Boolean = false): Array<RepositoryIndex>
    fun find(
        groupId: String,
        artifactId: String,
        exactGroupId: Boolean = true,
        exactArtifactId: Boolean = true
    ): Array<RepositoryIndex>

    suspend fun toPackage(index: RepositoryIndex): Package<*>
}