/*
 * Created by Dirk on 22.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins.repository

interface Repository {
    val url: String
    val name: String
    val indexes: Array<RepositoryIndex>
    suspend fun updateIndexes()

    fun find(groupId: String, exact: Boolean = false): Array<RepositoryIndex>
    fun find(
        groupId: String,
        artifactId: String,
        exactGroupId: Boolean = true,
        exactArtifactId: Boolean = true
    ): Array<RepositoryIndex>
}