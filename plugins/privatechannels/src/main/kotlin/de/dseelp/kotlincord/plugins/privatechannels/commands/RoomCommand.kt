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

package de.dseelp.kotlincord.plugins.privatechannels.commands

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.asSnowflake
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.setup.setup
import de.dseelp.kotlincord.api.utils.CommandScope
import de.dseelp.kotlincord.api.utils.green
import de.dseelp.kotlincord.api.utils.literal
import de.dseelp.kotlincord.api.utils.red
import de.dseelp.kotlincord.plugins.privatechannels.PrivateChannelPlugin
import de.dseelp.kotlincord.plugins.privatechannels.PrivateChannelPlugin.suspendingDatabase
import de.dseelp.kotlincord.plugins.privatechannels.db.ActivePrivateChannel
import de.dseelp.kotlincord.plugins.privatechannels.db.ActivePrivateChannels
import de.dseelp.kotlincord.plugins.privatechannels.db.PrivateChannel
import de.dseelp.kotlincord.plugins.privatechannels.db.PrivateChannels
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.VoiceChannel
import org.jetbrains.exposed.sql.and

class RoomCommand : Command<GuildSender> {
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.GUILD)

    @OptIn(InternalKotlinCordApi::class, dev.kord.common.annotation.KordPreview::class)
    override val node: CommandNode<GuildSender> = literal("room") {
        execute {

        }

        literal("create") {
            execute {
                val channel = sender.getChannel()
                setup(PrivateChannelPlugin, channel) {
                    voiceChannelStep {
                        embed {
                            title = "Create Private Channel"
                            description =
                                "Please tag a voice channel where the users should join to create a Private Channel \n To tag a voice channel use `#!Channel`"
                        }
                    }
                    messageStep {
                        embed {
                            title = "Create Private Channel"
                            description = """
                                Please enter a name template for the created channels.
                                The default template is: `%user%'s Room`
                            """.trimIndent()
                            field {
                                name = "%user%"
                                value = "The user name of the user creating the channel"
                            }
                            field {
                                name = "%game%"
                                value = "The game that the user creating the channel plays"
                            }
                        }
                    }
                    messageStep {
                        embed {
                            title = "Create Private Channel"
                            description = """
                                Please enter a text that is shown when someone not plays a game.
                                The default text is: `a Game`
                            """.trimIndent()
                        }
                    }
                    onCompletion { result ->
                        if (result.wasCancelled) {
                            channel.createEmbed {
                                title = "Private Channel creation cancelled"
                                description = "The creation of a private channel was cancelled"
                            }
                            return@onCompletion
                        }
                        val joinChannel = result.results[0] as VoiceChannel
                        if (joinChannel.asChannelOrNull() == null) return@onCompletion
                        suspendingDatabase {
                            suspendedTransaction {
                                val channelId = joinChannel.id.value
                                if (PrivateChannel.find { PrivateChannels.joinChannelId eq channelId }.count() != 0L) {
                                    channel.createEmbed {
                                        title = "Private Channel creation failed"
                                        color = Color.red
                                        description = "${joinChannel.mention} is already a Private Channel"
                                    }
                                    return@suspendedTransaction
                                }
                                PrivateChannel.new {
                                    joinChannelId = channelId
                                    guildId = joinChannel.guildId.value
                                    nameTemplate = result.results[1] as String
                                    defaultGame = result.results[2] as String
                                }
                                channel.createEmbed {
                                    title = "Private Channel created"
                                    color = Color.green
                                    description = "${joinChannel.mention} is now a Private Channel"
                                }
                            }
                        }
                    }
                }.start(true)
            }
        }

        literal("remove") {
            execute {
                suspendingDatabase {
                    val channel = sender.getChannel()
                    val guildId = channel.guildId.value
                    val guild = channel.guild.asGuild()
                    val (privateChannels, count) = suspendedTransaction {
                        val channels = PrivateChannel.find { PrivateChannels.guildId eq guildId }
                        channels to channels.count()
                    }
                    if (count != 0L) setup(PrivateChannelPlugin, channel) {
                        selectionStep {
                            message {
                                embed {
                                    title = "Remove PrivateChannel"
                                    description = "Please select the PrivateChannel you want to delete"
                                }
                            }
                            suspendedTransaction {
                                for (ch in privateChannels) {
                                    val channel2 = guild.getChannelOrNull(ch.joinChannelId.asSnowflake)
                                    if (channel2 == null) {
                                        ch.delete()
                                        continue
                                    }
                                    option(channel2.name, channel2.id)
                                }
                            }
                        }
                        buttonStep {
                            message {
                                embed {
                                    title = "Confirm Deletion"
                                    description =
                                        "Do you really want to delete this private channel. All users using it will be kicked out of their channels."
                                }
                            }
                            action(ButtonStyle.Danger, "Delete") { true }
                            cancelAction(ButtonStyle.Primary, "Cancel") { false }
                        }
                        onCompletion { result ->
                            if (channel.asChannelOrNull() == null) return@onCompletion
                            if (result.wasCancelled || result.results[1] == false) {
                                channel.createEmbed {
                                    title = "Private Channels"
                                    description = "No room was removed"
                                }
                                return@onCompletion
                            }
                            if (result.results[1] == true) {
                                @Suppress("UNCHECKED_CAST")
                                val snowflake = (result.results[0] as Array<Any?>)[0] as Snowflake
                                val pChannel = guild.getChannelOrNull(snowflake) ?: return@onCompletion
                                channel.createEmbed {
                                    title = "Private Channels"
                                    description = "The room ${pChannel.name} was removed"
                                }
                                pChannel.delete()
                                suspendedTransaction {
                                    val value = snowflake.value
                                    val privateChannel =
                                        PrivateChannel.find { (PrivateChannels.joinChannelId eq value) and (PrivateChannels.guildId eq guildId) }
                                            .firstOrNull() ?: return@suspendedTransaction
                                    ActivePrivateChannel.find { ActivePrivateChannels.privateChannel eq privateChannel.id }
                                        .onEach {
                                            guild.getChannelOrNull(it.channelId.asSnowflake)?.delete()
                                            it.delete()
                                        }
                                    privateChannel.delete()
                                }

                            }
                        }
                    }.start(true)
                    else {
                        sender.sendMessage {
                            embed {
                                title = "Error!"
                                description = "There are no private channels in this guild!"
                            }
                        }
                    }
                }
            }
        }
    }
}