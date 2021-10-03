package io.github.dseelp.framecord.core.logging

import com.log4k.*
import io.github.dseelp.framecord.api.InternalFrameCordApi
import io.github.dseelp.framecord.api.configs.BotConfig
import io.github.dseelp.framecord.api.console.ConsoleColor
import io.github.dseelp.framecord.api.logging.CommandLogLevel
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import io.github.dseelp.framecord.core.ConsoleImpl
import io.github.dseelp.framecord.core.CordImpl
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*

fun setupLogging(config: BotConfig.LoggingConfig) {
    Log4k.add(config.defaultLevel, ".*", CordAppender(lConfig = config, updateConfig = true))
}

open class CordAppender(

    /**
     * A lambda function to generate a string of the timestamp of the log.
     */
    private val generateTimestamp: () -> String = {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        sdf.timeZone = Calendar.getInstance().timeZone
        "${sdf.format(Date())}/ "
    },

    /**
     * A lambda function to generate a string to show level of the log.
     */
    private val generateLevel: (Level) -> String = {
        when (it.level) {
            Level.Verbose.level -> "VERBOSE"
            Level.Debug.level -> "DEBUG"
            Level.Info.level -> "INFO"
            CommandLogLevel.level -> "COMMAND"
            Level.Warn.level -> "WARNING"
            Level.Error.level -> "ERROR"
            Level.Assert.level -> "ASSERT"
            else -> "UNKNOWN"
        }
    },

    /**
     * A lambda function to generate a string to show class name of the log
     */
    private val generateClassName: (Config) -> String = {
        it.tag.substringAfterLast('.')
    },

    private val colored: Boolean = true,

    val lConfig: BotConfig.LoggingConfig = BotConfig.LoggingConfig(
        Level.Info,
        showErrors = false,
        ignorePatterns = false,
        fullClassNames = false,
        patterns = arrayOf()
    ),

    val updateConfig: Boolean = false,

    val logAppender: LogAppender = LogAppender(lConfig, updateConfig, colored, generateClassName, generateLevel),

    private val log: (level: Level, config: Config, event: Event) -> Unit = log@{ level, config, event ->
        logAppender.log(level, config, event)
    }
) : Appender({ level, config, event ->
    log(level, config, event)
})

@OptIn(InternalFrameCordApi::class)
open class LogAppender(
    lConfig: BotConfig.LoggingConfig,
    val updateConfig: Boolean = false,
    val colored: Boolean,
    val generateClassName: (Config) -> String,
    val generateLevel: (Level) -> String
) : CordKoinComponent {
    fun StringBuilder.appendColor(color: ConsoleColor) {
        if (colored) append(color)
    }

    private val lConfigParam = lConfig

    val lConfig: BotConfig.LoggingConfig
        get() = if (!updateConfig) lConfigParam else kotlin.runCatching { getKoin().get<BotConfig>() }.getOrNull()?.logging ?: lConfigParam

    fun log(level: Level, cfg: Config, event: Event) {
        val (message, throwable) = when (event) {
            is SimpleEvent -> event.message to null
            is SimpleThrowableEvent -> event.message to event.throwable
            else -> null to null
        }
        val config = Config(
            if (lConfig.ignorePatterns) cfg.enable else (!lConfig.patterns.any { it.pattern.matches(cfg.tag) }) && cfg.enable,
            cfg.tag,
            cfg.owners
        )
        if (!config.enable) return
        val lines = message?.lines()
        if (lines == null) {
            logAndPrint(level, config, null, throwable)
            return
        }
        lines.forEachIndexed { index, s ->
            val t = if (index == lines.lastIndex) throwable else null
            logAndPrint(level, config, s, t)
        }
    }

    fun showErrors(level: Level): Boolean =
        level.level <= Level.Debug.level || System.getProperty("showErrors").toBoolean()

    fun getColor(level: Level): ConsoleColor? = when {
        !colored -> null
        level.level >= Level.Warn.level -> ConsoleColor.RED
        else -> ConsoleColor.WHITE
    }

    fun renderLevel(level: Level, color: ConsoleColor? = getColor(level)): String {
        return (color?.toString() ?: "") + generateLevel(level)
    }

    fun logAndPrint(level: Level, config: Config, message: String?, throwable: Throwable? = null) {
        val builtMessage = if (message != null) buildString {
            appendColor(ConsoleColor.DARK_GRAY)
            append("[")
            appendColor(ConsoleColor.WHITE)
            append(CordImpl.formatter.format(System.currentTimeMillis()))
            appendColor(ConsoleColor.DARK_GRAY)
            append("] ")
            appendColor(ConsoleColor.GRAY)
            val color = getColor(level)
            append(renderLevel(level, color))
            appendColor(ConsoleColor.DARK_GRAY)
            append(": ")
            appendColor(ConsoleColor.DEFAULT)
            val logName = if (lConfig.fullClassNames) config.tag else generateClassName(config)
            if (logName != "") {
                appendColor(ConsoleColor.DARK_GRAY)
                append("[")
                appendColor(ConsoleColor.WHITE)
                append(logName)
                appendColor(ConsoleColor.DARK_GRAY)
                append("] ")
                appendColor(ConsoleColor.DEFAULT)
            }
            if (color == ConsoleColor.RED) appendColor(ConsoleColor.YELLOW)
            append(message)
            appendColor(ConsoleColor.DEFAULT)
        } else null
        if (builtMessage != null && !(message?.isEmpty() == true && throwable != null)) ConsoleImpl.forceWriteLine(
            builtMessage
        )
        throwable?.let { t ->
            val stream = stream(level, config)
            if (showErrors(level)) {
                t.printStackTrace(stream)
            } else {
                val uuid = ErrorManagerImpl.dispatch(t)
                stream.println("${t::class.qualifiedName}${if (t.message == null) "" else ": ${t.message}"} ($uuid)")
            }
        }
    }

    fun stream(level: Level, config: Config) =
        PrintStream(ConsoleImpl.ActionOutputStream { logAndPrint(level, config, it) }, true)
}