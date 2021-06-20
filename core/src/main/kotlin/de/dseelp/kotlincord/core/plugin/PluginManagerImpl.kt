/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
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
    fun shutdown(event: ShutdownEvent) {
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
    override fun unload(data: PluginData) {
        val plugin = data.plugin ?: return
        disable(plugin)
        if (data.classLoader is Closeable) (data.classLoader as Closeable).close()
        loader.removeData(data)
    }

    override fun unload(path: Path) {
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

    override fun enable(plugin: Plugin) {
        eventBus.call(PluginEnableEvent(plugin, PluginEventType.PRE))
        executeAction(plugin, PluginAction.Action.ENABLE)
        eventBus.call(PluginEnableEvent(plugin, PluginEventType.POST))
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

    override fun disable(plugin: Plugin) {
        eventBus.call(PluginDisableEvent(plugin, PluginEventType.PRE))
        executeAction(plugin, PluginAction.Action.DISABLE)
        plugin.koinApp.close()
        eventBus.call(PluginDisableEvent(plugin, PluginEventType.POST))
    }

    override fun get(name: String): PluginData? = loader.loadedPlugins.firstOrNull { it.meta?.name == name }
}