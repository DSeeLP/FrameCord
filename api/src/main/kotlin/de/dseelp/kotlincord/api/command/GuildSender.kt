/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.command

import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.message.MessageCreateEvent

class GuildSender(override val message: Message) : DiscordSender<GuildMessageChannel> {
    override val author: User = message.author!!
    override val isGuild: Boolean = true
    override val isPrivate: Boolean = false
    override suspend fun getChannel(): GuildMessageChannel = message.channel.asChannel() as GuildMessageChannel
    override val isConsole: Boolean = false
    override val name: String = author.username
    suspend fun getGuild() = message.getGuild()
    suspend fun getMember() = message.getAuthorAsMember()!!
}