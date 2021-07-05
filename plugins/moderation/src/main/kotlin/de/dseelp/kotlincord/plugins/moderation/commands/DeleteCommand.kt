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
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.common.kColor
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.*
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.interaction.embed
import java.awt.Color

object DeleteCommand : Command<GuildSender> {
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.GUILD)
    @OptIn(KordPreview::class)
    val channelAction = ModerationPlugin.registerButtonAction("ChannelDelete", literal("") {
        checkAccess {
            sender.interaction.message?.getAuthorAsMember()?.checkPermissions(Permission.ManageChannels) ?: false
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
        argument(LongArgument("channel")) {
            map<Long, GuildMessageChannel?>("channel") {
                val channel = sender.interaction.message?.getGuild()?.getChannelOrNull(it.asSnowflake) ?: return@map null
                if (channel.type == ChannelType.GuildText || channel.type == ChannelType.GuildNews) return@map channel as GuildMessageChannel
                null
            }

            literal("cancel") {
                execute {
                    val channel: GuildMessageChannel? = get("channel")
                    sender.interaction.acknowledgePublicDeferredMessageUpdate().edit {
                        components = mutableListOf()
                        embed {
                            title = "Channel Deletion cancelled"
                            description = "The channel ${channel?.mention} wasn't deleted"
                        }
                    }
                }
            }

            execute {
                val channel: GuildMessageChannel? = get("channel")
                sender.interaction.acknowledgePublicDeferredMessageUpdate().edit {
                    components = mutableListOf()
                    embed {
                        title = "Channel Deleted"
                        description = "The channel ${channel?.name} was deleted!"
                    }
                }
                channel?.delete()
            }
        }
    })
    @OptIn(KordPreview::class)
    override val node: CommandNode<GuildSender> = literal("delete") {
        argument(MentionArgument.messageChannel("channel")) {
            execute {
                val deleted: GuildMessageChannel = get("channel")
                sender.message.delete()
                sender.sendMessage {
                    embed {
                        title = "Delete channel"
                        description = "Please confirm that the channel ${deleted.mention} should be deleted!"
                    }
                    actionRow {
                        action(channelAction, ButtonStyle.Secondary, "${deleted.id.asString} cancel", "Cancel")
                        action(channelAction, ButtonStyle.Danger, deleted.id.asString, "Delete")
                    }
                }
            }
        }
    }
}