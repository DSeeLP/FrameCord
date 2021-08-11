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
import dev.kord.common.entity.ActivityType
import dev.kord.common.entity.PresenceStatus.*
import io.github.dseelp.framecord.core.PathQualifiersImpl
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlin.io.path.div

@Serializable
data class PresenceConfig(val presences: Array<Presence>) {
    companion object : ConfigSpec("") {

        fun load() {
            val json = Json {
                prettyPrint = true
                prettyPrintIndent = "  "
            }
            val file = (PathQualifiersImpl.configLocation / "presence.json").toFile()
            val defaultText = json.encodeToString(
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
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
                file.writeText(defaultText, Charsets.UTF_8)
            }
            val config = json.decodeFromString<PresenceConfig>(file.readText(Charsets.UTF_8))
            println(config)
        }
    }
}

@Serializable
data class Presence(val status: PresenceStatus, val activity: Activity, val stayTime: Long, val enabled: Boolean)

enum class PresenceStatus(val status: dev.kord.common.entity.PresenceStatus) {
    ONLINE(Online),
    IDLE(Idle),
    DO_NOT_DISTURB(DoNotDisturb),
    INVISIBLE(Invisible)
}

@Serializable
sealed class Activity {
    abstract val activityType: ActivityType

    @Serializable
    @SerialName("playing")
    class Playing(val value: String) : Activity() {
        @Transient
        override val activityType: ActivityType = ActivityType.Game
    }

    @Serializable
    @SerialName("streaming")
    class Streaming(val value: String) : Activity() {
        @Transient
        override val activityType: ActivityType = ActivityType.Game
    }

    @Serializable
    @SerialName("listening")
    class Listening(val value: String) : Activity() {
        @Transient
        override val activityType: ActivityType = ActivityType.Listening
    }

    @Serializable
    @SerialName("watching")
    class Watching(val value: String) : Activity() {
        @Transient
        override val activityType: ActivityType = ActivityType.Watching
    }

    @Serializable
    @SerialName("competing")
    class Competing(val value: String) : Activity() {
        @Transient
        override val activityType: ActivityType = ActivityType.Competing
    }
}

