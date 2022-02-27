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

package io.github.dseelp.framecord.api.setup

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.modify.actionRow
import io.github.dseelp.framecord.api.interactions.ButtonAction
import io.github.dseelp.framecord.api.interactions.SelectionMenu
import io.github.dseelp.framecord.api.plugins.Plugin
import io.github.dseelp.framecord.api.randomAlphanumeric
import io.github.dseelp.framecord.api.utils.action
import io.github.dseelp.framecord.api.utils.literal
import io.github.dseelp.framecord.api.utils.selectionMenu

class SelectionMenuStep(
    val plugin: Plugin,
    val messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit,
    options: Array<SelectionMenuStepOption>,
    val placeholder: String? = null,
    val allowedValues: IntRange = 1..1,
) : SetupStep {
    private val options = options.associateBy { randomAlphanumeric(16) }
    private var isDone = false

    @OptIn(KordPreview::class)
    private lateinit var buttonAction: ButtonAction
    private lateinit var selectionMenu: SelectionMenu
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
            execute {
                if (isDone) return@execute
                val acknowledge =
                    sender.interaction.deferPublicMessageUpdate()
                val ch = sender.interaction.channel.asChannel()
                if (ch !is GuildMessageChannel) return@execute
                val member = sender.interaction.user.asMember(ch.guildId)
                if (!checkAccess(member, ch)) return@execute
                isDone = true
                acknowledge.edit {
                    components?.clear()
                    actionRow {
                        selectionMenu(selectionMenu, true)
                    }
                    actionRow(actionRowBuilder(true))
                }
                setup.cancelSetup()
            }
        })
        selectionMenu = plugin.registerSelectionMenu {
            alwaysUseMultiOptionCallback = true
            placeholder = this@SelectionMenuStep.placeholder
            allowedValues = this@SelectionMenuStep.allowedValues
            this@SelectionMenuStep.options.forEach { (key, value) ->
                option(value.label, key) {
                    description = value.description
                    emoji = value.emoji
                    default = value.default
                }
            }
            onMultipleOptionClick {
                val acknowledge =
                    interaction.deferPublicMessageUpdate()
                if (isDone) return@onMultipleOptionClick
                val ch = interaction.channel.asChannel()
                if (ch !is GuildMessageChannel) return@onMultipleOptionClick
                val member = interaction.user.asMember(ch.guildId)
                if (!checkAccess(member, ch)) return@onMultipleOptionClick
                acknowledge.edit {
                    components?.clear()
                    actionRow {
                        selectionMenu(selectionMenu.copy(options = selectionMenu.options.map { option ->
                            if (selected.find { it.value == option.value } != null) option.copy(
                                default = true
                            ) else option
                        }.toTypedArray()), true)
                    }
                    actionRow(actionRowBuilder(true))
                }
                isDone = true
                plugin.unregisterButtonAction(buttonAction)
                plugin.unregisterSelectionMenu(selectionMenu)
                setup.completeStep(selected.mapNotNull { this@SelectionMenuStep.options[it.value] }.map { it.result }
                    .toTypedArray())
            }
        }
        return channel.createMessage {
            messageBuilder(this, channel)
            components.clear()
            actionRow {
                selectionMenu(selectionMenu)
            }
            actionRow(actionRowBuilder(false))
        }
    }

    private fun actionRowBuilder(disabled: Boolean): ActionRowBuilder.() -> Unit = {
        action(buttonAction, ButtonStyle.Danger, "", "Cancel", disabled = disabled)
    }

    override suspend fun cancel(message: Message) {
        plugin.unregisterButtonAction(buttonAction)
        plugin.unregisterSelectionMenu(selectionMenu)
    }
}

data class SelectionMenuStepOption(
    val label: String,
    val description: String? = null,
    val emoji: DiscordPartialEmoji? = null,
    val default: Boolean = false,
    val result: Any?
)