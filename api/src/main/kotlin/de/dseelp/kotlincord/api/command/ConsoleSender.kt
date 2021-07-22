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

package de.dseelp.kotlincord.api.command

import de.dseelp.kotlincord.api.console.ConsoleColor
import de.dseelp.kotlincord.api.logging.logger
import dev.kord.rest.builder.message.MessageCreateBuilder

object ConsoleSender : Sender {
    private val log by logger("")
    override val isConsole: Boolean = true
    override val name: String = "Console"
    override suspend fun sendMessage(vararg messages: String, parseColors: Boolean) {
        messages.onEach { log.command(if (parseColors) ConsoleColor.toColouredString('&', it) else it) }
    }

    override suspend fun sendMessage(message: MessageCreateBuilder.() -> Unit) {
        throw UnsupportedOperationException("A ConsoleSender can't send discord messages")
    }

}