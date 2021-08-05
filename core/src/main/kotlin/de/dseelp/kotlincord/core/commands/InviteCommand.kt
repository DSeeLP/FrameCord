/*
 * Copyright (c) 2021 DSeeLP & KotlinCord contributors
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

package de.dseelp.kotlincord.core.commands

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.command.CommandScope
import de.dseelp.kotlincord.api.command.DiscordSender
import de.dseelp.kotlincord.api.command.createEmbed
import de.dseelp.kotlincord.api.configs.BotConfig
import de.dseelp.kotlincord.api.plugins.DisableAutoLoad
import de.dseelp.kotlincord.api.utils.deleteIgnoringNotFound
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.api.utils.literal
import de.dseelp.kotlincord.api.utils.red
import de.dseelp.oauth2.discord.api.DiscordClient
import de.dseelp.oauth2.discord.api.entities.GuildPermission
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.*
import kotlinx.coroutines.delay
import org.koin.core.component.inject

@OptIn(InternalKotlinCordApi::class)
@DisableAutoLoad
class InviteCommand : Command<DiscordSender<MessageChannel>>, CordKoinComponent {

    private lateinit var oauth2Client: DiscordClient

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

    override val scopes: Array<CommandScope> = arrayOf(CommandScope.GUILD, CommandScope.PRIVATE)
    override val node: CommandNode<DiscordSender<MessageChannel>> = literal("invite") {
        checkAccess {
            getConfig().enabled
        }
        execute {
            val inviteConfig = getConfig()
            if (!inviteConfig.enabled || inviteConfig.clientId <= 0) {
                val message = sender.getChannel().createMessage {
                    embed {
                        color = Color.red
                        title = "Error!"
                        description = "The invite command is disabled!"
                    }
                }
                delay(5000)
                message.deleteIgnoringNotFound()
                return@execute
            }
            checkClient(inviteConfig)
            val link = oauth2Client.createBotInvite(GuildPermission.ADMINISTRATOR)
            sender.createEmbed {
                title = "KotlinCord"
                description = "You can invite the bot by clicking [this link]($link)"
            }
        }
    }
}