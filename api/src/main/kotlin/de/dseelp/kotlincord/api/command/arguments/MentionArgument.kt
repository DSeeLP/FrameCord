/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.command.arguments

import de.dseelp.kommon.command.CommandContext
import de.dseelp.kommon.command.arguments.Argument
import de.dseelp.kotlincord.api.Bot
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.asSnowflake
import de.dseelp.kotlincord.api.command.DiscordSender
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
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

        private fun getSnowflake(message: String): Snowflake? {
            if (!(message.startsWith("<@!") && message.endsWith('>'))) return null
            return Snowflake(message.replaceFirst("<@!", "").replaceFirst(">", ""))
        }

        fun <S : DiscordSender<out GuildMessageChannel>> user(name: String) =
            MentionArgument<S, User>(name) { message ->
                val snowflake = getSnowflake(message) ?: return@MentionArgument null
                bot.kord.getUser(snowflake)
            }

        fun <S : DiscordSender<out GuildMessageChannel>> member(name: String) =
            MentionArgument<S, Member>(name) { message ->
                val snowflake = getSnowflake(message) ?: return@MentionArgument null
                sender.message.getGuild().getMemberOrNull(snowflake)
            }

        private suspend fun <S : DiscordSender<out MessageChannel>> CommandContext<S>.getChannel(message: String): GuildChannel? {
            if (sender !is GuildSender) throw UnsupportedOperationException("This mention argument is only supported by Guilds")
            return if (message.startsWith("<#") && message.endsWith('>')) {
                sender.message.getGuild()
                    .getChannel(message.replaceFirst("<#", "").replaceFirst(">", "").asSnowflake)
            }else null
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
            if (!(message.startsWith("<@&") && message.endsWith('>'))) return@MentionArgument null
            (sender as GuildSender).getGuild().getRole(Snowflake(message.replaceFirst("<@&", "").replaceFirst(">", "")))
        }
    }
}