/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.utils

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.ParsedResult

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
            fun <T : Any> noOperation() = object : Actions<T> {
                override fun error(message: String, result: ParsedResult<T>?, throwable: Throwable?) = Unit

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