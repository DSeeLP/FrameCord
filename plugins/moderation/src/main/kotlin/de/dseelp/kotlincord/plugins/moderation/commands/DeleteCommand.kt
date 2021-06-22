/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.plugins.moderation.commands

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.arguments.LongArgument
import de.dseelp.kommon.command.literal
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.command.arguments.MentionArgument
import de.dseelp.kotlincord.api.utils.CommandScope
import de.dseelp.kotlincord.api.utils.embed
import de.dseelp.kotlincord.api.utils.messageBuilder
import de.dseelp.kotlincord.plugins.moderation.ModerationPlugin
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.interactions.components.ButtonStyle
import java.util.concurrent.TimeUnit

object DeleteCommand : Command<GuildSender> {
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.GUILD)
    val channelAction = ModerationPlugin.registerButtonAction("ChannelDelete", literal("") {
        checkAccess {
            sender.event.member?.user?.asTag == "DSeeLP#3721"
        }
        noAccess {
            sender.event.message?.editMessage(embed {
                title = "Haha"
                description = "You are not the King ${sender.event.user.asMention}! HAHAHA"
            })?.queue()
        }
        argument(LongArgument("channel")) {
            map<Long, TextChannel?>("channel") {
                sender.event.guild?.getTextChannelById(it)
            }

            literal("cancel") {
                execute {
                    val channel: TextChannel? = get("channel")
                    sender.event.message?.editMessage(messageBuilder {
                        actionRows.clear()
                        embed {
                            title = "Channel Deletion cancelled"
                            description = "The channel ${channel?.asMention} wasn't deleted"
                        }
                    }.build())?.queue { it.delete().queueAfter(5, TimeUnit.SECONDS) }
                }
            }

            execute {
                val channel: TextChannel? = get("channel")
                sender.event.message?.editMessage(messageBuilder {
                    actionRows.clear()
                    embed {
                        title = "Channel Deleted"
                        description = "The channel ${channel?.name} was deleted!"
                    }
                }.build())?.queue { it.delete().queueAfter(5, TimeUnit.SECONDS) }
                channel?.delete()?.queue()
            }
        }
    })
    override val node: CommandNode<GuildSender> = literal("delete") {
        argument(MentionArgument.messageChannel("channel")) {
            execute {
                val deleted: TextChannel = get("channel")
                sender.message.delete().queue()
                sender.channel.sendMessage(messageBuilder {
                    embed {
                        title = "Delete channel"
                        description = "Please confirm that the channel ${deleted.asMention} should be deleted!"
                    }
                    actionRow {
                        action(channelAction, ButtonStyle.SECONDARY, "${deleted.idLong} cancel", "Cancel")
                        action(channelAction, ButtonStyle.DANGER, "${deleted.idLong}", "Delete")
                    }
                }.build()).queue()
            }
        }
    }
}