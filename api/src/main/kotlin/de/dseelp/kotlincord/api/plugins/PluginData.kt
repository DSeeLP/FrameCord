/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
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
