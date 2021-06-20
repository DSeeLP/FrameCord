package de.dseelp.kotlincord.plugins.moderation.commands

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.arguments.LongArgument
import de.dseelp.kommon.command.literal
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.command.arguments.MentionArgument
import de.dseelp.kotlincord.api.utils.CommandScope
import de.dseelp.kotlincord.api.utils.EmbedBuilder
import de.dseelp.kotlincord.api.utils.checkPermission
import de.dseelp.kotlincord.api.utils.messageBuilder
import de.dseelp.kotlincord.plugins.moderation.ModerationPlugin
import kotlinx.datetime.Clock
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.components.ButtonStyle
import java.awt.Color
import java.util.concurrent.TimeUnit

object KickCommand : Command<GuildSender> {
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.GUILD)
    val channelAction = ModerationPlugin.registerButtonAction("UserKick", literal("") {
        checkAccess {
            sender.event.member?.checkPermission(Permission.KICK_MEMBERS) ?: false
        }
        noAccess {
            sender.event.reply(messageBuilder {
                embed {
                    color = Color.RED
                    title = "Permission Denied"
                    description = "You are not allowed to use this button!"
                }
                actionRows.clear()
            }.build()).setEphemeral(true).queue()
        }
        argument(LongArgument("user")) {
            map<Long, Member?>("user") {
                sender.event.guild?.retrieveMemberById(it)?.complete()
            }

            literal("cancel") {
                execute {
                    val author = sender.event.user
                    val user: Member? = get("user")
                    sender.event.message?.editMessage(messageBuilder {
                        actionRows.clear()
                        embed {
                            title = "User Kick cancelled"
                            description = "The user ${user?.asMention} wont be kicked out of the server"
                            footer = EmbedBuilder.Footer(author.name, author.effectiveAvatarUrl)
                            timestamp = Clock.System.now()
                        }
                    }.build())?.queue { it.delete().queueAfter(10, TimeUnit.SECONDS) }
                }
            }

            execute {
                val member: Member? = get("user")
                val user = member?.user
                sender.event.message?.editMessage(messageBuilder {
                    actionRows.clear()
                    embed {
                        title = "User kicked"
                        description = "The user ${user?.asMention} was kicked out of the server!"
                    }
                }.build())?.queue { it.delete().queueAfter(20, TimeUnit.SECONDS) }
                member?.kick()?.queue()
            }
        }
    })
    override val node: CommandNode<GuildSender> = literal("kick") {
        argument(MentionArgument.user("user")) {
            execute {
                val deleted: Member = sender.guild.retrieveMember(get("user")).complete()
                sender.message.delete().queue()
                sender.channel.sendMessage(messageBuilder {
                    embed {
                        title = "Kick user"
                        description = "Please confirm that the user ${deleted.asMention} should be kicked!"
                    }
                    actionRow {
                        action(channelAction, ButtonStyle.SECONDARY, "${deleted.idLong} cancel", "Cancel")
                        action(channelAction, ButtonStyle.DANGER, "${deleted.idLong}", "Kick")
                    }
                }.build()).queue()
            }
        }
    }
}