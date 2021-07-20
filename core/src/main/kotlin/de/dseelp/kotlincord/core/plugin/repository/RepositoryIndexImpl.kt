/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin.repository

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.plugins.repository.Package
import de.dseelp.kotlincord.api.plugins.repository.Repository
import de.dseelp.kotlincord.api.plugins.repository.RepositoryIndex
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import kotlinx.serialization.Serializable

@OptIn(InternalKotlinCordApi::class)
@Serializable
data class RepositoryIndexImpl(
    override val groupId: String,
    override val artifactId: String,
) : RepositoryIndex, CordKoinComponent {

    override suspend fun asPackage(repository: Repository): Package<*> {
        if (repository !is RepositoryImpl) throw IllegalArgumentException("This RepositoryIndex only supports de.dseelp.kotlincord.core.plugin.repository.RepositoryImpl as Repository!")
        return super.asPackage(repository)
    }

    override fun toString(): String = "$groupId:$artifactId"
}
