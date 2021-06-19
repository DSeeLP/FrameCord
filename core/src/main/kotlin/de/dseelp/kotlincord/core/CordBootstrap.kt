/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kotlincord.api.Cord
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.Version
import de.dseelp.kotlincord.api.command.ConsoleSender
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.command.PrivateSender
import de.dseelp.kotlincord.api.console.Console
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.logging.LogManager.ROOT
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.PluginLoader
import de.dseelp.kotlincord.api.plugins.PluginManager
import de.dseelp.kotlincord.api.utils.koin.CordKoinContext
import de.dseelp.kotlincord.core.plugin.PluginLoaderImpl
import de.dseelp.kotlincord.core.plugin.PluginManagerImpl
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.qualifier
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.slf4j.bridge.SLF4JBridgeHandler
import java.io.PrintStream
import java.net.URLClassLoader
import java.util.logging.LogManager

@OptIn(InternalKotlinCordApi::class)
object CordBootstrap {
    val log by logger(ROOT)
    val version = Version(0, 0, 1)

    val defaultModule = module {
        single<Console> { ConsoleImpl }
        single<Cord> { CordImpl }
        single { EventBus() }
        single(qualifier("console")) { CommandDispatcher<ConsoleSender>() }
        single(qualifier("guild")) { CommandDispatcher<GuildSender>() }
        single(qualifier("private")) { CommandDispatcher<PrivateSender>() }
        single<PluginLoader> { PluginLoaderImpl() }
        single<PluginManager> { PluginManagerImpl() }
        single<URLClassLoader>(qualifier("pluginClassLoader")) { URLClassLoader.newInstance(arrayOf()) }
        single { version }
    }

    val defaultModules = listOf(defaultModule, PathQualifiersImpl.module)

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        ConsoleImpl.terminal
        ConsoleImpl.reader

        val koinApp = koinApplication {
            modules(defaultModules)
            allowOverride(true)
        }
        CordKoinContext.app = koinApp
        System.setOut(PrintStream(ConsoleImpl.ActionOutputStream { ConsoleImpl.forceWriteLine(it) }, true))
        System.setErr(PrintStream(ConsoleImpl.ActionOutputStream { ConsoleImpl.forceWriteLine(it) }, true))
        ConsoleImpl.replaceSysOut()
        LogManager.getLogManager().getLogger("").apply {
            removeHandler(handlers[0])
            addHandler(SLF4JBridgeHandler())
        }
        Core.startup()
    }
}