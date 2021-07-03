/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kotlincord.api.Dispatcher
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.PathQualifiers
import de.dseelp.kotlincord.api.command.Sender
import de.dseelp.kotlincord.api.configs.BotConfig
import de.dseelp.kotlincord.api.configs.ConfigFormat
import de.dseelp.kotlincord.api.configs.config
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.logging.KLogger
import de.dseelp.kotlincord.api.logging.LogManager.CORE
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.PluginManager
import de.dseelp.kotlincord.api.randomAlphanumeric
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.core.listeners.CoreListener
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import org.spongepowered.configurate.kotlin.extensions.get
import kotlin.io.path.div

@OptIn(InternalKotlinCordApi::class)
object Core : CordKoinComponent {

    val log by logger(CORE)
    val pluginService by inject<PluginManager>()
    val guildDispatcher: CommandDispatcher<Sender> by inject(qualifier("guild"))
    val consoleDispatcher: Dispatcher by inject(qualifier("console"))
    val pathQualifiers by inject<PathQualifiers>()
    private val eventBus by inject<EventBus>()


    suspend fun startup() {
        loadConfig()
        val pluginLocation = pathQualifiers.pluginLocation
        val file = pluginLocation.toFile()
        if (!file.exists()) file.mkdir()
        for (path in file.listFiles()!!) {
            if (!path.isFile) continue
            try {
                val load = pluginService.load(path)
                pluginService.enable(load.plugin!!)
            } catch (t: Throwable) {
                log.error("Failed to load plugin $path", t)
            }
        }
        loadKoinModules(module {
            single { pathQualifiers.root }
        })
        loadToken()
        ConsoleImpl.startReading()
        eventBus.addClassHandler(FakePlugin, CoreListener)
        BotImpl.start()
    }

    fun loadToken(log: KLogger = logger(CORE).value) {
        log.info("Loading Bot Token")
        val cfg = config(ConfigFormat.JSON, pathQualifiers.root / "token.json") {
            defaults {
                get(TokenConfig("ENTER TOKEN HERE"))
            }
        }.node
        val tokenConfig = cfg.get<TokenConfig>()!!
        loadKoinModules(module {
            single(qualifier("token")) { cfg }
            single(qualifier("token")) { tokenConfig.token }
            single(qualifier("token")) { this }
        })
    }

    fun loadConfig(log: KLogger = logger(CORE).value) {
        log.info("Loading Config")
        val cfg = config(ConfigFormat.JSON, pathQualifiers.root / "config.json") {
            defaults {
                get(BotConfig(randomAlphanumeric(4), false))
            }
        }.node
        val config = cfg.get<BotConfig>()!!
        System.setProperty("debugMode", config.debug.toString())
        loadKoinModules(module {
            single(qualifier("config")) { cfg }
            single(qualifier("instanceId")) { config.instanceId }
            single(qualifier("config")) { this@module }
            single(qualifier("debugMode")) { config.debug }
        })
    }


}