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
import de.dseelp.kotlincord.api.utils.asOverwrite
import de.dseelp.kotlincord.plugins.privatechannels.PrivateChannelPlugin.database
import de.dseelp.kotlincord.plugins.privatechannels.PrivateChannelPlugin.suspendingDatabase
import de.dseelp.kotlincord.plugins.privatechannels.db.ActivePrivateChannel
import de.dseelp.kotlincord.plugins.privatechannels.db.ActivePrivateChannels
import de.dseelp.kotlincord.plugins.privatechannels.db.PrivateChannel
import de.dseelp.kotlincord.plugins.privatechannels.db.PrivateChannels
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.createVoiceChannel
import dev.kord.core.behavior.createVoiceChannel
import dev.kord.core.behavior.edit
import dev.kord.core.entity.VoiceState
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.channel.VoiceChannelDeleteEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.rest.builder.channel.VoiceChannelCreateBuilder
import kotlinx.coroutines.flow.count
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or

@Listener
object ChannelListener {

    @EventHandle
    suspend fun onChannelDelete(event: VoiceChannelDeleteEvent) = suspendingDatabase {
        suspendedTransaction {
            val channelId = event.channel.id.value
            val pnChannels = PrivateChannel.find { PrivateChannels.joinChannelId eq channelId }
            val guild = event.channel.getGuild()
            ActivePrivateChannel.find { ActivePrivateChannels.channelId eq channelId or (ActivePrivateChannels.privateChannel inList pnChannels.map { it.id }) }
                .forEach {
                    it.delete()
                    if (it.channelId == channelId) return@forEach
                    val channel = guild.getChannelOrNull(it.channelId.asSnowflake) ?: return@forEach
                    channel.delete()
                }
            pnChannels.forEach {
                it.delete()
            }
        }
    }

    @EventHandle
    suspend fun onVoiceUpdate(event: VoiceStateUpdateEvent) {
        suspendingDatabase {
            val oldId = event.old?.channelId
            val newId = event.state.channelId
            suspend fun checkDeleted(state: VoiceState): Boolean {
                val channelId = state.channelId ?: return false
                val channel = getChannel(state.guildId.value, channelId.value) ?: return false
                val guildChannel = state.getGuild().getChannelOrNull(channel.channelId.asSnowflake) ?: return false
                if (guildChannel.type == ChannelType.GuildVoice) if (((guildChannel as VoiceChannel).voiceStates.count() == 0)) {
                    guildChannel.delete()
                    transaction {
                        channel.delete()
                    }
                    return true
                }
                return false
            }
            if (newId == null) {
                if (oldId == null) return@suspendingDatabase
                val old = event.old!!
                checkDeleted(old)
            } else if (oldId != newId) {
                val old = event.old
                val current = event.state
                if (old != null) checkDeleted(old)
                handleJoin(current)
            }
        }
    }

    fun getJoinChannel(guildId: Long, channelId: Long) = database {
        return@database transaction {
            PrivateChannel.find { (PrivateChannels.joinChannelId eq channelId) and (PrivateChannels.guildId eq guildId) }
                .firstOrNull()
        }
    }

    fun getChannel(guildId: Long, channelId: Long) = database {
        return@database transaction {
            val possibleChannels = ActivePrivateChannel.find { (ActivePrivateChannels.channelId eq channelId) }
            val matchingGuildChannels = possibleChannels.filter { it.privateChannel.guildId == guildId }
            if (matchingGuildChannels.size > 1) throw IllegalStateException("This should simply not happen")
            return@transaction matchingGuildChannels.firstOrNull()
        }
    }

    suspend fun handleJoin(state: VoiceState) = suspendingDatabase {
        val channelId = state.channelId ?: return@suspendingDatabase
        val joinChannel = getJoinChannel(state.guildId.value, channelId.value) ?: return@suspendingDatabase
        val guild = state.getGuild()
        val member = state.getMember()
        val channel = state.getChannelOrNull()!!.asChannel() as VoiceChannel
        val createBuilder: VoiceChannelCreateBuilder.() -> Unit = {
            userLimit = joinChannel.userLimit
            permissionOverwrites.addAll(channel.permissionOverwrites.map { it.asOverwrite })
            permissionOverwrites.add(
                Overwrite(
                    member.id,
                    OverwriteType.Member,
                    Permissions(Permission.Connect, Permission.MoveMembers),
                    Permissions()
                )
            )
        }
        val name = joinChannel.nameTemplate.replace("%user%", member.displayName).replace(
            "%game%",
            member.getPresenceOrNull()?.activities?.firstOrNull()?.name ?: transaction { joinChannel.defaultGame })
        val created =
            channel.category?.createVoiceChannel(name, createBuilder) ?: guild.createVoiceChannel(
                name,
                createBuilder
            )
        member.edit {
            this.voiceChannelId = created.id
        }
        transaction {
            ActivePrivateChannel.new {
                this.channelId = created.id.value
                this.ownerId = member.id.value
                this.privateChannel = joinChannel
            }
        }
    }
}