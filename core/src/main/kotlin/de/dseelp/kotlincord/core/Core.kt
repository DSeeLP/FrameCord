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
import de.dseelp.kotlincord.core.plugin.repository.RepositoryLoaderImpl
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
        CordImpl.reloadPlugins()
        ConsoleImpl.startReading()
        loadKoinModules(module {
            single { pathQualifiers.root }
        })
        loadToken()
        eventBus.addClassHandler(FakePlugin, CoreListener)
        try {
            FakePlugin.repositoryManager.addLoader(FakePlugin, RepositoryLoaderImpl())
            FakePlugin.repositoryManager.reloadRepositories()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        FakePlugin.enable()
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
            single { config }
            single(qualifier("instanceId")) { config.instanceId }
            single(qualifier("config")) { this@module }
            single(qualifier("debugMode")) { config.debug }
        })
    }


}