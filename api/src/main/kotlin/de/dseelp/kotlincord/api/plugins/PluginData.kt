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

package de.dseelp.kotlincord.api.plugins

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import java.io.File

data class PluginData @InternalKotlinCordApi constructor(
    val classLoader: ClassLoader,
    val file: File,
    val meta: PluginMeta? = null,
    val plugin: Plugin? = null
) {

    @InternalKotlinCordApi
    fun findMeta(): PluginData? {
        if (classLoader !is PluginClassLoader) throw UnsupportedOperationException("This method is only supported by the PluginClassLoader")
        val meta = classLoader.findMeta() ?: return null
        return copy(meta = meta)
    }

    @InternalKotlinCordApi
    fun createPlugin(): PluginData? {
        checkNotNull(meta)
        if (classLoader !is PluginClassLoader) throw UnsupportedOperationException("This method is only supported by the PluginClassLoader")
        classLoader.initializePlugin(meta)
        if (classLoader.plugin == null) return null
        return copy(plugin = classLoader.plugin)
    }
}
