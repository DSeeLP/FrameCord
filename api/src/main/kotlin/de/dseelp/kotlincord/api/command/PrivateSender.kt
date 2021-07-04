/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.command

import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.DmChannel


class PrivateSender(override val message: Message) : DiscordSender<DmChannel> {
    override val author: User = message.author!!
    override val isGuild: Boolean = false
    override val isPrivate: Boolean = true
    override suspend fun getChannel(): DmChannel = message.channel.asChannel() as DmChannel
    override val isConsole: Boolean = false
    override val name: String = author.username
}