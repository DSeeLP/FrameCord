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
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

@InternalKotlinCordApi
abstract class PluginClassLoader(val file: File, parent: ClassLoader) :
    URLClassLoader(arrayOf(file.toURI().toURL()), parent),
    CordKoinComponent {
    val jarFile = JarFile(file)
    override fun close() {
        super.close()
        jarFile.close()
    }

    @InternalKotlinCordApi
    abstract fun findMeta(): PluginMeta?

    abstract fun initializePlugin(meta: PluginMeta)

    @InternalKotlinCordApi
    var plugin: Plugin? = null
        internal set
}