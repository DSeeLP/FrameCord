/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.plugins.moderation.commands

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.arguments.LongArgument
import de.dseelp.kommon.command.literal
import de.dseelp.kotlincord.api.action
import de.dseelp.kotlincord.api.asSnowflake
import de.dseelp.kotlincord.api.checkPermissions
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.command.arguments.MentionArgument
import de.dseelp.kotlincord.api.utils.CommandScope
import de.dseelp.kotlincord.plugins.moderation.ModerationPlugin
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.kColor
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.Member
import dev.kord.rest.builder.interaction.embed
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.awt.Color
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.seconds

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
                sender.sendMessage {
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