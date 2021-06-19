/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent

@OptIn(InternalKotlinCordApi::class)
interface PluginLoader : CordKoinComponent {
    val loadedPlugins: Array<PluginData>

    @InternalKotlinCordApi
    fun addData(data: PluginData)

    @InternalKotlinCordApi
    fun removeData(data: PluginData)
}