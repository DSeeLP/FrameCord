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

import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.createVoiceChannel
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.createVoiceChannel
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.VoiceState
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.channel.VoiceChannelDeleteEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.channel.VoiceChannelCreateBuilder
import io.github.dseelp.framecord.api.asSnowflake
import io.github.dseelp.framecord.api.bot
import io.github.dseelp.framecord.api.event.EventHandle
import io.github.dseelp.framecord.api.event.Listener
import io.github.dseelp.framecord.api.modules.checkModule
import io.github.dseelp.framecord.api.utils.MentionUtils
import io.github.dseelp.framecord.api.utils.asOverwrite
import io.github.dseelp.framecord.plugins.privatechannels.PrivateChannels.database
import io.github.dseelp.framecord.plugins.privatechannels.PrivateChannels.mId
import io.github.dseelp.framecord.plugins.privatechannels.PrivateChannels.suspendingDatabase
import io.github.dseelp.framecord.plugins.privatechannels.db.ActivePrivateChannelEntity
import io.github.dseelp.framecord.plugins.privatechannels.db.ActivePrivateChannelsTable
import io.github.dseelp.framecord.plugins.privatechannels.db.PrivateChannelEntity
import io.github.dseelp.framecord.plugins.privatechannels.db.PrivateChannelsTable
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@Listener
object ChannelListener {

    @EventHandle
    suspend fun onChannelDelete(event: VoiceChannelDeleteEvent) {
        checkModule(event.channel.guildId.value, mId) ?: return
        suspendingDatabase {
            suspendedTransaction {
                val channelId = event.channel.id.value
                val pnChannels = PrivateChannelEntity.find { PrivateChannelsTable.joinChannelId eq channelId }
                val guild = event.channel.getGuild()
                ActivePrivateChannelEntity.find { ActivePrivateChannelsTable.channelId eq channelId or (ActivePrivateChannelsTable.privateChannel inList pnChannels.map { it.id }) }
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
    }

    @EventHandle
    suspend fun onVoiceUpdate(event: VoiceStateUpdateEvent) {
        checkModule(event.state.guildId.value, mId) ?: return
        suspendingDatabase {
            val oldId = event.old?.channelId
            val newId = event.state.channelId
            suspend fun checkDeleted(state: VoiceState): Boolean {
                val channelId = state.channelId ?: return false
                val channel = getChannel(state.guildId.value, channelId.value) ?: return false
                val guildChannel = state.getGuild().getChannelOrNull(channel.channelId.asSnowflake) ?: return false
                if (guildChannel.type == ChannelType.GuildVoice) {
                    guildChannel as VoiceChannel
                    if (guildChannel.voiceStates.count() == 0) {
                        guildChannel.delete()
                        transaction {
                            channel.delete()
                        }
                        return true
                    } else {
                        if (newId == null) {
                            val userId = event.old!!.userId.value
                            if (suspendedTransaction {
                                    if (channel.ownerId == userId || channel.executiveId == userId) {
                                        val id =
                                            guildChannel.withStrategy(EntitySupplyStrategy.rest).voiceStates.first().userId.value
                                        if (id != channel.ownerId)
                                            channel.executiveId = id
                                        else channel.executiveId = null
                                        true
                                    } else false
                                }) return true
                        }
                    }
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

    fun getJoinChannel(guildId: ULong, channelId: ULong) = database {
        return@database transaction {
            PrivateChannelEntity.find { (PrivateChannelsTable.joinChannelId eq channelId) and (PrivateChannelsTable.guildId eq guildId) }
                .firstOrNull()
        }
    }

    fun getChannel(guildId: ULong, channelId: ULong) = database {
        return@database transaction {
            val possibleChannels =
                ActivePrivateChannelEntity.find { (ActivePrivateChannelsTable.channelId eq channelId) }
            val matchingGuildChannels = possibleChannels.filter { it.privateChannel.guildId == guildId }
            if (matchingGuildChannels.size > 1) throw IllegalStateException("This should simply not happen")
            return@transaction matchingGuildChannels.firstOrNull()
        }
    }

    suspend fun handleJoin(state: VoiceState) = suspendingDatabase {
        val channelId = state.channelId ?: return@suspendingDatabase
        val userId = state.userId.value
        getChannel(state.guildId.value, channelId.value)?.let { channel ->
            if (suspendedTransaction {
                    if (channel.ownerId == userId) {
                        channel.executiveId = null
                        true
                    } else false
                }) return@suspendingDatabase
        }
        val joinChannel = getJoinChannel(state.guildId.value, channelId.value) ?: return@suspendingDatabase
        val guild = state.getGuild()
        val member = state.getMember()
        val channel = state.getChannelOrNull()!!.asChannel() as VoiceChannel
        val createBuilder: VoiceChannelCreateBuilder.() -> Unit = {
            userLimit = channel.userLimit
            permissionOverwrites.addAll(channel.permissionOverwrites.map { it.asOverwrite })
            permissionOverwrites.add(
                Overwrite(
                    member.id,
                    OverwriteType.Member,
                    Permissions(Permission.Connect),
                    Permissions()
                )
            )
        }
        val name = suspendedTransaction { calculateChannelName(joinChannel, member) }
        val created =
            channel.category?.createVoiceChannel(name, createBuilder) ?: guild.createVoiceChannel(
                name,
                createBuilder
            )
        member.edit {
            this.voiceChannelId = created.id
        }
        transaction {
            ActivePrivateChannelEntity.new {
                this.channelId = created.id.value
                this.ownerId = member.id.value
                this.privateChannel = joinChannel
            }
        }
    }
}

suspend fun calculateChannelName(
    channel: PrivateChannelEntity,
    member: Member,
    template: String = channel.nameTemplate
): String {
    val result = template.replace("%user%", member.displayName, true).replace(
        "%game%",
        member.getPresenceOrNull()?.activities?.firstOrNull()?.name ?: channel.defaultGame,
        true
    )
    return MentionUtils.customEmojiRegex.replace(result, "")
}

suspend fun updateChannel(channel: ActivePrivateChannelEntity): String? {
    val guild = bot.kord.getGuild(channel.privateChannel.guildId.asSnowflake) ?: return null
    val guildChannel = guild.getChannel(channel.channelId.asSnowflake)
    val member = if (channel.executiveId == null) guild.getMember(channel.ownerId.asSnowflake) else guild.getMember(
        channel.executiveId!!.asSnowflake
    )
    val template = channel.customNameTemplate ?: channel.privateChannel.nameTemplate
    val calculated = calculateChannelName(channel.privateChannel, member, template)
    if (guildChannel is VoiceChannel && guildChannel.name != calculated && !channel.isRateLimited) {
        channel.lastUpdated = System.currentTimeMillis()
        guildChannel.edit {
            name = calculated
        }
    }
    return calculated
}

@OptIn(ExperimentalTime::class)
private val tenMinutesInMillis = 10.minutes.inWholeMilliseconds

val ActivePrivateChannelEntity.isRateLimited: Boolean
    get() = lastUpdated.let { if (it == null) false else System.currentTimeMillis() - it < tenMinutesInMillis }

@OptIn(ExperimentalTime::class)
val ActivePrivateChannelEntity.remainingRateLimited: Duration?
    get() {
        if (!isRateLimited) return null
        return ((lastUpdated!! + tenMinutesInMillis) - System.currentTimeMillis()).milliseconds
    }