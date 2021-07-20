/*
 * Copyright (c) 2021 KotlinCord team & contributors
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

package org.slf4j.impl

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.console.Console
import de.dseelp.kotlincord.api.console.ConsoleColor
import de.dseelp.kotlincord.api.logging.KLogger
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.core.ConsoleImpl
import de.dseelp.kotlincord.core.CordImpl
import org.koin.core.component.inject
import org.slf4j.helpers.MarkerIgnoringBase
import org.slf4j.helpers.MessageFormatter
import org.slf4j.spi.LocationAwareLogger
import java.io.PrintStream

@OptIn(InternalKotlinCordApi::class)
class CordLogger(name: String) : MarkerIgnoringBase(), KLogger, CordKoinComponent {
    private val LOG_LEVEL_TRACE = LocationAwareLogger.TRACE_INT
    private val LOG_LEVEL_DEBUG = LocationAwareLogger.DEBUG_INT
    private val LOG_LEVEL_INFO = LocationAwareLogger.INFO_INT
    private val LOG_LEVEL_COMMAND = LocationAwareLogger.INFO_INT + 5
    private val LOG_LEVEL_WARN = LocationAwareLogger.WARN_INT
    private val LOG_LEVEL_ERROR = LocationAwareLogger.ERROR_INT

    private val console by inject<Console>()

    var currentLogLevel = if (System.getProperty("debugMode").toBoolean()) LOG_LEVEL_DEBUG else LOG_LEVEL_INFO
        private set

    fun isLevelEnabled(logLevel: Int): Boolean {
        currentLogLevel = if (System.getProperty("debugMode").toBoolean()) LOG_LEVEL_DEBUG else LOG_LEVEL_INFO
        return logLevel >= currentLogLevel
    }

    private val shortLogName = name.substring(name.lastIndexOf(".") + 1)

    fun log(level: Int, message: String?, throwable: Throwable? = null) {
        if (!isLevelEnabled(level)) return
        val lines = message?.lines()
        if (lines == null) {
            logAndPrint(level, null, throwable)
            return
        }
        lines.forEachIndexed { index, s ->
            val t = if (index == lines.lastIndex) throwable else null
            logAndPrint(level, s, t)
        }
    }

    fun stream(level: Int) = PrintStream(ConsoleImpl.ActionOutputStream { logAndPrint(level, it) }, true)

    val colored = true

    /*fun logAndPrint(level: Int, message: String?, throwable: Throwable? = null) {
        if (!isLevelEnabled(level)) return
        val builtMessage = if (message != null) buildString {
            append(CordImpl.formatter.format(System.currentTimeMillis()))
            append(' ')
            append("[${renderLevel(level)}]")
            if (shortLogName != "") append(" $shortLogName")
            append(" - ")
            append(message)
        } else null
        if (builtMessage != null) console.forceWriteLine(builtMessage)
        throwable?.printStackTrace(console.printStream)
    }*/

    fun logAndPrint(level: Int, message: String?, throwable: Throwable? = null) {
        if (!isLevelEnabled(level)) return
        val builtMessage = if (message != null) buildString {
            appendColor(ConsoleColor.DARK_GRAY)
            append("[")
            appendColor(ConsoleColor.WHITE)
            append(CordImpl.formatter.format(System.currentTimeMillis()))
            appendColor(ConsoleColor.DARK_GRAY)
            append("] ")
            appendColor(ConsoleColor.GRAY)
            append(renderLevel(level))
            appendColor(ConsoleColor.DARK_GRAY)
            append(": ")
            appendColor(ConsoleColor.DEFAULT)
            if (shortLogName != "") {
                appendColor(ConsoleColor.DARK_GRAY)
                append("[")
                appendColor(ConsoleColor.WHITE)
                append(shortLogName)
                appendColor(ConsoleColor.DARK_GRAY)
                append("] ")
                appendColor(ConsoleColor.DEFAULT)
            }
            if (level >= LOG_LEVEL_WARN) appendColor(ConsoleColor.YELLOW)
            append(message)
            appendColor(ConsoleColor.DEFAULT)
        } else null
        if (builtMessage != null) console.forceWriteLine(builtMessage)
        throwable?.printStackTrace(stream(level))
    }

    fun StringBuilder.appendColor(color: ConsoleColor) {
        if (colored) append(color)
    }

    private fun renderLevel(level: Int): String {
        return renderLeveColor(level) + when (level) {
            LOG_LEVEL_TRACE -> "TRACE"
            LOG_LEVEL_DEBUG -> "DEBUG"
            LOG_LEVEL_INFO -> "INFO"
            LOG_LEVEL_COMMAND -> "COMMAND"
            LOG_LEVEL_WARN -> "WARNING"
            LOG_LEVEL_ERROR -> "ERROR"
            else -> throw IllegalStateException("Unrecognized level [$level]")
        }

    }

    private fun renderLeveColor(level: Int): String {
        if (!colored) return ""
        return when (level) {
            LOG_LEVEL_TRACE -> ConsoleColor.WHITE
            LOG_LEVEL_DEBUG -> ConsoleColor.WHITE
            LOG_LEVEL_INFO -> ConsoleColor.WHITE
            LOG_LEVEL_COMMAND -> ConsoleColor.WHITE
            LOG_LEVEL_WARN -> ConsoleColor.RED
            LOG_LEVEL_ERROR -> ConsoleColor.RED
            else -> throw IllegalStateException("Unrecognized level [$level]")
        }.toString()
    }

    private fun formatAndLog(level: Int, format: String, vararg args: Any) {
        val tp = MessageFormatter.arrayFormat(format, args)
        log(level, tp.message, tp.throwable)
    }

    override fun isCommandEnabled(): Boolean = isLevelEnabled(LOG_LEVEL_COMMAND)

    override fun command(msg: String) = log(LOG_LEVEL_COMMAND, msg)

    override fun command(format: String, arg: Any) = formatAndLog(LOG_LEVEL_COMMAND, format, arg)

    override fun command(format: String, arg1: Any, arg2: Any) = formatAndLog(LOG_LEVEL_COMMAND, format, arg1, arg2)

    override fun command(format: String, vararg arguments: Any) = formatAndLog(LOG_LEVEL_COMMAND, format, arguments)

    override fun command(msg: String, throwable: Throwable?) = log(LOG_LEVEL_COMMAND, msg, throwable)

    override fun isTraceEnabled(): Boolean = isLevelEnabled(LOG_LEVEL_TRACE)
    override fun trace(msg: String) = log(LOG_LEVEL_TRACE, msg)

    override fun trace(format: String, arg: Any) = formatAndLog(LOG_LEVEL_TRACE, format, arg)

    override fun trace(format: String, arg1: Any, arg2: Any) = formatAndLog(LOG_LEVEL_TRACE, format, arg1, arg2)

    override fun trace(format: String, vararg arguments: Any) = formatAndLog(LOG_LEVEL_TRACE, format, arguments)

    override fun trace(msg: String, throwable: Throwable?) = log(LOG_LEVEL_TRACE, msg, throwable)

    override fun isDebugEnabled(): Boolean = isLevelEnabled(LOG_LEVEL_DEBUG)
    override fun debug(msg: String) = log(LOG_LEVEL_DEBUG, msg)

    override fun debug(format: String, arg: Any) = formatAndLog(LOG_LEVEL_DEBUG, format, arg)

    override fun debug(format: String, arg1: Any, arg2: Any) = formatAndLog(LOG_LEVEL_DEBUG, format, arg1, arg2)

    override fun debug(format: String, vararg arguments: Any) = formatAndLog(LOG_LEVEL_DEBUG, format, arguments)

    override fun debug(msg: String, throwable: Throwable?) = log(LOG_LEVEL_DEBUG, msg, throwable)

    override fun isInfoEnabled(): Boolean = isLevelEnabled(LOG_LEVEL_INFO)

    override fun info(msg: String) = log(LOG_LEVEL_INFO, msg)

    override fun info(format: String, arg: Any) = formatAndLog(LOG_LEVEL_INFO, format, arg)

    override fun info(format: String, arg1: Any, arg2: Any) = formatAndLog(LOG_LEVEL_INFO, format, arg1, arg2)

    override fun info(format: String, vararg arguments: Any) = formatAndLog(LOG_LEVEL_INFO, format, arguments)

    override fun info(msg: String, throwable: Throwable?) = log(LOG_LEVEL_INFO, msg, throwable)

    override fun isWarnEnabled(): Boolean = isLevelEnabled(LOG_LEVEL_WARN)

    override fun warn(msg: String) = log(LOG_LEVEL_WARN, msg)

    override fun warn(format: String, arg: Any) = formatAndLog(LOG_LEVEL_WARN, format, arg)

    override fun warn(format: String, vararg arguments: Any) = formatAndLog(LOG_LEVEL_WARN, format, arguments)

    override fun warn(format: String, arg1: Any, arg2: Any) = formatAndLog(LOG_LEVEL_WARN, format, arg1, arg2)

    override fun warn(msg: String, throwable: Throwable?) = log(LOG_LEVEL_WARN, msg, throwable)

    override fun isErrorEnabled(): Boolean = isLevelEnabled(LOG_LEVEL_ERROR)

    override fun error(msg: String) = log(LOG_LEVEL_ERROR, msg)

    override fun error(format: String, arg: Any) = formatAndLog(LOG_LEVEL_ERROR, format, arg)

    override fun error(format: String, arg1: Any, arg2: Any) = formatAndLog(LOG_LEVEL_ERROR, format, arg1, arg2)

    override fun error(format: String, vararg arguments: Any) = formatAndLog(LOG_LEVEL_ERROR, format, arguments)

    override fun error(msg: String, throwable: Throwable?) = log(LOG_LEVEL_ERROR, msg, throwable)
}