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

package io.github.dseelp.framecord.core.commands

import de.dseelp.kommon.command.CommandNode
import de.dseelp.oauth2.discord.api.DiscordClient
import de.dseelp.oauth2.discord.api.entities.GuildPermission
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.create.embed
import io.github.dseelp.framecord.api.command.*
import io.github.dseelp.framecord.api.configs.BotConfig
import io.github.dseelp.framecord.api.logging.LogManager
import io.github.dseelp.framecord.api.logging.logger
import io.github.dseelp.framecord.api.plugins.DisableAutoLoad
import io.github.dseelp.framecord.api.utils.deleteAfter
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import io.github.dseelp.framecord.api.utils.literal
import io.github.dseelp.framecord.api.utils.red
import io.ktor.client.*
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
@DisableAutoLoad
class InviteCommand : Command<Sender>, CordKoinComponent {

    private lateinit var oauth2Client: DiscordClient
    override val description: String = "Sends an invite to invite the bot to your server"
    init {
        oauth2Client()
    }

    private fun oauth2Client() {
        val inviteConfig = getConfig()
        oauth2Client = DiscordClient(HttpClient(), inviteConfig.clientId.toString(), "", "")
    }

    private fun checkClient(config: BotConfig.InviteConfig = getConfig()) {
        if (oauth2Client.clientId != config.clientId.toString()) oauth2Client()
    }

    companion object : CordKoinComponent {
        internal fun getConfig(): BotConfig.InviteConfig {
            val config: BotConfig by inject()
            return config.invite
        }
    }

    val logger by logger(LogManager.ROOT)

    override val scopes: Array<CommandScope> = arrayOf(CommandScope.GUILD, CommandScope.PRIVATE, CommandScope.CONSOLE)

    @OptIn(ExperimentalTime::class)
    override val node: CommandNode<Sender> = literal("invite") {
        checkAccess {
            getConfig().enabled
        }
        execute {
            val inviteConfig = getConfig()
            if (!inviteConfig.enabled || inviteConfig.clientId <= 0) {
                if (sender is DiscordSender<*>) {
                    (sender as DiscordSender<*>).getChannel().createMessage {
                        embed {
                            color = Color.red
                            title = "Error!"
                            description = "The invite command is disabled!"
                        }
                    }.deleteAfter(seconds(5000))
                    return@execute
                } else {
                    logger.warn("The invite command is disabled in the config")
                }

            }
            checkClient(inviteConfig)
            val link = oauth2Client.createBotInvite(GuildPermission.ADMINISTRATOR)
            if (sender is DiscordSender<*>) {
                (sender as DiscordSender<*>).createEmbed {
                    title = "FrameCord"
                    description = "You can invite the bot by clicking [this link]($link)"
                }
            } else sender.sendMessage("You can invite the bot by using $link")
        }
    }
}