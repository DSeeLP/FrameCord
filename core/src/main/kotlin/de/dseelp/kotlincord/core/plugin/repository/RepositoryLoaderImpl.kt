package de.dseelp.kotlincord.core.plugin.repository

import de.dseelp.kotlincord.api.plugins.repository.Repository
import de.dseelp.kotlincord.api.plugins.repository.RepositoryLoader
import de.dseelp.kotlincord.api.plugins.repository.RepositoryMeta
import io.ktor.client.*

class RepositoryLoaderImpl : RepositoryLoader {
    override val type: String = "default"

    override fun isVersionSupported(version: Int): Boolean =
        when (version) {
            1 -> true
            else -> false
        }

    override val httpClient: HttpClient = defaultHttpClient()

    override suspend fun load(url: String, meta: RepositoryMeta): Repository? {
        return RepositoryImpl(httpClient, url)
    }
}