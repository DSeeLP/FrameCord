/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.command

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User

sealed interface DiscordSender<T : MessageChannel> : Sender {
    val jda: JDA
    val author: User
    val isGuild: Boolean
    val isPrivate: Boolean
    val channel: T
    val message: Message

    override fun sendMessage(message: Message) = channel.sendMessage(message).queue()

    override fun sendMessage(vararg messages: String, parseColors: Boolean) {
        messages.onEach { channel.sendMessage(it).queue() }
    }
}