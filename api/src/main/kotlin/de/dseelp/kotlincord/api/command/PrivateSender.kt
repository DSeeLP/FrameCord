/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.command

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.entities.User

class PrivateSender(override val jda: JDA, override val message: Message) : DiscordSender<PrivateChannel> {
    override val author: User = message.author
    override val isGuild: Boolean = true
    override val isPrivate: Boolean = false
    override val channel: PrivateChannel = message.privateChannel
    override val isConsole: Boolean = false
    override val name: String = author.name
}