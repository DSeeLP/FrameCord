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

package io.github.dseelp.framecord.core.utils

import com.uchuhimo.konf.ConfigSpec
import io.github.dseelp.framecord.api.presence.Activity
import io.github.dseelp.framecord.api.presence.Presence
import io.github.dseelp.framecord.api.presence.PresenceStatus
import io.github.dseelp.framecord.core.PathQualifiersImpl
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.path.div

@Serializable
data class PresenceConfig(val presences: Array<Presence>) {
    companion object : ConfigSpec("") {
        private val json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        private val file = (PathQualifiersImpl.configLocation / "presence.json").toFile()
        private val defaultText = json.encodeToString(
            PresenceConfig(
                arrayOf(
                    Presence(
                        PresenceStatus.ONLINE,
                        Activity.Playing("FrameCord"),
                        3000,
                        true
                    )
                )
            )
        )
        private fun checkConfig(text: String = defaultText): Boolean {
            return if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
                file.writeText(text, Charsets.UTF_8)
                false
            } else {
                true
            }
        }

        fun load(): PresenceConfig {
            checkConfig()
            val config = json.decodeFromString<PresenceConfig>(file.readText(Charsets.UTF_8))
            return config
        }

        fun write(config: PresenceConfig) {
            val text = json.encodeToString(config)
            if (checkConfig(text)) {
                file.writeText(text, Charsets.UTF_8)
            }
        }
    }
}



