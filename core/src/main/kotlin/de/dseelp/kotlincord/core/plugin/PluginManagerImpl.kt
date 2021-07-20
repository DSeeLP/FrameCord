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

package de.dseelp.kotlincord.core.plugin

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.event.EventHandle
import de.dseelp.kotlincord.api.event.Listener
import de.dseelp.kotlincord.api.events.PluginDisableEvent
import de.dseelp.kotlincord.api.events.PluginEnableEvent
import de.dseelp.kotlincord.api.events.PluginEventType
import de.dseelp.kotlincord.api.events.ShutdownEvent
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.*
import de.dseelp.kotlincord.api.utils.koin.KoinModules
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier
import java.io.Closeable
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@Listener
@OptIn(InternalKotlinCordApi::class)
class PluginManagerImpl : PluginManager {

    val loader: PluginLoader by inject()
    val parentLoader: URLClassLoader by inject(qualifier("pluginClassLoader"))
    val eventBus: EventBus by inject()
    val logger by logger<PluginManager>()

    @EventHandle
    fun onPluginEvent(event: PluginDisableEvent) {
        when (event.type) {
            PluginEventType.PRE -> logger.info("Disabling ${event.plugin.name}")
            PluginEventType.POST -> logger.info("Disabled ${event.plugin.name}")
        }
    }

    @EventHandle
    fun onPluginEvent(event: PluginEnableEvent) {
        when (event.type) {
            PluginEventType.PRE -> logger.info("Enabling ${event.plugin.name}")
            PluginEventType.POST -> logger.info("Enabled ${event.plugin.name}")
        }
    }

    @OptIn(InternalKotlinCordApi::class)
    @EventHandle
    @Suppress("UNUSED_PARAMETER")
    suspend fun shutdown(event: ShutdownEvent) {
        logger.info("Unloading plugins...")
        loader.loadedPlugins.onEach {
            unload(it)
        }
        logger.info("Plugins unloaded")
    }

    @OptIn(InternalKotlinCordApi::class)
    override fun load(file: File): PluginData {
        val classLoader = PluginClassLoaderImpl(file, parentLoader)
        val data = PluginData(classLoader, file, null, null).findMeta()?.createPlugin()
            ?: throw IllegalStateException("Failed to load file ${file.name}")
        val plugin = data.plugin!!
        KoinModules.load(plugin)
        loader.addData(data)
        return data
    }

    @OptIn(InternalKotlinCordApi::class)
    override suspend fun unload(data: PluginData) {
        val plugin = data.plugin ?: return
        disable(plugin)
        if (data.classLoader is Closeable) (data.classLoader as Closeable).close()
        loader.removeData(data)
    }

    override suspend fun unload(path: Path) {
        loader.loadedPlugins.firstOrNull { it.file.toPath() == path }?.let {
            val name = it.meta?.name
            logger.info("Unloading Plugin ${name}...")
            val runCatching = kotlin.runCatching { unload(it) }
            if (runCatching.exceptionOrNull() == null)
                logger.info("Plugin $name was unloaded.")
            else {
                logger.error("Failed to unload plugin $name", runCatching.exceptionOrNull()!!)
            }
        }
    }

    override suspend fun enable(plugin: Plugin) {
        eventBus.callAsync(PluginEnableEvent(plugin, PluginEventType.PRE))
        executeAction(plugin, PluginAction.Action.ENABLE)
        eventBus.callAsync(PluginEnableEvent(plugin, PluginEventType.POST))
    }

    private fun find(plugin: Plugin) = plugin::class.declaredFunctions.filter { it.hasAnnotation<PluginAction>() }
        .map { it to it.findAnnotation<PluginAction>()!! }

    private fun executeAction(plugin: Plugin, action: PluginAction.Action) {
        find(plugin).onEach {
            val mAction = it.second.action
            runCatching {
                if (mAction == action) it.first.run {
                    if (isSuspend) runBlocking { callSuspend(plugin) } else call(
                        plugin
                    )
                }
            }.exceptionOrNull()?.let { t -> t.cause?.let { c -> throw c } ?: t.printStackTrace() }
        }
    }

    override suspend fun disable(plugin: Plugin) {
        eventBus.callAsync(PluginDisableEvent(plugin, PluginEventType.PRE))
        executeAction(plugin, PluginAction.Action.DISABLE)
        plugin.koinApp.close()
        eventBus.callAsync(PluginDisableEvent(plugin, PluginEventType.POST))
    }

    override fun get(name: String): PluginData? = loader.loadedPlugins.firstOrNull { it.meta?.name == name }
}