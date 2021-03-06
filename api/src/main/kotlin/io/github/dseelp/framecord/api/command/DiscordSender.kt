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

package io.github.dseelp.framecord.api.command

import dev.kord.core.behavior.channel.GuildChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed interface DiscordSender<T : MessageChannel> : Sender {
    /**
     * The user who executed the command
     */
    val author: User

    suspend fun getChannel(): T

    /**
     * The message object of the executed command
     */
    val message: Message
    override suspend fun sendMessage(vararg messages: String, parseColors: Boolean) {
        val channel = (if (this is ThreadSender) getThread() else getChannel()).asChannelOrNull() ?: return
        messages.onEach { channel.createMessage(it) }
    }

    /**
     * Generates a footer based on the sender of the command
     * @return The generated footer
     */
    suspend fun footer() = EmbedBuilder.Footer().apply {
        if (message.channel is GuildChannelBehavior) {
            val member = message.getAuthorAsMember()!!
            text = member.tag
            icon = member.avatar.url
        } else {
            val author = message.author!!
            text = author.tag
            icon = author.avatar.url
        }
    }
}


/**
 * Sends a message to the channel/thread were the command was executed
 * @return The message that was sent
 */
@OptIn(ExperimentalContracts::class)
suspend inline fun DiscordSender<out MessageChannel>.createMessage(message: MessageCreateBuilder.() -> Unit): Message {
    contract {
        callsInPlace(message, InvocationKind.EXACTLY_ONCE)
    }
    return (if (this is ThreadSender) getThread() else getChannel()).createMessage(message)
}

/**
 * Sends an embed to the channel/thread were the command was executed
 * @return The message that was sent
 */
@OptIn(ExperimentalContracts::class)
suspend inline fun DiscordSender<out MessageChannel>.createEmbed(message: EmbedBuilder.() -> Unit): Message {
    contract {
        callsInPlace(message, InvocationKind.EXACTLY_ONCE)
    }
    return (if (this is ThreadSender) getThread() else getChannel()).createEmbed(message)
}