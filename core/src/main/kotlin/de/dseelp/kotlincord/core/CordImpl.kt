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
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import kotlin.system.exitProcess

@OptIn(InternalKotlinCordApi::class)
object CordImpl : Cord, CordKoinComponent {
    val eventBus by inject<EventBus>()
    val bot: Bot by inject()
    val coreLog by logger(LogManager.CORE)

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

    val formatter = SimpleDateFormat("dd.MM HH:mm:ss")
}