/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.command

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed

sealed interface Sender {
    val isConsole: Boolean
    val name: String

    fun sendMessage(vararg messages: String, parseColors: Boolean = true)

    fun sendMessage(embed: MessageEmbed) = sendMessage(net.dv8tion.jda.api.MessageBuilder(embed).build())
    fun sendMessage(embed: EmbedBuilder) = sendMessage(embed.build())
    fun sendMessage(message: Message)
}