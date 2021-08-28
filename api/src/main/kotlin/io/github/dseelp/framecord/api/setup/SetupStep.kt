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

import de.dseelp.kommon.command.CommandContext
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import io.github.dseelp.framecord.api.interactions.ButtonContext

interface SetupStep {

    suspend fun send(
        setup: Setup<*>,
        channel: GuildMessageChannel,
        checkAccess: suspend (member: Member, channel: GuildMessageChannel) -> Boolean
    ): Message

    suspend fun cancel(message: Message)
}

data class ButtonDefaultValue(
    val result: suspend CommandContext<ButtonContext>.() -> Any?,
    val style: ButtonStyle,
    val label: String? = null,
    val emoji: DiscordPartialEmoji? = null
)

class ButtonDefaultValueBuilder {
    var result: suspend CommandContext<ButtonContext>.() -> Any? = { null }
    var style: ButtonStyle = ButtonStyle.Primary
    var label: String? = null
    var emoji: DiscordPartialEmoji? = null

    fun computeResult(block: suspend CommandContext<ButtonContext>.() -> Any?) {
        result = block
    }

    fun build() = ButtonDefaultValue(result, style, label, emoji)
}

fun buttonDefaultValue(builderBlock: ButtonDefaultValueBuilder.() -> Unit): ButtonDefaultValue {
    return ButtonDefaultValueBuilder().apply(builderBlock).build()
}