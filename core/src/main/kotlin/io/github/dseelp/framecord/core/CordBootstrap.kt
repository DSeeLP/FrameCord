/*
 * Copyright (c) 2021 DSeeLP & FrameCord contributors
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

package io.github.dseelp.framecord.core

import de.dseelp.kommon.command.CommandDispatcher
import io.github.dseelp.framecord.api.Version
import io.github.dseelp.framecord.api.command.ConsoleSender
import io.github.dseelp.framecord.api.command.GuildSender
import io.github.dseelp.framecord.api.command.PrivateSender
import io.github.dseelp.framecord.api.console.Console
import io.github.dseelp.framecord.api.database.DatabaseRegistry
import io.github.dseelp.framecord.api.event.EventBus
import io.github.dseelp.framecord.api.guild.GuildManager
import io.github.dseelp.framecord.api.logging.LogManager.ROOT
import io.github.dseelp.framecord.api.logging.logger
import io.github.dseelp.framecord.api.modules.ModuleManager
import io.github.dseelp.framecord.api.plugins.PluginLoader
import io.github.dseelp.framecord.api.plugins.PluginManager
import io.github.dseelp.framecord.api.plugins.repository.RepositoryManager
import io.github.dseelp.framecord.api.setup.SetupManager
import io.github.dseelp.framecord.api.utils.IReflectionUtils
import io.github.dseelp.framecord.api.utils.koin.CordKoinContext
import io.github.dseelp.framecord.core.database.DatabaseRegistryImpl
import io.github.dseelp.framecord.core.modules.ModuleManagerImpl
import io.github.dseelp.framecord.core.plugin.PluginLoaderImpl
import io.github.dseelp.framecord.core.plugin.PluginManagerImpl
import io.github.dseelp.framecord.core.plugin.repository.RepositoryManagerImpl
import io.github.dseelp.framecord.core.utils.ReflectionUtilsImpl
import kotlinx.coroutines.*
import org.koin.core.qualifier.qualifier
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.slf4j.bridge.SLF4JBridgeHandler
import java.io.IOException
import java.io.PrintStream
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.logging.LogManager


@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
object CordBootstrap {
    val log by logger(ROOT)
    val manifestInfo: Properties?
        get() {
            val resEnum: Enumeration<*>
            try {
                resEnum = Thread.currentThread().contextClassLoader.getResources(JarFile.MANIFEST_NAME)
                while (resEnum.hasMoreElements()) {
                    try {
                        val url = resEnum.nextElement() as URL
                        val `is` = url.openStream()
                        if (`is` != null) {
                            val manifest = Manifest(`is`)
                            val mainAttribs = manifest.mainAttributes
                            val properties = Properties()
                            for (entry in mainAttribs.entries) {
                                properties.setProperty(entry.key?.toString(), entry.value?.toString())
                            }
                            return properties
                        }
                    } catch (e: Exception) { }
                }
            } catch (e1: IOException) { }
            return null
        }

    val version: Version

    init {
        val info = manifestInfo
        val default = Version(0, 4, 1)
        var temp = default
        if (info != null && info.contains("prodBuild")) {
            temp = try {
                Version.parse(info.getProperty("prodBuild"))
            } catch (ex: Exception) {
                System.err.println("Failed to parse prodBuild from Manifest! Falling back to hardcoded value")
                default
            }
        }
        version = temp
    }

    val defaultModule = module {
        single<Console> { ConsoleImpl } bind ConsoleImpl::class
        single<io.github.dseelp.framecord.api.Cord> { CordImpl } bind CordImpl::class
        single<EventBus> { StaticBus }
        single<io.github.dseelp.framecord.api.Bot> { BotImpl } bind BotImpl::class
        single<DatabaseRegistry> { DatabaseRegistryImpl() } bind DatabaseRegistryImpl::class
        single<RepositoryManager> { RepositoryManagerImpl() } bind RepositoryManagerImpl::class
        single(qualifier("console")) { CommandDispatcher<ConsoleSender>() }
        single(qualifier("guild")) { CommandDispatcher<GuildSender>() }
        single(qualifier("private")) { CommandDispatcher<PrivateSender>() }
        single(qualifier("thread")) { CommandDispatcher<PrivateSender>() }
        single<PluginLoader> { StaticPluginLoader } bind PluginLoaderImpl::class
        single<PluginManager> { StaticPluginManager } bind PluginManagerImpl::class
        single<URLClassLoader>(qualifier("pluginClassLoader")) { URLClassLoader.newInstance(arrayOf()) }
        single<IReflectionUtils> { ReflectionUtilsImpl() } bind ReflectionUtilsImpl::class
        single { version }
        single<GuildManager> { StaticGuildManger }
        single<SetupManager> { StaticSetupManager }
        single<ModuleManager> { ModuleManagerImpl } bind ModuleManagerImpl::class
    }

    val defaultModules = listOf(defaultModule, PathQualifiersImpl.module)

    @OptIn(DelicateCoroutinesApi::class)
    @JvmStatic
    fun main(args: Array<String>) {
        println(manifestInfo?.map { "${it.key} - ${it.value}" }?.joinToString(System.lineSeparator()))
        runBlocking {
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
            CoroutineExceptionHandler { coroutineContext, throwable ->

            }
            LogManager.getLogManager().getLogger("").apply {
                removeHandler(handlers[0])
                addHandler(SLF4JBridgeHandler())
            }
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                //log.debug("Uncaught exception handled in thread handler")
                log.error("", throwable)
            }
            GlobalScope.launch {
                Core.startup()
            }.join()
        }
    }
}