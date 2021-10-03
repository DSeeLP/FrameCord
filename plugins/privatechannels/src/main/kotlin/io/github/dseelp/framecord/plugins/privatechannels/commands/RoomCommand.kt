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

package io.github.dseelp.framecord.plugins.privatechannels.commands

import de.dseelp.kommon.command.CommandBuilder
import de.dseelp.kommon.command.CommandContext
import de.dseelp.kommon.command.CommandNode
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.editMemberPermission
import dev.kord.core.behavior.channel.editRolePermission
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.TopGuildChannel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import io.github.dseelp.framecord.api.asSnowflake
import io.github.dseelp.framecord.api.checkPermissions
import io.github.dseelp.framecord.api.command.Command
import io.github.dseelp.framecord.api.command.CommandScope
import io.github.dseelp.framecord.api.command.GuildSender
import io.github.dseelp.framecord.api.command.arguments.MentionArgument
import io.github.dseelp.framecord.api.command.createEmbed
import io.github.dseelp.framecord.api.modules.FeatureRestricted
import io.github.dseelp.framecord.api.randomAlphanumeric
import io.github.dseelp.framecord.api.setup.buttonDefaultValue
import io.github.dseelp.framecord.api.setup.setup
import io.github.dseelp.framecord.api.utils.*
import io.github.dseelp.framecord.plugins.privatechannels.PrivateChannels
import io.github.dseelp.framecord.plugins.privatechannels.PrivateChannels.mId
import io.github.dseelp.framecord.plugins.privatechannels.PrivateChannels.suspendingDatabase
import io.github.dseelp.framecord.plugins.privatechannels.db.ActivePrivateChannelEntity
import io.github.dseelp.framecord.plugins.privatechannels.db.ActivePrivateChannelsTable
import io.github.dseelp.framecord.plugins.privatechannels.db.PrivateChannelEntity
import io.github.dseelp.framecord.plugins.privatechannels.db.PrivateChannelsTable
import io.github.dseelp.framecord.plugins.privatechannels.isRateLimited
import io.github.dseelp.framecord.plugins.privatechannels.remainingRateLimited
import io.github.dseelp.framecord.plugins.privatechannels.updateChannel
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.sql.and
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class RoomCommand : Command<GuildSender> {
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.GUILD)
    override val featureRestricted: FeatureRestricted = FeatureRestricted(FeatureRestricted.Type.MODULE, mId)

    @OptIn(ExperimentalTime::class)
    private fun CommandBuilder<GuildSender>.adminCheck() {
        checkAccess {
            sender.getMember().checkPermissions(Permission.ManageChannels)
        }
        noAccess {
            sender.message.deleteIgnoringNotFound()
            sender.getChannel().createEmbed {
                title = "Permission denied"
                color = Color.red
                description = "You need the ManageChannel Permission to use this command"
                footer = sender.getMember().footer()
            }.deleteAfter(seconds(10))
        }
    }

    fun EmbedBuilder.applyPlaceholders() {
        field {
            name = "%user%"
            value = "The user name of the user creating the channel"
        }
        field {
            name = "%game%"
            value = "The game that the user creating the channel plays"
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun CommandContext<GuildSender>.checkRateLimit(channel: ActivePrivateChannelEntity): Boolean {
        if (channel.isRateLimited) {
            channel.remainingRateLimited?.toComponents { minutes, seconds, _ ->
                sender.createEmbed {
                    color = Color.red
                    title = "Command blocked"
                    description =
                        "You can execute the command again in ${if (minutes == 0) "$seconds seconds" else "$minutes minutes"}"
                    footer = sender.footer()
                }.deleteAfter(seconds(20))
            }
            return true
        }
        return false
    }

    @OptIn(ExperimentalTime::class)
    private fun CommandBuilder<GuildSender>.userCheck(allowExecutive: Boolean = true) {
        checkAccess {
            val member = sender.getMember()
            val channel = getMemberChannel(member) ?: return@checkAccess false
            suspendingDatabase {
                suspendedTransaction {
                    channel.ownerId == member.id.value || if (allowExecutive) channel.executiveId == member.id.value else false
                }
            }
        }
        noAccess {
            sender.message.deleteIgnoringNotFound()
            sender.getChannel().createEmbed {
                title = "Permission denied"
                color = Color.red
                description = "You are not the room owner or not in any room."
                footer = sender.getMember().footer()
            }.deleteAfter(seconds(10))
        }
    }

    suspend fun getMemberChannel(member: Member): ActivePrivateChannelEntity? = suspendingDatabase {
        suspendedTransaction {
            val voiceState = member.getVoiceStateOrNull() ?: return@suspendedTransaction null
            val channelId = voiceState.channelId?.value ?: return@suspendedTransaction null
            val channel = ActivePrivateChannelEntity.find { ActivePrivateChannelsTable.channelId eq channelId }.firstOrNull()
            return@suspendedTransaction channel
        }
    }

    @OptIn(
        io.github.dseelp.framecord.api.InternalFrameCordApi::class, dev.kord.common.annotation.KordPreview::class,
        ExperimentalTime::class
    )
    override val node: CommandNode<GuildSender> = literal("room") {
        execute {
            sender.message.deleteIgnoringNotFound()
            sender.getChannel().createEmbed {
                title = "Rooms Help"
                description = """
                    room create - Creates a join channel
                    room remove - Removes a created join channel
                    room ban <@Member> - Bans the provided member from the channel
                    room unban <@Member> - Unbans the provided member from the channel
                    room kick <@Member> - Kicks the provided member from the channel
                    room rename - Renames your private channel
                    room update - Updates the private channel
                    room lock - Locks the channel
                    room unlock - Unlocks the channel
                    room info - Shows an info about the room you are in
                    room move - Moves the ownership of a room to another person
                """.trimIndent()
                footer = sender.getMember().footer()
            }
        }

        literal("move") {
            userCheck(false)
            execute {
                sender.message.deleteIgnoringNotFound()
                val member = sender.getMember()
                setup(PrivateChannels, sender.getChannel()) {
                    checkAccess { m, _ ->
                        member.id == m.id
                    }

                    memberStep {
                        embed {
                            title = "Ownership Transfer"
                            description = "Please tag the member that would like to transfer ownership of your room to"
                        }
                    }
                    buttonStep {
                        message {
                            embed {
                                title = "Confirm Ownership transfer"
                                description = "Do you really want to transfer ownership of your room"
                            }
                        }
                        action(ButtonStyle.Danger, "Transfer") { true }
                        cancelAction(ButtonStyle.Primary, "Cancel") { false }
                    }

                    onCompletion {
                        if (it.wasCancelled) {
                            sender.createEmbed {
                                title = "Operation cancelled"
                                description =
                                    "The ownership of your room wasn't transferred" + if (it.lastActiveStepIndex == 1) " to ${(it.results[0] as Member).clientMention}" else ""
                            }
                            return@onCompletion
                        }
                        val target = it.results[0] as Member
                        if (target.isBot) {
                            sender.createEmbed {
                                title = "Operation Error"
                                description =
                                    "The ownership of your room can't be transferred to a bot"
                                color = Color.red
                            }
                            return@onCompletion
                        }
                        suspendingDatabase {
                            suspendedTransaction {
                                val channel = getMemberChannel(member) ?: return@suspendedTransaction
                                val guildChannel =
                                    sender.getGuild().getChannelOf<VoiceChannel>(channel.channelId.asSnowflake)
                                if (guildChannel.voiceStates.firstOrNull { state -> state.userId == target.id } == null) {
                                    sender.createEmbed {
                                        title = "Operation Error"
                                        description =
                                            "The ownership of your room can't be transferred to a user that is not in the room"
                                        color = Color.red
                                    }
                                    return@suspendedTransaction
                                }
                                channel.ownerId = target.id.value
                                sender.createEmbed {
                                    title = "Room Ownership transferred"
                                    description = "The Room owner is now ${target.clientMention}"
                                }
                            }
                        }
                    }
                }.start(true)
            }
        }

        literal("rename") {
            userCheck()
            execute {
                sender.message.deleteIgnoringNotFound()
                val member = sender.getMember()
                val gFooter = sender.footer()
                if (checkRateLimit(getMemberChannel(member)!!)) return@execute
                val resetValue = randomAlphanumeric(128)
                setup(PrivateChannels, sender.getChannel()) {
                    checkAccess { m, _ ->
                        member.id == m.id
                    }
                    messageStep(buttonDefaultValue {
                        label = "Reset"
                        computeResult {
                            sender.interaction.acknowledgePublicDeferredMessageUpdate()
                            resetValue
                        }
                    }) {
                        embed {
                            title = "Channel rename"
                            description = "Please enter the new name for the channel! You can use placeholders."
                            footer = gFooter
                            applyPlaceholders()
                        }
                    }
                    onCompletion {
                        if (it.wasCancelled) {
                            sender.createEmbed {
                                title = "Rename cancelled"
                                description = "The channel wasn't renamed"
                                footer = gFooter
                            }
                            return@onCompletion
                        }
                        val nameTemplate = it.results[0] as String
                        suspendingDatabase {
                            suspendedTransaction {
                                val channel = getMemberChannel(member)
                                if (channel == null) {
                                    sender.createEmbed {
                                        title = "Error!"
                                        color = Color.red
                                        description = "The channel doesn't exist anymore!"
                                        footer = gFooter
                                    }
                                    return@suspendedTransaction
                                }
                                if (nameTemplate == resetValue) {
                                    channel.customNameTemplate = null
                                } else
                                    channel.customNameTemplate = nameTemplate
                                val channelName = updateChannel(channel)
                                sender.createEmbed {
                                    title = "Channel renamed"
                                    description = "The channel has been renamed to `$channelName`"
                                    footer = gFooter
                                }
                            }
                        }

                    }
                }.start(true)
            }
        }

        literal("update") {
            userCheck()
            execute {
                sender.message.deleteIgnoringNotFound()
                suspendingDatabase {
                    suspendedTransaction {
                        getMemberChannel(sender.getMember())?.let { channel ->
                            if (checkRateLimit(channel)) return@let
                            updateChannel(channel)
                        }
                    }
                }
            }
        }

        literal("ban") {
            userCheck()
            execute {
                sender.getChannel().createEmbed {
                    title = "Error!"
                    description = "Please provide the member that should be banned from your channel!"
                }.deleteAfter(seconds(10))
            }

            argument(MentionArgument.member("member")) {
                userCheck()
                execute {
                    sender.message.deleteIgnoringNotFound()
                    suspendingDatabase {
                        val member = sender.getMember()
                        val target = get<Member>("member")
                        val activeChannel = getMemberChannel(member)!!
                        val channel = suspendedTransaction {
                            sender.getGuild().getChannel(activeChannel.channelId.asSnowflake) as TopGuildChannel
                        }
                        val targetId = target.id.value
                        if (!suspendedTransaction {
                                activeChannel.ownerId == targetId || activeChannel.executiveId == targetId
                            }) {
                            channel.editMemberPermission(target.id) {
                                denied += Permission.Connect
                            }
                            target.edit {
                                voiceChannelId = null
                            }
                            sender.getChannel().createEmbed {
                                title = "User banned"
                                description = "The user ${target.mention} was banned from ${channel.mention}"
                            }.deleteAfter(seconds(10))
                        }
                    }
                }
            }
        }

        literal("info") {
            checkAccess {
                getMemberChannel(sender.getMember()) != null
            }
            noAccess {
                sender.getChannel().createEmbed {
                    title = "No Room found"
                    color = Color.red
                    description = "You must be in a room to execute this command"
                    footer = sender.getMember().footer()
                }.deleteAfter(seconds(10))
            }

            execute {
                sender.message.deleteIgnoringNotFound()
                val member = sender.getMember()
                suspendingDatabase {
                    val channel = getMemberChannel(member)!!
                    val guildChannel = suspendedTransaction {
                        sender.getGuild().getChannel(channel.channelId.asSnowflake)
                    }
                    val kord = sender.message.kord
                    suspendedTransaction {
                        sender.getChannel().createEmbed {
                            title = guildChannel.name
                            field("Owner", inline = true) { kord.getUser(channel.ownerId.asSnowflake)!!.mention }
                            field("Executive Owner", inline = true) {
                                channel.executiveId?.asSnowflake?.let {
                                    kord.getUser(
                                        it
                                    )?.mention
                                } ?: "NaN"
                            }
                            field("Status") {
                                if (channel.locked) "Locked" else "Open"
                            }
                            footer = member.footer()
                        }.deleteAfter(seconds(30))
                    }
                }
            }
        }

        literal("unban") {
            userCheck()
            execute {
                sender.message.deleteIgnoringNotFound()
                sender.getChannel().createEmbed {
                    title = "Error!"
                    description = "Please provide the member that should be unbanned from your channel!"
                }.deleteAfter(seconds(10))
            }

            argument(MentionArgument.member("member")) {
                userCheck()
                execute {
                    sender.message.deleteIgnoringNotFound()
                    suspendingDatabase {
                        val member = sender.getMember()
                        val target = get<Member>("member")
                        val activeChannel = getMemberChannel(member)!!
                        val channel = suspendedTransaction {
                            sender.getGuild().getChannel(activeChannel.channelId.asSnowflake) as TopGuildChannel
                        }
                        val targetId = target.id.value
                        if (!suspendedTransaction {
                                activeChannel.ownerId == targetId || activeChannel.executiveId == targetId
                            }) {
                            channel.editMemberPermission(target.id) {
                                denied -= Permission.Connect
                            }
                            sender.getChannel().createEmbed {
                                title = "User unbanned"
                                description = "The user ${target.mention} was unbanned from ${channel.mention}"
                            }.deleteAfter(seconds(10))
                        }
                    }
                }
            }
        }

        literal("kick") {
            userCheck()
            execute {
                sender.message.deleteIgnoringNotFound()
                sender.getChannel().createEmbed {
                    title = "Error!"
                    description = "Please provide the member that should be unbanned from your channel!"
                }.deleteAfter(seconds(10))
            }

            argument(MentionArgument.member("member")) {
                userCheck()
                execute {
                    sender.message.deleteIgnoringNotFound()
                    suspendingDatabase {
                        val member = sender.getMember()
                        val target = get<Member>("member")
                        val activeChannel = getMemberChannel(member)!!
                        val channel = suspendedTransaction {
                            sender.getGuild().getChannel(activeChannel.channelId.asSnowflake) as VoiceChannel
                        }
                        val targetId = target.id.value
                        if (!suspendedTransaction {
                                activeChannel.ownerId == targetId || activeChannel.executiveId == targetId
                            }) {
                            target.edit {
                                voiceChannelId = null
                            }
                            sender.getChannel().createEmbed {
                                title = "User kicked"
                                description = "The user ${target.mention} was kicked from ${channel.mention}"
                            }.deleteAfter(seconds(10))
                        }
                    }
                }
            }
        }

        literal("lock") {
            userCheck()
            execute {
                sender.message.deleteIgnoringNotFound()
                suspendingDatabase {
                    val member = sender.getMember()
                    val activeChannel = getMemberChannel(member)!!
                    val channel = suspendedTransaction {
                        activeChannel.locked = true
                        sender.getGuild().getChannel(activeChannel.channelId.asSnowflake) as TopGuildChannel
                    }
                    channel.editRolePermission(channel.guildId) {
                        denied += Permission.Connect
                        allowed -= Permission.Connect
                    }
                    sender.getChannel().createEmbed {
                        title = "Channel locked"
                        description = "The channel ${channel.mention} is now locked"
                        footer = member.footer()
                    }.deleteAfter(seconds(10))
                }
            }
        }

        literal("delete") {
            userCheck()
            execute {
                sender.message.deleteIgnoringNotFound()
                suspendingDatabase {
                    val member = sender.getMember()
                    val activeChannel = getMemberChannel(member)!!
                    val channel = suspendedTransaction {
                        sender.getGuild().getChannel(activeChannel.channelId.asSnowflake)
                    }
                    channel.asChannelOrNull()?.delete()
                    sender.getChannel().createEmbed {
                        title = "Channel deleted"
                        description = "The channel `${channel.name}` was deleted"
                        footer = member.footer()
                    }.deleteAfter(seconds(10))
                }
            }
        }

        literal("unlock") {
            userCheck()
            execute {
                sender.message.deleteIgnoringNotFound()
                suspendingDatabase {
                    val member = sender.getMember()
                    val activeChannel = getMemberChannel(member)!!
                    val channel = suspendedTransaction {
                        activeChannel.locked = false
                        sender.getGuild().getChannel(activeChannel.channelId.asSnowflake) as TopGuildChannel
                    }
                    channel.editRolePermission(channel.guildId) {
                        denied -= Permission.Connect
                    }
                    sender.getChannel().createEmbed {
                        title = "Channel unlocked"
                        description = "The channel ${channel.mention} is now unlocked"
                        footer = member.footer()
                    }.deleteAfter(seconds(10))
                }
            }
        }

        literal("create") {
            adminCheck()
            execute {
                sender.message.deleteIgnoringNotFound()
                val channel = sender.getChannel()
                val member = sender.getMember()
                var shouldReturn = false
                suspendingDatabase {
                    suspendedTransaction {
                        val guildId = sender.getGuild().id.value
                        val privateChannelCount = PrivateChannelEntity.find { PrivateChannelsTable.guildId eq guildId }.count()
                        if (privateChannelCount >= 25) {
                            sender.createEmbed {
                                title = "Limit reached"
                                color = Color.red
                                description = "A Guild can only have 25 join channels."
                            }
                            shouldReturn = true
                        }
                    }
                }
                if (shouldReturn) return@execute
                setup(PrivateChannels, channel) {
                    checkAccess { executor, channel ->
                        executor.id == member.id
                    }

                    voiceChannelStep {
                        embed {
                            title = "Create Private Channel"
                            description =
                                "Please tag a voice channel where the users should join to create a Private Channel \n To tag a voice channel use `#!Channel`"
                            footer = member.footer()
                        }
                    }
                    messageStep(buttonDefaultValue {
                        label = "Use default"
                        computeResult {
                            sender.interaction.acknowledgePublicDeferredMessageUpdate()
                            "%user%'s Room"
                        }
                    }) {
                        embed {
                            title = "Create Private Channel"
                            description = """
                                Please enter a name template for the created channels.
                                The default template is: `%user%'s Room`
                            """.trimIndent()
                            applyPlaceholders()
                            footer = member.footer()
                        }
                    }
                    messageStep(buttonDefaultValue {
                        label = "Use default"
                        computeResult {
                            sender.interaction.acknowledgePublicDeferredMessageUpdate()
                            "a Game"
                        }
                    }) {
                        embed {
                            title = "Create Private Channel"
                            description = """
                                Please enter a text that is shown when someone not plays a game.
                                The default text is: `a Game`
                            """.trimIndent()
                            footer = member.footer()
                        }
                    }
                    onCompletion { result ->
                        if (result.wasCancelled) {
                            channel.createEmbed {
                                title = "Private Channel creation cancelled"
                                description = "The creation of a private channel was cancelled"
                                footer = member.footer()
                            }
                            return@onCompletion
                        }
                        val joinChannel = result.results[0] as VoiceChannel
                        if (joinChannel.asChannelOrNull() == null) return@onCompletion
                        suspendingDatabase {
                            suspendedTransaction {
                                val channelId = joinChannel.id.value
                                if (PrivateChannelEntity.find { PrivateChannelsTable.joinChannelId eq channelId }.count() != 0L) {
                                    channel.createEmbed {
                                        title = "Private Channel creation failed"
                                        color = Color.red
                                        description = "${joinChannel.mention} is already a Private Channel"
                                    }
                                    return@suspendedTransaction
                                }
                                PrivateChannelEntity.new {
                                    joinChannelId = channelId
                                    guildId = joinChannel.guildId.value
                                    nameTemplate = result.results[1] as String
                                    defaultGame = result.results[2] as String
                                }
                                channel.createEmbed {
                                    title = "Private Channel created"
                                    color = Color.green
                                    description = "${joinChannel.mention} is now a Private Channel"
                                    footer = member.footer()
                                }
                            }
                        }
                    }
                }.start(true)
            }
        }

        literal("remove") {
            adminCheck()
            execute {
                sender.message.deleteIgnoringNotFound()
                suspendingDatabase {
                    val channel = sender.getChannel()
                    val guildId = channel.guildId.value
                    val guild = channel.guild.asGuild()
                    val (privateChannels, count) = suspendedTransaction {
                        val channels = PrivateChannelEntity.find { PrivateChannelsTable.guildId eq guildId }
                        channels to channels.count()
                    }
                    val member = sender.getMember()
                    if (count != 0L) setup(PrivateChannels, channel) {
                        checkAccess { executor, channel ->
                            executor.id == member.id
                        }
                        selectionStep {
                            message {
                                embed {
                                    title = "Remove PrivateChannel"
                                    description = "Please select the PrivateChannel you want to delete"
                                    footer = member.footer()
                                }
                            }
                            suspendedTransaction {
                                for (ch in privateChannels) {
                                    val channel2 = guild.getChannelOrNull(ch.joinChannelId.asSnowflake)
                                    if (channel2 == null) {
                                        ch.delete()
                                        continue
                                    }
                                    option(channel2.name, channel2.id)
                                }
                            }
                        }
                        buttonStep {
                            message {
                                embed {
                                    title = "Confirm Deletion"
                                    description =
                                        "Do you really want to delete this private channel. All users using it will be kicked out of their channels."
                                    footer = member.footer()
                                }
                            }
                            action(ButtonStyle.Danger, "Delete") { true }
                            cancelAction(ButtonStyle.Primary, "Cancel") { false }
                        }
                        onCompletion { result ->
                            if (channel.asChannelOrNull() == null) return@onCompletion
                            if (result.wasCancelled || result.results[1] == false) {
                                channel.createEmbed {
                                    title = "Private Channels"
                                    description = "No room was removed"
                                }
                                return@onCompletion
                            }
                            if (result.results[1] == true) {
                                @Suppress("UNCHECKED_CAST")
                                val snowflake = (result.results[0] as Array<Any?>)[0] as Snowflake
                                val pChannel = guild.getChannelOrNull(snowflake) ?: return@onCompletion
                                channel.createEmbed {
                                    title = "Private Channels"
                                    description = "The room ${pChannel.name} was removed"
                                    footer = member.footer()
                                }
                                pChannel.delete()
                                suspendedTransaction {
                                    val value = snowflake.value
                                    val privateChannel =
                                        PrivateChannelEntity.find { (PrivateChannelsTable.joinChannelId eq value) and (PrivateChannelsTable.guildId eq guildId) }
                                            .firstOrNull() ?: return@suspendedTransaction
                                    ActivePrivateChannelEntity.find { ActivePrivateChannelsTable.privateChannel eq privateChannel.id }
                                        .onEach {
                                            guild.getChannelOrNull(it.channelId.asSnowflake)?.delete()
                                            it.delete()
                                        }
                                    privateChannel.delete()
                                }

                            }
                        }
                    }.start(true)
                    else {
                        sender.getChannel().createEmbed {
                            title = "Error!"
                            description = "There are no private channels in this guild!"
                            footer = member.footer()
                        }
                    }
                }
            }
        }
    }
}