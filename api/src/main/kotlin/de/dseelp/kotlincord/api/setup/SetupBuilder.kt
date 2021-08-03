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

import de.dseelp.kommon.command.CommandContext
import de.dseelp.kotlincord.api.apply
import de.dseelp.kotlincord.api.interactions.ButtonContext
import de.dseelp.kotlincord.api.plugins.Plugin
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.MessageCreateBuilder

suspend fun <P : Plugin> setup(
    plugin: P,
    channel: GuildMessageChannel,
    block: suspend SetupBuilder<P>.() -> Unit
): Setup<P> {
    val builder = SetupBuilder(plugin, channel)
    builder.apply { }
    block.invoke(builder)
    return builder.build()
}


class SetupBuilder<P : Plugin>(val plugin: P, val channel: GuildMessageChannel) {
    val steps = mutableListOf<SetupStep>()
    var onCompletion: suspend (result: SetupResult) -> Unit = {}
    var checkAccess: suspend (member: Member, channel: GuildMessageChannel) -> Boolean = { _, _ -> true }

    fun checkAccess(checkAccess: suspend (member: Member, channel: GuildMessageChannel) -> Boolean) {
        this.checkAccess = checkAccess
    }

    fun onCompletion(onCompletion: suspend (result: SetupResult) -> Unit) {
        this.onCompletion = onCompletion
    }

    fun step(step: SetupStep) {
        steps.add(step)
    }

    suspend fun buttonStep(block: suspend ButtonStepBuilder.() -> Unit) {
        step(ButtonStepBuilder().apply(block).build(plugin))
    }

    suspend fun selectionStep(block: suspend SelectionMenuStepBuilder.() -> Unit) {
        step(SelectionMenuStepBuilder().apply(block).build(plugin))
    }

    fun messageStep(messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit) {
        step(MessageStep(messageBuilder))
    }

    fun channelStep(messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit) {
        step(ChannelStep(messageBuilder))
    }

    fun voiceChannelStep(messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit) {
        step(VoiceChannelStep(messageBuilder))
    }

    fun messageChannelStep(messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit) {
        step(MessageChannelStep(messageBuilder))
    }

    fun textChannelStep(messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit) {
        step(TextChannelStep(messageBuilder))
    }

    fun newsChannelStep(messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit) {
        step(NewsChannelStep(messageBuilder))
    }

    fun roleStep(messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit) {
        step(RoleStep(messageBuilder))
    }

    fun memberStep(messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit) {
        step(MemberStep(messageBuilder))
    }

    fun userStep(messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit) {
        step(UserStep(messageBuilder))
    }

    fun build(): Setup<P> {
        return Setup(plugin, channel, steps.toTypedArray(), onCompletion, checkAccess)
    }
}

@OptIn(KordPreview::class)
class ButtonStepBuilder {
    val actions = mutableListOf<ButtonStepAction>()
    var messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit = {}

    fun message(messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit) {
        this.messageBuilder = messageBuilder
    }

    fun action(
        style: ButtonStyle,
        label: String? = null,
        emoji: DiscordPartialEmoji? = null,
        resultBlock: CommandContext<ButtonContext>.() -> Any?
    ) {
        actions.add(ButtonStepAction(style, label, emoji, false, resultBlock))
    }

    fun cancelAction(
        style: ButtonStyle,
        label: String? = null,
        emoji: DiscordPartialEmoji? = null,
        resultBlock: CommandContext<ButtonContext>.() -> Any?
    ) {
        actions.add(ButtonStepAction(style, label, emoji, true, resultBlock))
    }

    fun build(plugin: Plugin): ButtonStep = ButtonStep(plugin, messageBuilder, actions.toTypedArray())
}

@OptIn(KordPreview::class)
class SelectionMenuStepBuilder {
    val options = mutableListOf<SelectionMenuStepOption>()
    var messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit = {}
    var placeholder: String? = null
    var allowedValues: IntRange = 1..1

    fun message(messageBuilder: suspend MessageCreateBuilder.(channel: GuildMessageChannel) -> Unit) {
        this.messageBuilder = messageBuilder
    }

    fun option(
        label: String,
        result: Any?,
        description: String? = null,
        emoji: DiscordPartialEmoji? = null,
        default: Boolean = false
    ) {
        options.add(SelectionMenuStepOption(label, description, emoji, default, result))
    }

    fun build(plugin: Plugin): SelectionMenuStep =
        SelectionMenuStep(plugin, messageBuilder, options.toTypedArray(), placeholder, allowedValues)
}