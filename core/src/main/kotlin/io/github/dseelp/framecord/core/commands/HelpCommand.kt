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

import de.dseelp.kommon.command.CommandContext
import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.literal
import dev.kord.core.behavior.channel.threads.ThreadChannelBehavior
import dev.kord.core.entity.channel.MessageChannel
import io.github.dseelp.framecord.api.InternalFrameCordApi
import io.github.dseelp.framecord.api.command.*
import io.github.dseelp.framecord.api.guild.info
import io.github.dseelp.framecord.api.utils.Commands

class HelpCommand : Command<Sender> {
    override val scopes: Array<CommandScope> = CommandScope.ALL


    @OptIn(InternalFrameCordApi::class)
    override val node: CommandNode<Sender> = literal("help") {
        execute {
            if (sender is DiscordSender<*>) {
                if (sender is GuildChannelSender<*>) (this as CommandContext<DiscordSender<*>>).sendDcHelp(
                    false
                )
                else (this as CommandContext<DiscordSender<*>>).sendDcHelp(true)
            } else {
                val commands = Commands.getCommandsForScope(CommandScope.CONSOLE)
                sender.sendMessage(buildString {
                    commands.forEach {
                        append("${it.key.name}\n")
                        for (triple in it.value) {
                            val description = triple.description
                            append("    ${triple.name}${if (description.isBlank() || description.isEmpty()) "" else " : $description"}\n")
                        }
                    }
                }.dropLast(1))
            }
        }
    }

    override val description: String = "Shows this help site"


    @OptIn(InternalFrameCordApi::class)
    suspend fun CommandContext<DiscordSender<*>>.sendDcHelp(private: Boolean) {
        val nSender = if (private) PrivateSender(sender.message) else {
            if (sender.message.channel is ThreadChannelBehavior) ThreadSender(sender.message) else GuildSender(sender.message)
        }
        val context = CommandContext(mapOf(), mapOf(), mapOf(), nSender)
        val commands =
            (if (private) Commands.getCommandsForScope(CommandScope.PRIVATE) else {
                val guildId = (sender as GuildSenderBehavior).getGuild().id.value
                val unionList =
                    Commands.getCommandsForScope(CommandScope.GUILD, guildId).mapValues { it.value.toMutableList() }
                        .toMutableMap()
                Commands.getCommandsForScope(CommandScope.THREAD, guildId).forEach { (plugin, list) ->
                    val combined = unionList.getOrDefault(plugin, mutableListOf()).let {
                        it.addAll(list)
                        it.distinctBy { value ->
                            value.node
                        }
                    }
                    unionList[plugin] = combined.toMutableList()
                }
                unionList
            }).map {
                it.key to it.value.filter { h ->
                    @Suppress("UNCHECKED_CAST")
                    (h.node as CommandNode<DiscordSender<out MessageChannel>>).checkAccess.invoke(context)
                }
            }.filterNot { it.second.isEmpty() }
        val prefix =
            if (sender is GuildChannelSender<*>) (sender as GuildChannelSender<*>).getGuild().info.prefix else "!"
        sender.createEmbed {
            title = "FrameCord Help"
            footer = sender.footer()

            commands.forEach {
                field {
                    name = it.first.name
                    value = buildString {
                        for (triple in it.second) {
                            val description = triple.description
                            append("$prefix${triple.name}${if (description.isBlank() || description.isEmpty()) "" else " : $description"}\n")
                        }
                    }.dropLast(1)
                }
            }

        }
    }
}