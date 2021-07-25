/*
 * Copyright (c) 2021 DSeeLP & KotlinCord contributors
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dseelp.kotlincord.core.plugin.repository

import de.dseelp.kotlincord.api.event.EventHandle
import de.dseelp.kotlincord.api.event.Listener
import de.dseelp.kotlincord.api.events.PluginDisableEvent
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.repository.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.div

@Listener
class RepositoryManagerImpl : RepositoryManager {
    private val logger by logger<RepositoryManager>()
    var mutex: Mutex = Mutex()
    val httpClient: HttpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout.Feature)
        expectSuccess = false
    }
    private val loaderMutex = Mutex()
    private var loaders = mapOf<Plugin, List<RepositoryLoader>>()

    override val repositories: Array<Repository>
        @Suppress("UNCHECKED_CAST")
        get() = _repositories

    private var _repositories: Array<Repository> = arrayOf()

    private val path = Path("") / "repositories.json"

    init {
        runBlocking {
            RepositoryConfig.set(path, RepositoryConfig.load(path).repositories.distinct().toTypedArray())
        }
    }

    override suspend fun addRepository(urlString: String): Repository {
        if (RepositoryConfig.load(path).repositories.contains(urlString)) repositories.first { it.url == urlString }
        RepositoryConfig.add(path, urlString)
        reloadRepositories()
        return repositories.first { it.url == urlString }
    }

    override suspend fun addRepository(url: URL): Repository = addRepository(url.toString())

    override suspend fun removeRepository(url: URL) = removeRepository(url.toString())

    override suspend fun removeRepository(urlString: String) {
        if (!RepositoryConfig.load(path).repositories.contains(urlString)) return
        RepositoryConfig.remove(path, urlString)
        reloadRepositories()
    }

    override suspend fun updateIndexes() {
        repositories.onEach { it.updateIndexes() }
    }

    override fun find(groupId: String, exact: Boolean): Map<Repository, Array<RepositoryIndex>> =
        repositories.associateWith { it.find(groupId, exact) }

    override fun find(
        groupId: String,
        artifactId: String,
        exactGroupId: Boolean,
        exactArtifactId: Boolean
    ): Map<Repository, Array<RepositoryIndex>> {
        val searchingGroupId = if (groupId == "*") "" else groupId
        val results =
            repositories.associateWith { it.find(searchingGroupId, artifactId, exactGroupId, exactArtifactId) }
        if (results.size > 20) throw IllegalArgumentException("Too many results found!")
        return results
    }

    override suspend fun reloadRepositories() {
        val urls = RepositoryConfig.load(path).repositories.distinct()
        val oldUrls = _repositories.map { it.url }.distinct()
        val newUrls = urls - oldUrls
        if (newUrls.isEmpty()) return
        val new = newUrls.mapNotNull { url -> loadRepository(url) }
        _repositories =
            (_repositories + new).distinct().filter { repository -> urls.contains(repository.url) }.toTypedArray()
    }

    override suspend fun loadRepository(url: String, logErrors: Boolean): Repository? {
        val meta = httpClient.get<RepositoryMetaHolder?> {
            url {
                takeFrom(url)
                pathComponents("index.json")
            }
        }?.meta ?: run {
            logger.warn("Failed to load plugin repository! $url")
            return null
        }
        val loader = getLoaderByTypeOrNull(meta.type) ?: return null
        return loader.load(url, meta)
    }

    override suspend fun addLoader(plugin: Plugin, loader: RepositoryLoader) {
        checkPluginLoaders(plugin)
        loaderMutex.withLock {
            loaders += plugin to listOf(*(loaders[plugin]!! + loader).toTypedArray())
        }
    }

    private suspend fun checkPluginLoaders(plugin: Plugin) {
        loaderMutex.withLock {
            if (loaders.containsKey(plugin)) return@withLock
            loaders += plugin to listOf()
        }
    }

    override suspend fun removeLoader(plugin: Plugin, loader: RepositoryLoader) {
        checkPluginLoaders(plugin)
    }

    override suspend fun removeLoaders(plugin: Plugin) {
        loaderMutex.withLock {
            if (loaders.containsKey(plugin)) return@withLock
            loaders -= plugin
        }
    }

    override suspend fun getLoaders(plugin: Plugin): Array<RepositoryLoader> {
        checkPluginLoaders(plugin)
        return loaderMutex.withLock {
            loaders[plugin]!!.toTypedArray()
        }
    }

    override suspend fun getLoaderByType(type: String): RepositoryLoader = getLoaderByTypeOrNull(type)!!

    override suspend fun getLoaderByTypeOrNull(type: String): RepositoryLoader? {
        loaderMutex.withLock {
            loaders.entries.onEach {
                for (loader in it.value) {
                    if (loader.type.equals(type, true)) return loader
                }
            }
        }
        return null
    }

    @EventHandle
    suspend fun onPluginDisable(event: PluginDisableEvent) {
        removeLoaders(event.plugin)
    }

}
