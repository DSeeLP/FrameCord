/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.json.toJson
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.PathQualifiers
import de.dseelp.kotlincord.api.bot
import de.dseelp.kotlincord.api.configs.BotConfig
import de.dseelp.kotlincord.api.configs.file
import de.dseelp.kotlincord.api.configs.toFile
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.logging.KLogger
import de.dseelp.kotlincord.api.logging.LogManager.CORE
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.PluginManager
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.core.listeners.CoreListener
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import kotlin.io.path.div

@OptIn(InternalKotlinCordApi::class)
object Core : CordKoinComponent {

    val log by logger(CORE)
    val pluginService by inject<PluginManager>()
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
        ConsoleImpl.startReading()
        loadKoinModules(module {
            single { pathQualifiers.root }
        })
        loadToken()
        eventBus.addClassHandler(FakePlugin, CoreListener)
        BotImpl.start()
        log.info("Startup complete")
        bot.job.join()
    }

    fun loadToken(log: KLogger = logger(CORE).value) {
        log.info("Loading Bot Token")
        val path = pathQualifiers.root / "token.json"
        val cfg = Config { addSpec(TokenConfig) }.from.json.file(path)
            .apply { toJson.toFile(path) }.from.systemProperties().from.env()
        val tokenConfig = TokenConfig.fromConfig(cfg)
        loadKoinModules(module {
            single(qualifier("token")) { cfg }
            single(qualifier("token")) { tokenConfig.token }
            single(qualifier("token")) { this }
        })
    }

    fun loadConfig(log: KLogger = logger(CORE).value) {
        log.info("Loading Config")
        val path = pathQualifiers.root / "config.json"
        val cfg = Config { addSpec(BotConfig) }.from.json.file(path)
        cfg.toJson.toFile(path)
        val config = BotConfig.fromConfig(cfg)
        System.setProperty("debugMode", config.debug.toString())
        loadKoinModules(module {
            single(qualifier("instanceId")) { config.instanceId }
            single(qualifier("config")) { this@module }
            single(qualifier("debugMode")) { config.debug }
        })
    }


}