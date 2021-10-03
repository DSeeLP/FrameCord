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

import com.log4k.LoggerConfig
import com.log4k.configuration
import com.log4k.e
import com.log4k.i
import io.github.dseelp.framecord.api.Cord
import io.github.dseelp.framecord.api.Version
import io.github.dseelp.framecord.api.event.EventBus
import io.github.dseelp.framecord.api.events.ReloadEvent
import io.github.dseelp.framecord.api.events.ShutdownEvent
import io.github.dseelp.framecord.api.plugins.Plugin
import io.github.dseelp.framecord.api.plugins.PluginLoader
import io.github.dseelp.framecord.api.plugins.PluginManager
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import kotlinx.coroutines.cancelAndJoin
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.exitProcess

@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
@LoggerConfig(tag = "Core")
object CordImpl : Cord, CordKoinComponent {
    private val eventBus by inject<EventBus>()
    private val bot: io.github.dseelp.framecord.api.Bot by inject()
    private val loader: PluginLoader by inject()
    private val pluginService: PluginManager by inject()
    override val version: Version = CordBootstrap.version
    private val cCfg = configuration(Cord::class)

    override suspend fun reload(vararg scopes: io.github.dseelp.framecord.api.ReloadScope) {
        i("Reloading...", cCfg)
        eventBus.callAsync(ReloadEvent(scopes.toList().toTypedArray()))
        i("Reload complete!", cCfg)
    }

    override suspend fun shutdown() = shutdown(true)

    @io.github.dseelp.framecord.api.InternalFrameCordApi
    override suspend fun shutdown(unloadPlugins: Boolean) {
        i("Shutting down...")
        eventBus.callAsync(ShutdownEvent())
        ConsoleImpl.stopReading()
        ConsoleImpl.stopCurrentRead()
        Thread {
            Thread.sleep(10000)
            e("Shutdown took too long")
            e("Forcing shutdown...")
            exitProcess(1)
        }.start()
        i("Canceling Bot Job")
        bot.job.cancelAndJoin()
        i("Bot Job cancelled")
        bot.kord.shutdown()
        println("Shutdown complete")
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
                if (!(path.isFile && path.extension == "jar")) continue
                val load = Core.pluginService.load(path)
                Core.pluginService.enable(load.plugin!!)
            } catch (t: Throwable) {
                e("Failed to load plugin $path", t, config = configuration(Core::class))
            }
        }
    }

    override val guildCount: ULong
        get() = _guildCount.get().toULong()

    var _guildCount: AtomicLong = AtomicLong(0)

    val formatter = SimpleDateFormat("dd.MM HH:mm:ss")
}