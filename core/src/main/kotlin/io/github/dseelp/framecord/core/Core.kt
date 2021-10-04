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

import com.log4k.e
import com.log4k.i
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.json.toJson
import io.github.dseelp.framecord.api.bot
import io.github.dseelp.framecord.api.configs.BotConfig
import io.github.dseelp.framecord.api.configs.file
import io.github.dseelp.framecord.api.configs.toFile
import io.github.dseelp.framecord.api.event.EventBus
import io.github.dseelp.framecord.api.plugins.PluginManager
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import io.github.dseelp.framecord.core.listeners.CoreListener
import io.github.dseelp.framecord.core.logging.ErrorManagerImpl
import io.github.dseelp.framecord.core.plugin.repository.RepositoryLoaderImpl
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import kotlin.io.path.div

@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
object Core : CordKoinComponent {

    val pluginService by inject<PluginManager>()
    val pathQualifiers by inject<io.github.dseelp.framecord.api.PathQualifiers>()
    private val eventBus by inject<EventBus>()


    suspend fun startup() {
        loadConfig()
        val config: BotConfig by inject()
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
        ErrorManagerImpl
        FakePlugin.enable()
        CordImpl.reloadPlugins()
        BotImpl.start()
        i("Startup complete")
        bot.job.join()
    }

    fun loadToken() {
        i("Loading Bot Token")
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

    suspend fun loadConfig() {
        i("Loading Config")
        val path = pathQualifiers.root / "config.json"
        val cfg = Config { addSpec(BotConfig) }.from.json.file(path)
        cfg.toJson.toFile(path)
        val config = BotConfig.fromConfig(cfg)
        if (config.instanceId.length > 4) {
            e("The length of an instanceId should not exceed 4 characters")
            CordImpl.shutdown()
            return
        }
        System.setProperty("showErrors", config.logging.showErrors.toString())
        loadKoinModules(module {
            single { config }
            single(qualifier("instanceId")) { config.instanceId }
            single(qualifier("config")) { this@module }
        })
    }


}