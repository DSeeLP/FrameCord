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

package io.github.dseelp.framecord.plugins.moderation.commands

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.arguments.LongArgument
import de.dseelp.kommon.command.literal
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.kColor
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.Member
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import io.github.dseelp.framecord.api.action
import io.github.dseelp.framecord.api.asSnowflake
import io.github.dseelp.framecord.api.checkPermissions
import io.github.dseelp.framecord.api.command.Command
import io.github.dseelp.framecord.api.command.CommandScope
import io.github.dseelp.framecord.api.command.GuildSender
import io.github.dseelp.framecord.api.command.arguments.MentionArgument
import io.github.dseelp.framecord.api.command.createMessage
import io.github.dseelp.framecord.plugins.moderation.ModerationPlugin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.awt.Color
import kotlin.time.Duration

object KickCommand : Command<GuildSender> {
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.GUILD)

    @OptIn(KordPreview::class, kotlin.time.ExperimentalTime::class)
    val channelAction = ModerationPlugin.registerButtonAction("UserKick", literal("") {
        checkAccess {
            sender.interaction.message?.getAuthorAsMember()?.checkPermissions(Permission.KickMembers) ?: false
        }
        noAccess {
            sender.interaction.acknowledgeEphemeral().followUpEphemeral {
                embed {
                    color = Color.RED.kColor
                    title = "Permission Denied"
                    description = "You are not allowed to use this button!"
                }
            }
        }
        argument(LongArgument("user")) {
            map<Long, Member?>("user") {
                sender.interaction.message?.getGuild()?.getMemberOrNull(it.asSnowflake)
            }

            literal("cancel") {
                execute {
                    val author = sender.interaction.user.asUserOrNull()
                    val user: Member? = get("user")
                    sender.interaction.acknowledgePublicDeferredMessageUpdate().edit {
                        components = mutableListOf()
                        embed {
                            title = "User Kick cancelled"
                            description = "The user ${user?.mention} wont be kicked out of the server"
                            footer {
                                text = author?.username.toString()
                                icon = author?.avatar?.url.toString()
                            }
                            timestamp = Clock.System.now()
                        }
                    }
                }
            }

            execute {
                val member: Member? = get("user")
                val msg = sender.interaction.acknowledgePublicDeferredMessageUpdate().edit {
                    components = mutableListOf()
                    embed {
                        title = "User kicked"
                        description = "The user ${member?.mention} was kicked out of the server!"
                    }
                }
                member?.kick()
                coroutineScope {
                    launch {
                        delay(Duration.seconds(20))
                        msg.delete()
                    }
                }
            }
        }
    })

    @OptIn(KordPreview::class)
    override val node: CommandNode<GuildSender> = literal("kick") {
        argument(MentionArgument.member("member")) {
            execute {
                val member: Member = get("member") ?: return@execute
                sender.message.delete()
                sender.createMessage {
                    embed {
                        title = "Kick user"
                        description = "Please confirm that the user ${member.mention} should be kicked!"
                    }
                    actionRow {
                        action(channelAction, ButtonStyle.Secondary, "${member.id.value} cancel", "Cancel")
                        action(channelAction, ButtonStyle.Danger, "${member.id.value}", "Kick")
                    }
                }
            }
        }
    }
}