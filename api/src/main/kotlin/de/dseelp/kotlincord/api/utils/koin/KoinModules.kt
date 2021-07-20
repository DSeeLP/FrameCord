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