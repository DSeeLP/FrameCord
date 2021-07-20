/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import de.dseelp.kotlincord.api.Bot
import de.dseelp.kotlincord.api.Cord
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.ReloadScope
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.events.ReloadEvent
import de.dseelp.kotlincord.api.events.ShutdownEvent
import de.dseelp.kotlincord.api.logging.LogManager
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.PluginLoader
import de.dseelp.kotlincord.api.plugins.PluginManager
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import kotlin.system.exitProcess

@OptIn(InternalKotlinCordApi::class)
object CordImpl : Cord, CordKoinComponent {
    private val eventBus by inject<EventBus>()
    private val bot: Bot by inject()
    private val coreLog by logger(LogManager.CORE)
    private val loader: PluginLoader by inject()
    private val pluginService: PluginManager by inject()

    override suspend fun reload(vararg scopes: ReloadScope) {
        coreLog.info("Reloading...")
        eventBus.callAsync(ReloadEvent(scopes.toList().toTypedArray()))
        coreLog.info("Reload complete!")
    }

    override suspend fun shutdown() = shutdown(true)

    @InternalKotlinCordApi
    override suspend fun shutdown(unloadPlugins: Boolean) {
        coreLog.also { log ->
            log.info("Shutting down...")
            eventBus.callAsync(ShutdownEvent())
            ConsoleImpl.stopReading()
            ConsoleImpl.stopCurrentRead()
            Thread {
                Thread.sleep(10000)
                log.error("Shutdown took too long")
                log.error("Forcing shutdown...")
                exitProcess(1)
            }.start()
            bot.kord.shutdown()
            println("Shutdown complete")
        }
    }

    override fun getPlugin(): Plugin = FakePlugin
    override suspend fun reloadPlugins() {
        for (index in 0..loader.loadedPlugins.lastIndex) {
            val data = loader.loadedPlugins.getOrNull(index) ?: continue
            pluginService.unload(data)
        }
        val pluginLocation = Core.pathQualifiers.pluginLocation
        val file = pluginLocation.toFile()
        if (!file.exists()) file.mkdir()
        for (path in file.listFiles()!!) {
            try {
                val load = Core.pluginService.load(path)
                Core.pluginService.enable(load.plugin!!)
            } catch (t: Throwable) {
                Core.log.error("Failed to load plugin $path", t)
            }
        }
    }

    val formatter = SimpleDateFormat("dd.MM HH:mm:ss")
}