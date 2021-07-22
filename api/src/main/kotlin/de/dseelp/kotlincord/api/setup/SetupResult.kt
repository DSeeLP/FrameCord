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

import dev.kord.core.entity.channel.GuildMessageChannel

data class SetupResult(
    val wasCancelled: Boolean,
    val isCompleted: Boolean,
    val channel: GuildMessageChannel,
    val results: Array<Any?>,
    val steps: Array<SetupStep>,
    val lastActiveStep: SetupStep?,
    val lastActiveStepIndex: Int?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SetupResult) return false

        if (wasCancelled != other.wasCancelled) return false
        if (isCompleted != other.isCompleted) return false
        if (!results.contentEquals(other.results)) return false
        if (!steps.contentEquals(other.steps)) return false
        if (lastActiveStep != other.lastActiveStep) return false
        if (lastActiveStepIndex != other.lastActiveStepIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = wasCancelled.hashCode()
        result = 31 * result + isCompleted.hashCode()
        result = 31 * result + results.contentHashCode()
        result = 31 * result + steps.contentHashCode()
        result = 31 * result + (lastActiveStep?.hashCode() ?: 0)
        result = 31 * result + (lastActiveStepIndex ?: 0)
        return result
    }
}
