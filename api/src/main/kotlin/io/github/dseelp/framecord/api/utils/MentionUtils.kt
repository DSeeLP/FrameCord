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

package io.github.dseelp.framecord.api.utils

import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.*
import io.github.dseelp.framecord.api.asSnowflake
import io.github.dseelp.framecord.api.bot

object MentionUtils {
    fun getUserSnowflake(message: String): Snowflake? {
        val msg = when {
            message.startsWith("<@!") && message.endsWith('>') -> message.replaceFirst("<@!", "")
            message.startsWith("<@") && message.endsWith('>') -> message.replaceFirst("<@", "")
            else -> null
        }?.replaceFirst(">", "") ?: return null
        return Snowflake(msg)
    }

    suspend fun user(message: String): User? {
        val snowflake = getUserSnowflake(message) ?: return null
        return bot.kord.getUser(snowflake)
    }

    suspend fun member(guild: Guild, message: String): Member? {
        val snowflake = getUserSnowflake(message) ?: return null
        return guild.getMemberOrNull(snowflake)
    }

    private suspend fun getChannel(guild: Guild, message: String): GuildChannel? {
        return if (message.startsWith("<#") && message.endsWith('>')) {
            guild.getChannel(message.replaceFirst("<#", "").replaceFirst(">", "").asSnowflake)
        } else null
    }

    suspend fun channel(guild: Guild, message: String): GuildChannel? = getChannel(guild, message)

    suspend fun textChannel(guild: Guild, message: String): GuildChannel? {
        val channel = getChannel(guild, message) ?: return null
        return if (channel.type == ChannelType.GuildText) channel as TextChannel
        else null
    }

    suspend fun voiceChannel(guild: Guild, message: String): GuildChannel? {
        val channel = getChannel(guild, message) ?: return null
        return if (channel.type == ChannelType.GuildVoice) channel as VoiceChannel
        else null
    }

    suspend fun newsChannel(guild: Guild, message: String): GuildChannel? {
        val channel = getChannel(guild, message) ?: return null
        return if (channel.type == ChannelType.GuildNews) channel as NewsChannel
        else null
    }

    suspend fun messageChannel(guild: Guild, message: String): GuildChannel? {
        val channel = getChannel(guild, message) ?: return null
        return if (channel.type == ChannelType.GuildText || channel.type == ChannelType.GuildNews) channel as GuildMessageChannel
        else null
    }

    suspend fun role(guild: Guild, message: String): Role? {
        if (!(message.startsWith("<@&") && message.endsWith('>'))) return null
        return guild.getRole(Snowflake(message.replaceFirst("<@&", "").replaceFirst(">", "")))
    }
}