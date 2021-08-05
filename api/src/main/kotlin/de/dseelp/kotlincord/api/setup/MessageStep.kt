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

package de.dseelp.kotlincord.api.setup

import de.dseelp.kotlincord.api.action
import de.dseelp.kotlincord.api.event.EventHandle
import de.dseelp.kotlincord.api.interactions.ButtonAction
import de.dseelp.kotlincord.api.randomAlphanumeric
import de.dseelp.kotlincord.api.utils.literal
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.modify.actionRow

open class MessageStep(
    val messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit,
) : SetupStep {
    @OptIn(KordPreview::class)
    protected lateinit var channel: GuildMessageChannel
    protected lateinit var setup: Setup<*>
    protected lateinit var checkAccess: suspend (member: Member, channel: GuildMessageChannel) -> Boolean
    protected lateinit var buttonAction: ButtonAction
    private lateinit var message: Message
    protected var isDone = false

    @OptIn(KordPreview::class)
    override suspend fun send(
        setup: Setup<*>,
        channel: GuildMessageChannel,
        checkAccess: suspend (member: Member, channel: GuildMessageChannel) -> Boolean
    ): Message {
        if (this::channel.isInitialized || this::setup.isInitialized) throw IllegalStateException()
        buttonAction = setup.plugin.registerButtonAction(randomAlphanumeric(32), literal("") {
            execute {
                sender.interaction.acknowledgePublicDeferredMessageUpdate()
                setup.plugin.unregisterButtonAction(buttonAction)
                setup.cancelSetup()
            }
        })
        this.channel = channel
        this.setup = setup
        this.checkAccess = checkAccess
        message = channel.createMessage {
            messageBuilder(this, channel)
            components.clear()
            actionRow {
                action(buttonAction, ButtonStyle.Danger, "", "Cancel")
            }
        }
        return message
    }

    override fun cancel(message: Message) {
        setup.plugin.unregisterButtonAction(buttonAction)
    }

    @OptIn(KordPreview::class)
    @EventHandle
    suspend fun onMessageReceived(event: MessageCreateEvent) {
        if (isDone) return
        val message = event.message
        if (message.author?.id == event.kord.selfId) return
        if (message.author == null) return
        val channel = message.channel.asChannel()
        if (channel !is GuildMessageChannel) return
        if (event.guildId != channel.guildId) return
        if (!checkAccess(event.member!!, channel)) return
        val ok = handleMessage(message)
        if (ok) this.message.edit {
            components?.clear()
            actionRow {
                action(buttonAction, ButtonStyle.Danger, "", "Cancel", disabled = true)
            }
        }
        if (ok) isDone = true
    }

    open suspend fun handleMessage(message: Message): Boolean {
        setup.completeStep(message.content)
        return true
    }

}