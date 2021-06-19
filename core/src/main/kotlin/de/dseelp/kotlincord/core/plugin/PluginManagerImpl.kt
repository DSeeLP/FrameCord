/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.event.EventHandle
import de.dseelp.kotlincord.api.events.ShutdownEvent
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.*
import de.dseelp.kotlincord.api.utils.Commands
import de.dseelp.kotlincord.api.utils.koin.KoinModules
import de.dseelp.kotlincord.core.FakePlugin
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

@OptIn(InternalKotlinCordApi::class)
class PluginManagerImpl : PluginManager {

    val loader: PluginLoader by inject()
    val parentLoader: URLClassLoader by inject(qualifier("pluginClassLoader"))
    val eventBus: EventBus by inject()
    val logger by logger<PluginManager>()

    init {
        eventBus.addClassHandler(FakePlugin, this)
    }

    @OptIn(InternalKotlinCordApi::class)
    @EventHandle
    @Suppress("UNUSED_PARAMETER")
    fun shutdown(event: ShutdownEvent) {
        logger.info("Unloading plugins...")
        loader.loadedPlugins.onEach {
            unload(it)
        }
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
    override fun unload(data: PluginData) {
        val plugin = data.plugin ?: return
        disable(plugin)
        eventBus.unregister(plugin::class)
        Commands.unregister(plugin)
        val modules = KoinModules[plugin]
        loader.loadedPlugins.onEach {
            if (it.plugin == plugin) return@onEach
            it.plugin?.koinApp?.unloadModules(modules)
        }
        KoinModules.unregister(plugin)
        if (data.classLoader is Closeable) (data.classLoader as Closeable).close()
        loader.removeData(data)
    }

    override fun unload(path: Path) {
        loader.loadedPlugins.firstOrNull { it.file.toPath() == path }?.let { unload(it) }
    }

    override fun enable(plugin: Plugin) {
        find(plugin).onEach {
            val action = it.second.action
            if (action == PluginAction.Action.ENABLE) it.first.run {
                if (isSuspend) runBlocking { callSuspend(plugin) } else call(
                    plugin
                )
            }
        }
    }

    private fun find(plugin: Plugin) = plugin::class.declaredFunctions.filter { it.hasAnnotation<PluginAction>() }
        .map { it to it.findAnnotation<PluginAction>()!! }

    override fun disable(plugin: Plugin) {
        find(plugin).onEach {
            val action = it.second.action
            if (action == PluginAction.Action.DISABLE) it.first.run {
                if (isSuspend) runBlocking { callSuspend(plugin) } else call(
                    plugin
                )
            }
        }
        plugin.koinApp.close()
    }

    override fun get(name: String): PluginData? = loader.loadedPlugins.firstOrNull { it.meta?.name == name }
}