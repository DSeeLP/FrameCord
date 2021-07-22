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

package de.dseelp.kotlincord.api.plugins.repository

import de.dseelp.kotlincord.api.plugins.Plugin
import java.net.URL


interface RepositoryManager {

    val repositories: Array<Repository>
    fun getByName(name: String): Array<Repository> =
        name.lowercase().let { s -> repositories.filter { it.meta.name.lowercase() == s } }.toTypedArray()

    fun getByUrl(url: String): Array<Repository> =
        url.lowercase().let { s -> repositories.filter { it.url.lowercase() == s } }.toTypedArray()

    suspend fun addRepository(url: URL): Repository
    suspend fun addRepository(urlString: String): Repository
    suspend fun removeRepository(url: URL)
    suspend fun removeRepository(urlString: String)

    suspend fun updateIndexes()

    fun find(groupId: String, exact: Boolean = false): Map<Repository, Array<RepositoryIndex>>

    fun find(
        groupId: String,
        artifactId: String,
        exactGroupId: Boolean = true,
        exactArtifactId: Boolean = true
    ): Map<Repository, Array<RepositoryIndex>>

    suspend fun reloadRepositories()

    suspend fun loadRepository(url: String, logErrors: Boolean = true): Repository?

    suspend fun addLoader(plugin: Plugin, loader: RepositoryLoader)
    suspend fun removeLoader(plugin: Plugin, loader: RepositoryLoader)
    suspend fun removeLoaders(plugin: Plugin)
    suspend fun getLoaders(plugin: Plugin): Array<RepositoryLoader>
    suspend fun getLoaderByType(type: String): RepositoryLoader
    suspend fun getLoaderByTypeOrNull(type: String): RepositoryLoader?

}