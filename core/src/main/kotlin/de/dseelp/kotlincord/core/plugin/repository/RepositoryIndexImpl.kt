/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin.repository

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.plugins.repository.InvalidRepositoryException
import de.dseelp.kotlincord.api.plugins.repository.Package
import de.dseelp.kotlincord.api.plugins.repository.Repository
import de.dseelp.kotlincord.api.plugins.repository.RepositoryIndex
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@OptIn(InternalKotlinCordApi::class)
@Serializable
data class RepositoryIndexImpl(
    override val groupId: String,
    override val artifactId: String,
) : RepositoryIndex, CordKoinComponent {

    override suspend fun asPackage(repository: Repository): Package {
        if (repository !is RepositoryImpl) throw IllegalArgumentException("This RepositoryIndex only supports de.dseelp.kotlincord.core.plugin.repository.RepositoryImpl as Repository!")
        val httpClient = repository.httpClient
        val url = Url(repository.url)
        val response: HttpResponse = httpClient.get {
            url {
                takeFrom(url)
                path(groupId.replace('.', '/'), artifactId, "package.json")
            }
        }
        if (response.status == HttpStatusCode.NotFound) {
            throw InvalidRepositoryException("Failed to find package ${groupId}:${artifactId} in repository ${repository.url}")
        }
        val catching = kotlin.runCatching { response.receive<PackageImpl>() }
        return catching
            .getOrElse {
                throw InvalidRepositoryException(
                    "Defect package.json for package ${toString()} in repository ${repository.url}",
                    catching.exceptionOrNull()
                )
            }
            .apply { this.repository = repository }
    }

    override fun toString(): String = "$groupId:$artifactId"
}
