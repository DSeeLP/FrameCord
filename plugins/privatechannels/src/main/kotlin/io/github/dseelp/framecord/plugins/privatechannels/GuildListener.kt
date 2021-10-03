/*
 * Copyright (c) 2021 DSeeLP & FrameCord contributors
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

package io.github.dseelp.framecord.plugins.privatechannels

import dev.kord.common.entity.ChannelType
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import io.github.dseelp.framecord.api.asSnowflake
import io.github.dseelp.framecord.api.event.EventHandle
import io.github.dseelp.framecord.api.event.Listener
import io.github.dseelp.framecord.api.modules.checkModule
import io.github.dseelp.framecord.plugins.privatechannels.ChannelListener.handleJoin
import io.github.dseelp.framecord.plugins.privatechannels.PrivateChannels.database
import io.github.dseelp.framecord.plugins.privatechannels.PrivateChannels.mId
import io.github.dseelp.framecord.plugins.privatechannels.PrivateChannels.suspendingDatabase
import io.github.dseelp.framecord.plugins.privatechannels.db.ActivePrivateChannelEntity
import io.github.dseelp.framecord.plugins.privatechannels.db.ActivePrivateChannelsTable
import io.github.dseelp.framecord.plugins.privatechannels.db.PrivateChannelEntity
import io.github.dseelp.framecord.plugins.privatechannels.db.PrivateChannelsTable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import org.jetbrains.exposed.sql.deleteWhere

@Listener
object GuildListener {
    @EventHandle
    fun onGuildLeave(event: GuildDeleteEvent) {
        checkModule(event.guildId.value, mId) ?: return
        if (event.unavailable) return
        val guildId = event.guildId.value
        database {
            transaction {
                PrivateChannelsTable.deleteWhere { PrivateChannelsTable.guildId eq guildId }
            }
        }
    }

    @EventHandle
    suspend fun onGuildCreate(event: GuildCreateEvent) {
        checkModule(event.guild.id.value, mId) ?: return
        val guildId = event.guild.id.value
        val guild = event.guild.asGuild()
        suspendingDatabase {
            suspendedTransaction {
                val pnChannels = PrivateChannelEntity.find { PrivateChannelsTable.guildId eq guildId }
                val active =
                    ActivePrivateChannelEntity.find { ActivePrivateChannelsTable.privateChannel inList pnChannels.map { it.id } }
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