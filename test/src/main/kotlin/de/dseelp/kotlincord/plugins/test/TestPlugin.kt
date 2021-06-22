/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.plugins.test

import de.dseelp.kommon.command.arguments.LongArgument
import de.dseelp.kommon.command.literal
import de.dseelp.kotlincord.api.command.DiscordSender
import de.dseelp.kotlincord.api.command.arguments.MentionArgument
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.PluginAction
import de.dseelp.kotlincord.api.plugins.PluginInfo
import de.dseelp.kotlincord.api.utils.CommandScope
import de.dseelp.kotlincord.api.utils.messageBuilder
import de.dseelp.kotlincord.api.utils.register
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.interactions.components.ButtonStyle
import java.util.concurrent.TimeUnit

@PluginInfo("de.dseelp.kotlincord", "Test", "0.0.1", "This is a testing module", ["DSeeLP"])
object TestPlugin : Plugin() {

    @PluginAction(PluginAction.Action.ENABLE)
    fun enable() {
        println("Enabled plugin test")
        register(literal("testplugin") {
            execute {
                sender.sendMessage("Hi")
            }
        }, CommandScope.ALL)
        val channelAction = registerButtonAction("ChannelDelete", literal("") {
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
        register(literal<DiscordSender<TextChannel>>("delete") {
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
        }, CommandScope.GUILD)
    }

}