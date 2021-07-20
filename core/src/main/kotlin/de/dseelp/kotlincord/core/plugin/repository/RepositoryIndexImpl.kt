/*
 * Copyright (c) 2021 KotlinCord team & contributors
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
