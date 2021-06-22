/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import java.io.File
import java.nio.file.Path

@OptIn(InternalKotlinCordApi::class)
interface PluginManager : CordKoinComponent {
    fun load(file: File): PluginData
    fun load(path: Path) = load(path.toFile())

    fun unload(data: PluginData)
    fun unload(path: Path)


    fun enable(plugin: Plugin)
    fun enable(name: String) =
        enable(get(name)?.plugin ?: throw RuntimeException("A module with the name $name couldn't be found"))

    fun disable(plugin: Plugin)
    fun disable(name: String) =
        disable(get(name)?.plugin ?: throw RuntimeException("A module with the name $name couldn't be found"))

    operator fun get(name: String): PluginData?
}