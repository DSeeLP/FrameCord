/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.command.arguments

import de.dseelp.kommon.command.CommandContext
import de.dseelp.kommon.command.arguments.Argument
import de.dseelp.kotlincord.api.command.DiscordSender
import de.dseelp.kotlincord.api.command.GuildSender
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.*

class MentionArgument<S : DiscordSender<out MessageChannel>, M : Any>(
    name: String,
    val converter: CommandContext<S>.(message: Message) -> M?
) :
    Argument<S, M>(name) {
    override fun complete(context: CommandContext<S>, value: String): Array<String> = arrayOf()

    override fun get(context: CommandContext<S>, value: String): M? =
        converter.invoke(context, MessageBuilder(value).build())

    companion object {
        fun <S : DiscordSender<out MessageChannel>> user(name: String) = MentionArgument<S, User>(name) { message ->
            val content = message.contentRaw
            val matcher = Message.MentionType.USER.pattern.matcher(content)
            if (!matcher.matches()) return@MentionArgument null
            sender.jda.retrieveUserById(content.replaceFirst("<@!", "").replaceFirst(">", "")).complete()
        }

        fun <S : DiscordSender<out MessageChannel>> messageChannel(name: String) =
            MentionArgument<S, TextChannel>(name) { message ->
                val content = message.contentRaw
                val matcher = Message.MentionType.CHANNEL.pattern.matcher(content)
                if (!matcher.matches()) return@MentionArgument null
                if (!sender.isGuild) throw UnsupportedOperationException("This mention argument is only supported by Guilds")
                (sender as GuildSender).guild.getTextChannelById(content.replaceFirst("<#", "").replaceFirst(">", ""))
            }

        fun <S : DiscordSender<out MessageChannel>> role(name: String) = MentionArgument<S, Role>(name) { message ->
            val content = message.contentRaw
            val matcher = Message.MentionType.CHANNEL.pattern.matcher(content)
            if (!matcher.matches()) return@MentionArgument null
            if (!sender.isGuild) throw UnsupportedOperationException("This mention argument is only supported by Guilds")
            (sender as GuildSender).guild.getRoleById(content.replaceFirst("<@&", "").replaceFirst(">", ""))
        }
    }
}