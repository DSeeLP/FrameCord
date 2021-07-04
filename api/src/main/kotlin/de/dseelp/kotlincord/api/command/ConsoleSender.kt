/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.command

import de.dseelp.kotlincord.api.console.ConsoleColor
import de.dseelp.kotlincord.api.logging.logger
import dev.kord.rest.builder.message.MessageCreateBuilder

object ConsoleSender : Sender {
    private val log by logger("")
    override val isConsole: Boolean = true
    override val name: String = "Console"
    override suspend fun sendMessage(vararg messages: String, parseColors: Boolean) {
        messages.onEach { log.command(if (parseColors) ConsoleColor.toColouredString('&', it) else it) }
    }

    override suspend fun sendMessage(message: MessageCreateBuilder.() -> Unit) {
        throw UnsupportedOperationException("A ConsoleSender can't send discord messages")
    }

}