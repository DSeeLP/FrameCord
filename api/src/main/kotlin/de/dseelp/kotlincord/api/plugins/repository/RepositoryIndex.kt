/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins.repository

interface RepositoryIndex {
    val groupId: String
    val artifactId: String

    suspend fun asPackage(repository: Repository): Package<*> = repository.toPackage(this)
}