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

package de.dseelp.kotlincord.plugins.privatechannels

import de.dseelp.kotlincord.api.asSnowflake
import de.dseelp.kotlincord.api.event.EventHandle
import de.dseelp.kotlincord.api.event.Listener
import de.dseelp.kotlincord.plugins.privatechannels.ChannelListener.handleJoin
import de.dseelp.kotlincord.plugins.privatechannels.PrivateChannelPlugin.database
import de.dseelp.kotlincord.plugins.privatechannels.PrivateChannelPlugin.suspendingDatabase
import de.dseelp.kotlincord.plugins.privatechannels.db.ActivePrivateChannel
import de.dseelp.kotlincord.plugins.privatechannels.db.ActivePrivateChannels
import de.dseelp.kotlincord.plugins.privatechannels.db.PrivateChannel
import de.dseelp.kotlincord.plugins.privatechannels.db.PrivateChannels
import dev.kord.common.entity.ChannelType
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import org.jetbrains.exposed.sql.deleteWhere

@Listener
object GuildListener {
    @EventHandle
    fun onGuildLeave(event: GuildDeleteEvent) {
        val guildId = event.guildId.value
        database {
            transaction {
                PrivateChannels.deleteWhere { PrivateChannels.guildId eq guildId }
            }
        }
    }

    @EventHandle
    suspend fun onGuildCreate(event: GuildCreateEvent) {
        val guildId = event.guild.id.value
        val guild = event.guild.asGuild()
        suspendingDatabase {
            suspendedTransaction {
                val pnChannels = PrivateChannel.find { PrivateChannels.guildId eq guildId }
                val active =
                    ActivePrivateChannel.find { ActivePrivateChannels.privateChannel inList pnChannels.map { it.id } }
                val shouldDeleted = pnChannels.filter {
                    val channel = guild.getChannelOrNull(it.joinChannelId.asSnowflake)
                    channel == null
                }
                val notDeleted = pnChannels.filterNot { shouldDeleted.contains(it) }
                for (channel in active) {
                    val ch = guild.getChannelOrNull(channel.channelId.asSnowflake)
                    if (ch == null) {
                        channel.delete()
                        continue
                    }
                    if (ch.type != ChannelType.GuildVoice) continue
                    ch as VoiceChannel
                    if (ch.voiceStates.count() == 0 || shouldDeleted.contains(channel.privateChannel)) {
                        ch.delete()
                        channel.delete()
                    }
                }
                for (pnChannel in shouldDeleted) {
                    pnChannel.delete()
                }
                for (privateChannel in notDeleted) {
                    val channel = guild.getChannel(privateChannel.joinChannelId.asSnowflake)
                    channel as VoiceChannel
                    channel.voiceStates.collect {
                        handleJoin(it)
                    }
                }
            }
        }
    }
}