/*
 * Copyright (c) 2021 KotlinCord team & contributors
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

package de.dseelp.kotlincord.core.commands.guild

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.arguments.StringArgument
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.guild.info
import de.dseelp.kotlincord.api.utils.*
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createMessage
import kotlinx.coroutines.delay

class PrefixCommand : Command<GuildSender> {
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.GUILD)
    override val node: CommandNode<GuildSender> = literal("prefix") {
        execute {
            val guildInfo = sender.getGuild().info
            val message = sender.getChannel().createMessage {
                embed {
                    title = "Prefix"
                    description = "The guild prefix is ${guildInfo.prefix}"
                }
            }
            sender.message.asMessageOrNull()?.delete()
            delay(10000)
            message.deleteIgnoringNotFound()
        }

        argument(StringArgument("prefix")) {
            execute {
                val prefix: String = get("prefix")
                if (prefix.length > 8) {
                    sendError(sender, "Error!", "The prefix can't be longer than 8 characters")
                    return@execute
                }
                val count = prefix.count { it == ' ' }
                if (count > 1) {
                    sendError(sender, "Error!", "The prefix can contain only one space!")
                    return@execute
                }
                val guild = sender.getGuild()
                guild.info = guild.info.copy(prefix = prefix)
                sender.sendMessage {
                    embed {
                        color = Color.green
                        title = "Prefix set to $prefix"
                    }
                }
            }
        }
    }

    private suspend fun sendError(sender: GuildSender, title: String, description: String) {
        val message = sender.getChannel().createMessage {
            embed {
                color = Color.red
                this.title = title
                this.description = description
            }
        }
        sender.message.delete()
        delay(5000)
        message.deleteIgnoringNotFound()
    }
}