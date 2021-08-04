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

package de.dseelp.kotlincord.plugins.privatechannels.commands

import de.dseelp.kommon.command.CommandBuilder
import de.dseelp.kommon.command.CommandContext
import de.dseelp.kommon.command.CommandNode
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.asSnowflake
import de.dseelp.kotlincord.api.checkPermissions
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.command.arguments.MentionArgument
import de.dseelp.kotlincord.api.command.createEmbed
import de.dseelp.kotlincord.api.setup.setup
import de.dseelp.kotlincord.api.utils.*
import de.dseelp.kotlincord.plugins.privatechannels.PrivateChannelPlugin
import de.dseelp.kotlincord.plugins.privatechannels.PrivateChannelPlugin.suspendingDatabase
import de.dseelp.kotlincord.plugins.privatechannels.db.ActivePrivateChannel
import de.dseelp.kotlincord.plugins.privatechannels.db.ActivePrivateChannels
import de.dseelp.kotlincord.plugins.privatechannels.db.PrivateChannel
import de.dseelp.kotlincord.plugins.privatechannels.db.PrivateChannels
import de.dseelp.kotlincord.plugins.privatechannels.isRateLimited
import de.dseelp.kotlincord.plugins.privatechannels.remainingRateLimited
import de.dseelp.kotlincord.plugins.privatechannels.updateChannel
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.editMemberPermission
import dev.kord.core.behavior.channel.editRolePermission
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.rest.builder.message.EmbedBuilder
import org.jetbrains.exposed.sql.and
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class RoomCommand : Command<GuildSender> {
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.GUILD)

    @OptIn(ExperimentalTime::class)
    private fun CommandBuilder<GuildSender>.adminCheck() {
        checkAccess {
            sender.getMember().checkPermissions(Permission.ManageChannels)
        }
        noAccess {
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
    suspend fun CommandContext<GuildSender>.checkRateLimit(channel: ActivePrivateChannel): Boolean {
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
    private fun CommandBuilder<GuildSender>.userCheck() {
        checkAccess {
            val member = sender.getMember()
            val channel = getMemberChannel(member) ?: return@checkAccess false
            suspendingDatabase {
                suspendedTransaction {
                    channel.ownerId == member.id.value || channel.executiveId == member.id.value
                }
            }
        }
        noAccess {
            sender.getChannel().createEmbed {
                title = "Permission denied"
                color = Color.red
                description = "You are not the room owner or not in any room."
                footer = sender.getMember().footer()
            }.deleteAfter(seconds(10))
        }
    }

    suspend fun getMemberChannel(member: Member): ActivePrivateChannel? = suspendingDatabase {
        suspendedTransaction {
            val voiceState = member.getVoiceStateOrNull() ?: return@suspendedTransaction null
            val channelId = voiceState.channelId?.value ?: return@suspendedTransaction null
            val channel = ActivePrivateChannel.find { ActivePrivateChannels.channelId eq channelId }.firstOrNull()
            return@suspendedTransaction channel
        }
    }

    @OptIn(
        InternalKotlinCordApi::class, dev.kord.common.annotation.KordPreview::class,
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
                """.trimIndent()
                footer = sender.getMember().footer()
            }
        }

        literal("rename") {
            userCheck()
            execute {
                sender.message.deleteIgnoringNotFound()
                val member = sender.getMember()
                val channel = getMemberChannel(member)!!
                val gFooter = sender.footer()
                if (checkRateLimit(channel)) return@execute
                setup(PrivateChannelPlugin, sender.getChannel()) {
                    messageStep {
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
                        }
                        val nameTemplate = it.results[0] as String
                        suspendingDatabase {
                            suspendedTransaction {
                                channel.customNameTemplate = nameTemplate
                                updateChannel(channel)
                            }
                        }
                        sender.createEmbed {
                            title = "Channel renamed"
                            description = "The channel has been renamed to `$name`"
                            footer = gFooter
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
                            sender.getGuild().getChannel(activeChannel.channelId.asSnowflake)
                        }
                        val targetId = target.id.value
                        if (!suspendedTransaction {
                                activeChannel.ownerId == targetId || activeChannel.executiveId == targetId
                            }) {
                            /*channel.addOverwrite(
                                PermissionOverwrite(
                                    PermissionOverwriteData(
                                        target.id,
                                        OverwriteType.Member,
                                        Permissions(),
                                        Permissions(Permission.Connect)
                                    )
                                )
                            )*/
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
                            footer = member.footer()
                        }.deleteAfter(seconds(30))
                    }
                }
            }
        }

        literal("unban") {
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
                            sender.getGuild().getChannel(activeChannel.channelId.asSnowflake)
                        }
                        val targetId = target.id.value
                        if (!suspendedTransaction {
                                activeChannel.ownerId == targetId || activeChannel.executiveId == targetId
                            }) {
                            /*channel.addOverwrite(
                                PermissionOverwrite(
                                    PermissionOverwriteData(
                                        target.id,
                                        OverwriteType.Member,
                                        Permissions(),
                                        Permissions()
                                    )
                                )
                            )*/
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
            execute {
                sender.message.deleteIgnoringNotFound()
                suspendingDatabase {
                    val member = sender.getMember()
                    val activeChannel = getMemberChannel(member)!!
                    val channel = suspendedTransaction {
                        sender.getGuild().getChannel(activeChannel.channelId.asSnowflake)
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
            execute {
                sender.message.deleteIgnoringNotFound()
                suspendingDatabase {
                    val member = sender.getMember()
                    val activeChannel = getMemberChannel(member)!!
                    val channel = suspendedTransaction {
                        sender.getGuild().getChannel(activeChannel.channelId.asSnowflake)
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
                setup(PrivateChannelPlugin, channel) {
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
                    messageStep {
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
                    messageStep {
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
                                if (PrivateChannel.find { PrivateChannels.joinChannelId eq channelId }.count() != 0L) {
                                    channel.createEmbed {
                                        title = "Private Channel creation failed"
                                        color = Color.red
                                        description = "${joinChannel.mention} is already a Private Channel"
                                    }
                                    return@suspendedTransaction
                                }
                                PrivateChannel.new {
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
                        val channels = PrivateChannel.find { PrivateChannels.guildId eq guildId }
                        channels to channels.count()
                    }
                    val member = sender.getMember()
                    if (count != 0L) setup(PrivateChannelPlugin, channel) {
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
                                        PrivateChannel.find { (PrivateChannels.joinChannelId eq value) and (PrivateChannels.guildId eq guildId) }
                                            .firstOrNull() ?: return@suspendedTransaction
                                    ActivePrivateChannel.find { ActivePrivateChannels.privateChannel eq privateChannel.id }
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