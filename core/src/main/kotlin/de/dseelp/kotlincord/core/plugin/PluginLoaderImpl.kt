/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.plugins.PluginData
import de.dseelp.kotlincord.api.plugins.PluginLoader

class PluginLoaderImpl : PluginLoader {

    var plugins = mutableListOf<PluginData>()

    override val loadedPlugins: Array<PluginData>
        get() = plugins.toTypedArray()

    @InternalKotlinCordApi
    override fun addData(data: PluginData) {
        plugins.add(data)
    }

    @InternalKotlinCordApi
    override fun removeData(data: PluginData) {
        plugins.remove(data)
    }
}