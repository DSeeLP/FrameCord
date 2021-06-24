/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.utils

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.ParsedResult
import de.dseelp.kotlincord.api.buttons.ButtonContext
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.logging.logger
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import java.awt.Color

object CommandUtils {
    fun <T : Any> CommandDispatcher<T>.execute(
        sender: T,
        message: String,
        actions: Actions<T>,
        bypassAccess: Boolean = false,
        cache: CommandCache<T> = CommandCache.noOperation()
    ) {
        val hashCode = sender.hashCode()
        val cached = cache[hashCode, message]
        val parsed = cached ?: parse(sender, message)
        if (parsed == null || parsed.failed) {
            actions.error(message, parsed, null)
            return
        }
        if (cached == null) cache[hashCode, message] = parsed

        val throwable = parsed.execute(bypassAccess)
        if (throwable == null)
            actions.success(parsed)
        else actions.error(message, parsed, throwable)
    }

    interface Actions<T : Any> {
        fun error(message: String, result: ParsedResult<T>?, throwable: Throwable?)
        fun success(result: ParsedResult<T>)

        companion object {
            val logger by logger("CommandUtils")
            fun <T : Any> noOperation() = object : Actions<T> {
                override fun error(message: String, result: ParsedResult<T>?, throwable: Throwable?) {
                    val sender = result?.context?.sender
                    if (throwable is InsufficientPermissionException) {
                        if (sender is GuildSender) {
                            val selfMember = sender.guild.retrieveMember(sender.jda.selfUser).complete()
                            val noPermissionEmbed = embedBuilder {
                                title = "Not enough permissions!"
                                color = Color.RED
                            }
                            logger.debug("", throwable)
                            if (selfMember.hasPermission(sender.channel, Permission.MESSAGE_WRITE)) {
                                noPermissionEmbed.description =
                                    "The bot is missing permissions please grant him Administrator permissions!"
                                sender.channel.sendMessage(noPermissionEmbed.build()).queue()
                            } else {
                                val owner = sender.guild.retrieveOwner().complete()
                                val user = owner.user
                                user.openPrivateChannel().queue { channel ->
                                    noPermissionEmbed.description =
                                        "The bot is missing permissions on the guild ${sender.guild.name} please grant him Administrator permissions!"
                                    channel.sendMessage(noPermissionEmbed.build()).queue()
                                }
                            }
                        } else if (sender is ButtonContext) {

                        }
                    } else {
                        throwable?.printStackTrace()
                    }
                }

                override fun success(result: ParsedResult<T>) = Unit
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