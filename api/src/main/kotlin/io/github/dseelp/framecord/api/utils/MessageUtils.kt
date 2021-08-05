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

package io.github.dseelp.framecord.api.utils

import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

suspend fun MessageBehavior.deleteIgnoringNotFound() {
    try {
        delete()
    } catch (e: RestRequestException) {
        if (e.status.code != 404) {
            throw e
        }
    }
}

@ExperimentalTime
fun MessageBehavior.deleteAfterAsync(duration: Duration) = kord.async {
    delay(duration.inWholeMilliseconds)
    deleteIgnoringNotFound()
}

@OptIn(ExperimentalTime::class)
fun MessageBehavior.deleteAfter(duration: Duration) = kord.launch {
    delay(duration.inWholeMilliseconds)
    deleteIgnoringNotFound()
}

@ExperimentalTime
fun MessageBehavior.afterAsync(duration: Duration, block: suspend (MessageBehavior) -> Unit) = kord.async {
    delay(duration.inWholeMilliseconds)
    this@afterAsync.asMessageOrNull() ?: return@async
    block(this@afterAsync)
}

@ExperimentalTime
fun MessageBehavior.after(duration: Duration, block: suspend (MessageBehavior) -> Unit) = kord.launch {
    delay(duration.inWholeMilliseconds)
    this@after.asMessageOrNull() ?: return@launch
    block(this@after)
}

@ExperimentalTime
suspend fun MessageBehavior.after(block: suspend (MessageBehavior) -> Unit) {
    block(this)
}

suspend fun UserBehavior.footer(): EmbedBuilder.Footer {
    val member = asUser()
    return EmbedBuilder.Footer().apply {
        text = member.tag
        icon = member.avatar.url
    }
}


