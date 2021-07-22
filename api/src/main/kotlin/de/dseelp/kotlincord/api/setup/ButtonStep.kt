/*
 * Copyright (c) 2021 KotlinCord team & contributors
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

package de.dseelp.kotlincord.api.setup

import de.dseelp.kommon.command.CommandContext
import de.dseelp.kotlincord.api.action
import de.dseelp.kotlincord.api.interactions.ButtonAction
import de.dseelp.kotlincord.api.interactions.ButtonContext
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.randomAlphanumeric
import de.dseelp.kotlincord.api.utils.literal
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.interaction.actionRow
import dev.kord.rest.builder.message.MessageCreateBuilder

class ButtonStep(
    val plugin: Plugin,
    val messageBuilder: MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit,
    actions: Array<ButtonStepAction>,
) : SetupStep {
    private val actions = actions.associateBy { randomAlphanumeric(16) }
    private var isDone = false
    private lateinit var buttonAction: ButtonAction
    private lateinit var checkAccess: suspend (member: Member, channel: GuildMessageChannel) -> Boolean

    @OptIn(KordPreview::class)
    override suspend fun send(
        setup: Setup<*>,
        channel: GuildMessageChannel,
        checkAccess: suspend (member: Member, channel: GuildMessageChannel) -> Boolean
    ): Message {
        if (this::buttonAction.isInitialized) throw UnsupportedOperationException()
        this.checkAccess = checkAccess
        buttonAction = plugin.registerButtonAction(randomAlphanumeric(32), literal("") {
            for (action in this@ButtonStep.actions) {
                literal(action.key) {
                    execute {
                        val acknowledge =
                            sender.interaction.acknowledgePublicDeferredMessageUpdate()
                        if (isDone) return@execute
                        val channel = sender.interaction.channel.asChannel()
                        if (channel !is GuildMessageChannel) return@execute
                        val member = sender.interaction.user.asMember(channel.guildId)
                        if (!checkAccess(member, channel)) return@execute
                        acknowledge.edit {
                            components?.clear()
                            buildActionRows(true).onEach { actionRow(it) }
                        }
                        isDone = true
                        setup.completeStep(action.value.resultBlock(this))
                    }
                }
                literal("cancel") {
                    execute {
                        val acknowledge =
                            sender.interaction.acknowledgePublicDeferredMessageUpdate()
                        if (isDone) return@execute
                        val channel = sender.interaction.channel.asChannel()
                        if (channel !is GuildMessageChannel) return@execute
                        val member = sender.interaction.user.asMember(channel.guildId)
                        if (!checkAccess(member, channel)) return@execute
                        acknowledge.edit {
                            components?.clear()
                            buildActionRows(true).onEach { actionRow(it) }
                        }
                        isDone = true
                        setup.cancelSetup()
                    }
                }
            }
        })
        return channel.createMessage {
            messageBuilder.invoke(this, channel)
            components.clear()
            buildActionRows().onEach { actionRow(it) }
        }
    }

    @OptIn(KordPreview::class)
    private fun buildActionRows(disabledButtons: Boolean = false): Array<ActionRowBuilder.() -> Unit> {
        return arrayOf({
            for ((key, action) in actions) {
                action(buttonAction, action.style, key, action.label, action.emoji, disabledButtons)
            }
        }, {
            action(buttonAction, ButtonStyle.Danger, "cancel", "Cancel", disabled = disabledButtons)
        })
    }

    override fun cancel(message: Message) {
        if (!this::buttonAction.isInitialized) return
        plugin.unregisterButtonAction(buttonAction)
    }
}

data class ButtonStepAction @OptIn(KordPreview::class) constructor(
    val style: ButtonStyle,
    val label: String? = null,
    val emoji: DiscordPartialEmoji? = null,
    val resultBlock: CommandContext<ButtonContext>.() -> Any?
)