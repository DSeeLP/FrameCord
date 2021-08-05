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

package de.dseelp.kotlincord.api.setup

import de.dseelp.kotlincord.api.utils.MentionUtils
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.create.MessageCreateBuilder

class UserStep(messageBuilder: suspend MessageCreateBuilder.(GuildMessageChannel) -> Unit) :
    MessageStep(messageBuilder) {
    override suspend fun handleMessage(message: Message): Boolean {
        val user = MentionUtils.user(message.content) ?: return false
        setup.completeStep(user)
        return true
    }
}

class MemberStep(messageBuilder: suspend MessageCreateBuilder.(GuildMessageChannel) -> Unit) :
    MessageStep(messageBuilder) {
    override suspend fun handleMessage(message: Message): Boolean {
        val user = MentionUtils.member(channel.getGuild(), message.content) ?: return false
        setup.completeStep(user)
        return true
    }
}

class RoleStep(messageBuilder: suspend MessageCreateBuilder.(GuildMessageChannel) -> Unit) :
    MessageStep(messageBuilder) {
    override suspend fun handleMessage(message: Message): Boolean {
        val role = MentionUtils.role(channel.getGuild(), message.content) ?: return false
        setup.completeStep(role)
        return true
    }
}

class ChannelStep(messageBuilder: suspend MessageCreateBuilder.(GuildMessageChannel) -> Unit) :
    MessageStep(messageBuilder) {
    override suspend fun handleMessage(message: Message): Boolean {
        val channel = MentionUtils.channel(channel.getGuild(), message.content) ?: return false
        setup.completeStep(channel)
        return true
    }
}

class VoiceChannelStep(messageBuilder: suspend MessageCreateBuilder.(GuildMessageChannel) -> Unit) :
    MessageStep(messageBuilder) {
    override suspend fun handleMessage(message: Message): Boolean {
        val channel = MentionUtils.voiceChannel(channel.getGuild(), message.content) ?: return false
        setup.completeStep(channel)
        return true
    }
}

class MessageChannelStep(messageBuilder: suspend MessageCreateBuilder.(GuildMessageChannel) -> Unit) :
    MessageStep(messageBuilder) {
    override suspend fun handleMessage(message: Message): Boolean {
        val channel = MentionUtils.messageChannel(channel.getGuild(), message.content) ?: return false
        setup.completeStep(channel)
        return true
    }
}

class TextChannelStep(messageBuilder: suspend MessageCreateBuilder.(GuildMessageChannel) -> Unit) :
    MessageStep(messageBuilder) {
    override suspend fun handleMessage(message: Message): Boolean {
        val channel = MentionUtils.textChannel(channel.getGuild(), message.content) ?: return false
        setup.completeStep(channel)
        return true
    }
}

class NewsChannelStep(messageBuilder: suspend MessageCreateBuilder.(GuildMessageChannel) -> Unit) :
    MessageStep(messageBuilder) {
    override suspend fun handleMessage(message: Message): Boolean {
        val channel = MentionUtils.newsChannel(channel.getGuild(), message.content) ?: return false
        setup.completeStep(channel)
        return true
    }
}
