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

package de.dseelp.kotlincord.api.command.arguments

import de.dseelp.kommon.command.CommandContext
import de.dseelp.kommon.command.arguments.Argument
import de.dseelp.kotlincord.api.Bot
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.command.DiscordSender
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.utils.MentionUtils
import de.dseelp.kotlincord.api.utils.MentionUtils.getUserSnowflake
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import dev.kord.common.entity.ChannelType
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.*
import org.koin.core.component.inject

class MentionArgument<S : DiscordSender<out MessageChannel>, M : Any>(
    name: String,
    val converter: suspend CommandContext<S>.(message: String) -> M?
) :
    Argument<S, M>(name) {
    override fun complete(context: CommandContext<S>, value: String): Array<String> = arrayOf()

    override suspend fun get(context: CommandContext<S>, value: String): M? = converter.invoke(context, value)

    @OptIn(InternalKotlinCordApi::class)
    companion object : CordKoinComponent {
        private val bot: Bot by inject()

        fun <S : DiscordSender<out GuildMessageChannel>> user(name: String) =
            MentionArgument<S, User>(name) { message ->
                val snowflake = getUserSnowflake(message) ?: return@MentionArgument null
                bot.kord.getUser(snowflake)
            }

        fun <S : DiscordSender<out GuildMessageChannel>> member(name: String) =
            MentionArgument<S, Member>(name) { message ->
                val snowflake = getUserSnowflake(message) ?: return@MentionArgument null
                sender.message.getGuild().getMemberOrNull(snowflake)
            }

        private suspend fun <S : DiscordSender<out MessageChannel>> CommandContext<S>.getChannel(message: String): GuildChannel? {
            if (sender !is GuildSender) throw UnsupportedOperationException("This mention argument is only supported by Guilds")
            return MentionUtils.channel(sender.message.getGuild(), message)
        }

        fun <S : DiscordSender<out MessageChannel>> channel(name: String) =
            MentionArgument<S, GuildChannel>(name) { message ->
                getChannel(message)
            }

        fun <S : DiscordSender<out MessageChannel>> textChannel(name: String) =
            MentionArgument<S, TextChannel>(name) { message ->
                val channel = getChannel(message) ?: return@MentionArgument null
                if (channel.type == ChannelType.GuildText) channel as TextChannel
                else null
            }

        fun <S : DiscordSender<out MessageChannel>> messageChannel(name: String) =
            MentionArgument<S, GuildMessageChannel>(name) { message ->
                val channel = getChannel(message) ?: return@MentionArgument null
                if (channel.type == ChannelType.GuildText || channel.type == ChannelType.GuildNews) channel as GuildMessageChannel
                else null
            }

        fun <S : DiscordSender<out MessageChannel>> newsChannel(name: String) =
            MentionArgument<S, NewsChannel>(name) { message ->
                val channel = getChannel(message) ?: return@MentionArgument null
                if (channel.type == ChannelType.GuildNews) channel as NewsChannel
                else null
            }

        fun <S : DiscordSender<out GuildMessageChannel>> role(name: String) = MentionArgument<S, Role>(name) { message ->
            if (sender !is GuildSender) throw UnsupportedOperationException("This mention argument is only supported by Guilds")
            MentionUtils.role((sender as GuildSender).getGuild(), message)
        }
    }
}