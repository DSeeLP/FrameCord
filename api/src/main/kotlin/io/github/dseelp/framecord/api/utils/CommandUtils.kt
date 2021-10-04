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

package io.github.dseelp.framecord.api.utils

import com.log4k.d
import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.ParsedResult
import dev.kord.common.entity.Permission
import dev.kord.common.kColor
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.rest.json.JsonErrorCode
import dev.kord.rest.request.RestRequestException
import io.github.dseelp.framecord.api.InternalFrameCordApi
import io.github.dseelp.framecord.api.checkPermissions
import io.github.dseelp.framecord.api.command.*
import io.github.dseelp.framecord.api.interactions.ButtonContext
import io.github.dseelp.framecord.api.modules.FeatureRestricted
import io.github.dseelp.framecord.api.modules.checkBoolean
import io.github.dseelp.framecord.api.plugins.Plugin
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import org.koin.core.component.inject
import java.awt.Color

object CommandUtils {
    @OptIn(InternalFrameCordApi::class)
    suspend fun <T : Any> CommandDispatcher<T>.execute(
        sender: T,
        message: String,
        actions: Actions<T>,
        bypassAccess: Boolean = false,
        cache: CommandCache<T> = CommandCache.noOperation()
    ) {
        val hashCode = sender.hashCode()
        val cached = cache[hashCode, message]
        val parsed = cached ?: parse(sender, message)
        if (parsed == null || parsed.failed || parsed.node == null) {
            actions.error(message, parsed, null)
            return
        }
        if (cached == null) cache[hashCode, message] = parsed
        if (sender is GuildSenderBehavior) {
            val guildId = sender.getGuild().id.value
            val holder = Commands.getCommandHolder(
                if (sender is ThreadSender) CommandScope.THREAD else CommandScope.GUILD,
                parsed.root.name!!
            )
            val checked = holder?.featureRestricted?.checkBoolean(guildId)
            if (checked == false) return
        }

        val throwable = parsed.execute(bypassAccess)
        if (throwable == null)
            actions.success(parsed)
        else actions.error(message, parsed, throwable)
    }

    interface Actions<T : Any> {
        suspend fun error(message: String, result: ParsedResult<T>?, throwable: Throwable?)
        suspend fun success(result: ParsedResult<T>)

        suspend fun handleError(message: String, result: ParsedResult<T>?, throwable: Throwable?) {
            if (throwable == null) return
            val sender = result?.context?.sender
            if (throwable is RestRequestException && throwable.error?.code == JsonErrorCode.PermissionLack) {
                if (sender is GuildSender) {
                    val selfMember = sender.getGuild().getMember(bot.kord.selfId)
                    d("", throwable)
                    if (selfMember.checkPermissions(sender.getChannel(), Permission.SendMessages)) {
                        sender.createEmbed {
                            title = "Not enough permissions!"
                            color = Color.RED.kColor
                            description = "The bot is missing permissions please grant him Administrator permissions!"
                        }
                    } else {
                        val guild = sender.getGuild()
                        val owner = guild.owner.asMember()
                        owner.getDmChannelOrNull()?.createEmbed {
                            title = "Not enough permissions!"
                            color = Color.RED.kColor
                            description =
                                "The bot is missing permissions on the guild ${guild.name} please grant him Administrator permissions!"
                        }
                    }
                } else if (sender is ButtonContext) {

                }
            } else {
                throwable.printStackTrace()
            }
        }

        @OptIn(InternalFrameCordApi::class)
        companion object : CordKoinComponent {
            private val bot: io.github.dseelp.framecord.api.Bot by inject()
            fun <T : Any> noOperation() = object : Actions<T> {

                override suspend fun success(result: ParsedResult<T>) = Unit
                override suspend fun error(message: String, result: ParsedResult<T>?, throwable: Throwable?) {
                    handleError(message, result, throwable)
                }
            }
        }
    }


    interface CommandCache<T : Any> {
        operator fun get(senderHashcode: Int, message: String): ParsedResult<T>?
        operator fun set(senderHashcode: Int, message: String, result: ParsedResult<T>)

        companion object {
            fun <T : Any> noOperation() = object : CommandCache<T> {
                override fun get(senderHashcode: Int, message: String): ParsedResult<T>? = null

                override fun set(senderHashcode: Int, message: String, result: ParsedResult<T>) = Unit

            }
        }
    }
}

@InternalFrameCordApi
data class CommandHolder(
    val plugin: Plugin,
    val scopes: Array<CommandScope>,
    val description: String,
    val name: String,
    val node: CommandNode<out Sender>,
    val featureRestricted: FeatureRestricted? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommandHolder) return false

        if (!scopes.contentEquals(other.scopes)) return false
        if (description != other.description) return false
        if (name != other.name) return false
        if (node != other.node) return false
        if (featureRestricted != other.featureRestricted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scopes.contentHashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + node.hashCode()
        result = 31 * result + (featureRestricted?.hashCode() ?: 0)
        return result
    }
}