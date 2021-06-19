/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
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