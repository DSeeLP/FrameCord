/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.utils

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.ParsedResult
import de.dseelp.kotlincord.api.Bot
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.buttons.ButtonContext
import de.dseelp.kotlincord.api.checkPermissions
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import dev.kord.common.entity.Permission
import dev.kord.common.kColor
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.json.JsonErrorCode
import dev.kord.rest.request.RestRequestException
import org.koin.core.component.inject
import java.awt.Color

object CommandUtils {
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
        suspend fun error(message: String, result: ParsedResult<T>?, throwable: Throwable?)
        suspend fun success(result: ParsedResult<T>)

        suspend fun handleError(message: String, result: ParsedResult<T>?, throwable: Throwable?) {
            if (throwable == null) return
            val sender = result?.context?.sender
            if (throwable is RestRequestException && throwable.error?.code == JsonErrorCode.PermissionLack) {
                if (sender is GuildSender) {
                    val selfMember = sender.getGuild().getMember(bot.kord.selfId)
                    logger.debug("", throwable)
                    if (selfMember.checkPermissions(sender.getChannel(), Permission.SendMessages)) {
                        sender.sendMessage {
                            embed {
                                title = "Not enough permissions!"
                                color = Color.RED.kColor
                                description = "The bot is missing permissions please grant him Administrator permissions!"
                            }
                        }
                    } else {
                        val guild = sender.getGuild()
                        val owner = guild.owner.asMember()
                        owner.getDmChannelOrNull()?.createMessage {
                            embed {
                                title = "Not enough permissions!"
                                color = Color.RED.kColor
                                description =
                                    "The bot is missing permissions on the guild ${guild.name} please grant him Administrator permissions!"
                            }
                        }
                    }
                } else if (sender is ButtonContext) {

                }
            } else {
                throwable.printStackTrace()
            }
        }

        @OptIn(InternalKotlinCordApi::class)
        companion object: CordKoinComponent {
            val logger by logger("CommandUtils")
            private val bot: Bot by inject()
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