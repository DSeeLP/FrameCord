/*
 * Created by Dirk on 22.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins.repository

interface RepositoryIndex {
    val groupId: String
    val artifactId: String

    suspend fun asPackage(repository: Repository): Package
}