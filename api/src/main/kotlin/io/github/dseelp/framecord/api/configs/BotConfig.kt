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

import com.log4k.Level
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import io.github.dseelp.framecord.api.logging.CommandLogLevel
import io.github.dseelp.framecord.api.randomAlphanumeric

data class BotConfig(
    val instanceId: String,
    val logging: LoggingConfig,
    val invite: InviteConfig,
    val intents: IntentsConfig,
    val rest: RestConfig,
    val clientId: Long,
    val clientSecret: String,
    val botAdmins: Array<Long>
) {
    companion object : ConfigSpec("") {
        val instanceId by optional(randomAlphanumeric(4))
        val clientId by optional(-1L)
        val clientSecret by optional("Hi")
        val botAdmins by optional(arrayOf<Long>())

        object LoggingSpec : ConfigSpec() {
            val showErrors by optional(false)
            val fullClassNames by optional(false)
            val defaultLevel by optional(LogLevel.INFO)
            val ignorePatterns by optional(false)
            val patterns by optional(
                arrayOf(
                    InLogPattern("^com.zaxxer.hikari.*", LogLevel.WARN),
                    InLogPattern("^Exposed\$", LogLevel.WARN)
                )
            )
        }

        object InviteSpec : ConfigSpec() {
            val enabled by optional(false)
        }

        object IntentsSpec : ConfigSpec() {
            val presence by optional(false)
            val guildMembers by optional(false)
            val messages by optional(false)
        }

        object RestSpec : ConfigSpec() {
            val enabled by optional(false)
            val host by optional("127.0.0.1")
            val port by optional(2340)
            val redirectUrl by optional("http://127.0.0.1:2340")
            val proxySupport by optional(false)
        }

        enum class LogLevel {
            VERBOSE,
            DEBUG,
            INFO,
            COMMAND,
            WARN,
            ERROR;

            val log4k: Level
                get() = when (this) {
                    VERBOSE -> Level.Verbose
                    DEBUG -> Level.Debug
                    INFO -> Level.Info
                    COMMAND -> CommandLogLevel
                    WARN -> Level.Warn
                    ERROR -> Level.Error
                }
        }

        fun fromConfig(config: Config): BotConfig = BotConfig(
            config[instanceId],
            LoggingConfig(
                config[LoggingSpec.defaultLevel].log4k,
                config[LoggingSpec.showErrors],
                config[LoggingSpec.ignorePatterns],
                config[LoggingSpec.fullClassNames],
                config[LoggingSpec.patterns].map { LogPattern(Regex(it.pattern), it.level) }.toTypedArray()
            ),
            InviteConfig(config[InviteSpec.enabled]),
            IntentsConfig(config[IntentsSpec.presence], config[IntentsSpec.guildMembers], config[IntentsSpec.messages]),
            RestConfig(
                config[RestSpec.enabled],
                config[RestSpec.host],
                config[RestSpec.port],
                config[RestSpec.redirectUrl],
                config[RestSpec.proxySupport]
            ),
            config[clientId],
            config[clientSecret],
            config[botAdmins]
        )
    }


    data class InviteConfig(val enabled: Boolean)
    data class IntentsConfig(val presence: Boolean, val guildMembers: Boolean, val messages: Boolean)
    data class RestConfig(
        val enabled: Boolean,
        val host: String,
        val port: Int,
        val redirectUrl: String,
        val proxySupport: Boolean
    )

    data class LoggingConfig(
        val defaultLevel: Level,
        val showErrors: Boolean,
        val ignorePatterns: Boolean,
        val fullClassNames: Boolean,
        val patterns: Array<LogPattern>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LoggingConfig) return false

            if (defaultLevel != other.defaultLevel) return false
            if (showErrors != other.showErrors) return false
            if (ignorePatterns != other.ignorePatterns) return false
            if (fullClassNames != other.fullClassNames) return false
            if (!patterns.contentEquals(other.patterns)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = defaultLevel.hashCode()
            result = 31 * result + showErrors.hashCode()
            result = 31 * result + ignorePatterns.hashCode()
            result = 31 * result + fullClassNames.hashCode()
            result = 31 * result + patterns.contentHashCode()
            return result
        }

    }


    data class InLogPattern(val pattern: String, val level: LogLevel)
    data class LogPattern(val pattern: Regex, val level: LogLevel)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BotConfig) return false

        if (instanceId != other.instanceId) return false
        if (logging != other.logging) return false
        if (invite != other.invite) return false
        if (intents != other.intents) return false
        if (rest != other.rest) return false
        if (clientId != other.clientId) return false
        if (clientSecret != other.clientSecret) return false
        if (!botAdmins.contentEquals(other.botAdmins)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = instanceId.hashCode()
        result = 31 * result + logging.hashCode()
        result = 31 * result + invite.hashCode()
        result = 31 * result + intents.hashCode()
        result = 31 * result + rest.hashCode()
        result = 31 * result + clientId.hashCode()
        result = 31 * result + clientSecret.hashCode()
        result = 31 * result + botAdmins.contentHashCode()
        return result
    }
}
