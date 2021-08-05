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

package io.github.dseelp.framecord.api.plugins

import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import java.io.File
import java.nio.file.Path

@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
interface PluginManager : CordKoinComponent {
    fun load(file: File): PluginData
    fun load(path: Path) = load(path.toFile())

    suspend fun unload(data: PluginData)
    suspend fun unload(path: Path)


    suspend fun enable(plugin: Plugin)
    suspend fun enable(name: String) =
        enable(get(name)?.plugin ?: throw RuntimeException("A module with the name $name couldn't be found"))

    suspend fun disable(plugin: Plugin)
    suspend fun disable(name: String) =
        disable(get(name)?.plugin ?: throw RuntimeException("A module with the name $name couldn't be found"))

    operator fun get(name: String): PluginData?
}