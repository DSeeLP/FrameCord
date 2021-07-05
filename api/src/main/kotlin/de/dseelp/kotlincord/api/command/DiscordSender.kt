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
    suspend fun getChannel(): T
    val message: Message

    override suspend fun sendMessage(message: MessageCreateBuilder.() -> Unit) {
        getChannel().asChannelOrNull()?.createMessage(message)
    }

    override suspend fun sendMessage(vararg messages: String, parseColors: Boolean) {
        messages.onEach { getChannel().createMessage(it) }
    }
}