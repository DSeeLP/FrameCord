/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.utils.koin

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.event.EventHandle
import de.dseelp.kotlincord.api.event.Listener
import de.dseelp.kotlincord.api.events.PluginDisableEvent
import de.dseelp.kotlincord.api.events.PluginEventType
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.PluginLoader
import org.koin.core.component.inject
import org.koin.core.module.Module
import org.koin.dsl.ModuleDeclaration
import org.koin.dsl.module


@Listener
@InternalKotlinCordApi
object KoinModules : CordKoinComponent {
    private val loader: PluginLoader by inject()
    val modules = hashMapOf<Plugin, MutableList<Module>>()

    fun unregister(plugin: Plugin) {
        modules.remove(plugin)
    }

    internal fun register(plugin: Plugin, modules: List<Module>) {
        this.modules.getOrPut(plugin) { mutableListOf() }.addAll(modules)
        loader.loadedPlugins.onEach {
            if (it.plugin == plugin) return
            it.plugin?.koinApp?.modules(modules)
        }
    }

    fun load(plugin: Plugin) {
        val merged = mutableListOf<Module>()
        modules.values.onEach { merged.addAll(it) }
        plugin.koinApp.modules(merged)
    }

    @EventHandle
    fun onPluginDisable(event: PluginDisableEvent) {
        if (event.type != PluginEventType.POST) return
        val modules = KoinModules[event.plugin]
        loader.loadedPlugins.onEach {
            if (it.plugin == event.plugin) return@onEach
            it.plugin?.koinApp?.unloadModules(modules)
        }
        unregister(event.plugin)
    }


    operator fun get(plugin: Plugin) = modules.getOrPut(plugin) { mutableListOf() }
}


@OptIn(InternalKotlinCordApi::class)
fun Plugin.registerKoinModules(module: Module) = KoinModules.register(this, listOf(module))

@OptIn(InternalKotlinCordApi::class)
fun Plugin.registerKoinModules(modules: List<Module>) = KoinModules.register(this, modules)

fun Plugin.registerKoinModules(
    createdAtStart: Boolean = false,
    moduleDeclaration: ModuleDeclaration
) = registerKoinModules(
    module(createdAtStart, moduleDeclaration)
)