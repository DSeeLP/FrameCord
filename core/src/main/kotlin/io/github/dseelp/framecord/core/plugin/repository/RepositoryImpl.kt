/*
 * Copyright (c) 2021 DSeeLP & FrameCord contributors
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

package io.github.dseelp.framecord.core.plugin.repository

import io.github.dseelp.framecord.api.logging.logger
import io.github.dseelp.framecord.api.plugins.repository.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.net.ConnectException

class RepositoryImpl(
    val httpClient: HttpClient,
    override val url: String,
    indexes: Array<RepositoryIndex> = arrayOf()
) : Repository {

    val log by logger<RepositoryManager>()

    private val parsedUrl = Url(url)

    private lateinit var _meta: RepositoryMeta
    private var _indexes: Array<RepositoryIndex> = indexes

    override val meta: RepositoryMeta
        get() = _meta
    override val indexes: Array<RepositoryIndex>
        get() = _indexes

    init {
        runBlocking {
            refresh()
        }
    }

    private suspend fun refresh() {
        val data = requestData() ?: return
        if (this::_meta.isInitialized && (_meta.version != data.meta.version)) return
        _meta = data.meta
        updateIndexes(data)
    }

    private suspend fun requestData(): RepositoryDTO? {
        try {
            return httpClient.get {
                url {
                    takeFrom(parsedUrl)
                    pathComponents("index.json")
                }
            }
        } catch (ex: Throwable) {
            if (ex is ConnectException) {
                log.error("Failed to connect to repository! $url")
            } else throw InvalidRepositoryException("Invalid Repository! $url", ex)
        }
        return null
    }


    override suspend fun updateIndexes() = updateIndexes(requestData())

    fun updateIndexes(data: RepositoryDTO?) {
        if (data == null) return
        @Suppress("UNCHECKED_CAST")
        _indexes = data.packages as Array<RepositoryIndex>
    }

    override fun find(groupId: String, exact: Boolean): Array<RepositoryIndex> =
        (if (exact) indexes.filter { it.groupId == groupId } else indexes.filter { it.groupId.startsWith(groupId) }).toTypedArray()

    override fun find(
        groupId: String,
        artifactId: String,
        exactGroupId: Boolean,
        exactArtifactId: Boolean
    ): Array<RepositoryIndex> =
        indexes.filter { it.groupId.check(groupId, exactGroupId) && it.artifactId.check(artifactId, exactArtifactId) }
            .toTypedArray()

    @OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
    override suspend fun toPackage(index: RepositoryIndex): Package<*> {
        val response: HttpResponse = httpClient.get {
            url {
                takeFrom(this@RepositoryImpl.url)
                pathComponents(index.groupId.replace('.', '/'), index.artifactId, "package.json")
            }
        }
        if (response.status == HttpStatusCode.NotFound) {
            throw InvalidRepositoryException("Failed to find package ${index.groupId}:${index.artifactId} in repository $url")
        }
        val catching = kotlin.runCatching { response.receive<PackageImpl>() }
        return catching
            .getOrElse {
                throw InvalidRepositoryException(
                    "Defect package.json for package $index in repository $url",
                    catching.exceptionOrNull()
                )
            }
            .apply { this.repository = this@RepositoryImpl }
    }

    private fun String.check(value: String, exact: Boolean) =
        if (exact) this.equals(value, ignoreCase = true) else this.lowercase().startsWith(value.lowercase())
}