/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.command

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.rest.builder.message.MessageCreateBuilder

sealed interface DiscordSender<T : MessageChannel> : Sender {
    val author: User
    val isGuild: Boolean
    val isPrivate: Boolean
    val channel: T
    val message: Message

    override fun sendMessage(message: MessageCreateBuilder.() -> Unit) {

    }

    override suspend fun sendMessage(vararg messages: String, parseColors: Boolean) {
        messages.onEach { channel.createMessage(it) }
    }
}