package de.dseelp.kotlincord.api.plugins.repository

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*

interface RepositoryLoader {
    val type: String
    fun isVersionSupported(version: Int): Boolean
    val httpClient: HttpClient

    fun defaultHttpClient() = HttpClient {
        install(JsonFeature)
        expectSuccess = false
        install(HttpTimeout)
    }

    suspend fun load(url: String, meta: RepositoryMeta): Repository?

}