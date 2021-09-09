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

import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.gateway.editPresence
import io.github.dseelp.framecord.api.bot
import io.github.dseelp.framecord.api.configs.BotConfig
import io.github.dseelp.framecord.api.logging.logger
import io.github.dseelp.framecord.api.modules.ModuleManager
import io.github.dseelp.framecord.api.placeholders.PlaceholderContext
import io.github.dseelp.framecord.api.placeholders.PlaceholderContext.*
import io.github.dseelp.framecord.api.placeholders.PlaceholderManager
import io.github.dseelp.framecord.api.placeholders.PlaceholderType
import io.github.dseelp.framecord.api.presence.Activity
import io.github.dseelp.framecord.api.presence.Presence
import io.github.dseelp.framecord.api.presence.PresenceManager
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import io.github.dseelp.framecord.core.listeners.EventBusListener
import kotlinx.coroutines.*
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier
import kotlin.coroutines.CoroutineContext

@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
object BotImpl : io.github.dseelp.framecord.api.Bot, CordKoinComponent {
    override val kord: Kord
        get() = _kord!!
    override val isStarted: Boolean
        get() = _kord != null
    override val job: Job = SupervisorJob()
    override val moduleManager: ModuleManager by inject()
    override val coroutineContext: CoroutineContext = job + Dispatchers.Default

    var _kord: Kord? = null

    val logger by logger<io.github.dseelp.framecord.api.Bot>()

    @OptIn(PrivilegedIntent::class)
    suspend fun start() {
        val token by inject<String>(qualifier("token"))
        val config by inject<BotConfig>()
        val intentConfig = config.intents
        _kord = Kord(token) {
            intents = Intents {
                +Intents.all
                if (!intentConfig.presence) -Intent.GuildPresences
                if (!intentConfig.guildMembers) -Intent.GuildMembers
            }
        }
        bot.launch {
            kord.login {
                status = PresenceStatus.DoNotDisturb
                playing("Starting...")
            }
        }
        bot.launch {
            delay(1000)
            val presenceManager by inject<PresenceManager>()
            var currentPresence: Presence?
            var currentIndex = 0
            var isFirst = true
            loop@ while (isActive) {
                val all = presenceManager.getAll()
                if (all.isEmpty()) {
                    delay(1000)
                    continue
                }
                if (!bot.isStarted) {
                    delay(100)
                    continue
                }
                var runs = 0
                do {
                    runs++
                    if (!isFirst) currentIndex++
                    else isFirst = false
                    if (currentIndex > all.lastIndex) currentIndex = 0
                    currentPresence = presenceManager.getAll().getOrNull(currentIndex)
                    if (runs > 10 && (currentPresence == null || !currentPresence.enabled)) currentPresence = Presence(
                        io.github.dseelp.framecord.api.presence.PresenceStatus.ONLINE,
                        Activity.Playing("FrameCord v${CordImpl.version}"),
                        10000,
                        true
                    )
                } while (currentPresence?.enabled == false)
                if (currentPresence == null) continue@loop
                val baseArguments = mapOf(Arguments.Cord.Version to CordImpl.version)
                bot.kord.gateway.gateways.onEach {
                    val gateway = it.value
                    val arguments = baseArguments + mapOf(Parameters.Shard.Id to it.key)
                    gateway.editPresence {
                        val activity = currentPresence.activity
                        status = currentPresence.status.status

                        val processedName = PlaceholderManager.replacePlaceholders(arguments, PlaceholderType.PRESENCE, activity.name)
                        when (activity) {
                            is Activity.Competing -> competing(processedName)
                            is Activity.Listening -> listening(processedName)
                            is Activity.Playing -> playing(processedName)
                            is Activity.Streaming -> streaming(processedName, PlaceholderManager.replacePlaceholders(arguments, PlaceholderType.PRESENCE, activity.url))
                            is Activity.Watching -> watching(processedName)
                        }
                    }
                }
                delay(currentPresence.stayTime)
            }
        }
        EventBusListener
        logger.info("Startup complete")
    }
}