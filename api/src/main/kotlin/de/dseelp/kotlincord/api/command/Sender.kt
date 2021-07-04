/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.command

import dev.kord.rest.builder.message.MessageCreateBuilder


sealed interface Sender {
    val isConsole: Boolean
    val name: String

    suspend fun sendMessage(vararg messages: String, parseColors: Boolean = true)

    suspend fun sendMessage(message: MessageCreateBuilder.() -> Unit)
}