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

package io.github.dseelp.framecord.api.configs

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import io.github.dseelp.framecord.api.randomAlphanumeric

data class BotConfig(val instanceId: String, val debug: Boolean, val invite: InviteConfig, val intents: IntentsConfig) {
    companion object : ConfigSpec("") {
        val instanceId by optional(randomAlphanumeric(4))
        val debugMode by optional(false)

        object InviteSpec : ConfigSpec() {
            val enabled by optional(false)
            val clientId by optional(-1L)
        }

        object IntentsSpec : ConfigSpec() {
            val presence by optional(false)
            val guildMembers by optional(false)
        }

        fun fromConfig(config: Config): BotConfig = BotConfig(
            config[instanceId],
            config[debugMode],
            InviteConfig(config[InviteSpec.enabled], config[InviteSpec.clientId]),
            IntentsConfig(config[IntentsSpec.presence], config[IntentsSpec.guildMembers])
        )
    }


    data class InviteConfig(val enabled: Boolean, val clientId: Long)
    data class IntentsConfig(val presence: Boolean, val guildMembers: Boolean)
}
