/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.command

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User

class GuildSender(override val jda: JDA, override val message: Message) : DiscordSender<TextChannel> {
    override val author: User = message.author
    override val isGuild: Boolean = true
    override val isPrivate: Boolean = false
    override val channel: TextChannel = message.textChannel
    override val isConsole: Boolean = false
    override val name: String = author.name
    val guild = message.guild
}