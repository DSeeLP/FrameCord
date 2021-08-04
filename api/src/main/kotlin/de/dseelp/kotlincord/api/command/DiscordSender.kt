/*
 * Copyright (c) 2021 DSeeLP & KotlinCord contributors
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

package de.dseelp.kotlincord.api.command

import dev.kord.core.behavior.channel.GuildChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageCreateBuilder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed interface DiscordSender<T : MessageChannel> : Sender {
    val author: User
    val isGuild: Boolean
    val isPrivate: Boolean
    suspend fun getChannel(): T
    val message: Message
    override val isConsole: Boolean
        get() = false

    override suspend fun sendMessage(message: MessageCreateBuilder.() -> Unit) {
        createMessage(message)
    }

    override suspend fun sendMessage(vararg messages: String, parseColors: Boolean) {
        val channel = getChannel().asChannelOrNull() ?: return
        messages.onEach { channel.createMessage(it) }
    }

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

@OptIn(ExperimentalContracts::class)
suspend inline fun DiscordSender<out MessageChannel>.createMessage(message: MessageCreateBuilder.() -> Unit): Message {
    contract {
        callsInPlace(message, InvocationKind.EXACTLY_ONCE)
    }
    return getChannel().createMessage(message)
}

@OptIn(ExperimentalContracts::class)
suspend inline fun DiscordSender<out MessageChannel>.createMessageSafe(message: MessageCreateBuilder.() -> Unit): Message? {
    contract {
        callsInPlace(message, InvocationKind.EXACTLY_ONCE)
    }
    return getChannel().asChannelOrNull()?.createMessage(message)
}

@OptIn(ExperimentalContracts::class)
suspend inline fun DiscordSender<out MessageChannel>.createEmbed(message: EmbedBuilder.() -> Unit): Message {
    contract {
        callsInPlace(message, InvocationKind.EXACTLY_ONCE)
    }
    return getChannel().createEmbed(message)
}

@OptIn(ExperimentalContracts::class)
suspend inline fun DiscordSender<out MessageChannel>.createEmbedSafe(message: EmbedBuilder.() -> Unit): Message? {
    contract {
        callsInPlace(message, InvocationKind.EXACTLY_ONCE)
    }
    return getChannel().asChannelOrNull()?.createEmbed(message)
}