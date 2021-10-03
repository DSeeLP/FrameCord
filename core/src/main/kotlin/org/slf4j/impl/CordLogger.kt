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

package org.slf4j.impl

import com.log4k.*
import io.github.dseelp.framecord.api.configs.BotConfig
import io.github.dseelp.framecord.api.logging.CommandLogLevel
import io.github.dseelp.framecord.api.logging.KLogger
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import org.slf4j.helpers.MarkerIgnoringBase
import org.slf4j.helpers.MessageFormatter
import org.slf4j.spi.LocationAwareLogger

@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
class CordLogger(name: String) : MarkerIgnoringBase(), KLogger, CordKoinComponent {
    private val LOG_LEVEL_TRACE = LocationAwareLogger.TRACE_INT
    private val LOG_LEVEL_DEBUG = LocationAwareLogger.DEBUG_INT
    private val LOG_LEVEL_INFO = LocationAwareLogger.INFO_INT
    private val LOG_LEVEL_COMMAND = LocationAwareLogger.INFO_INT + 5
    private val LOG_LEVEL_WARN = LocationAwareLogger.WARN_INT
    private val LOG_LEVEL_ERROR = LocationAwareLogger.ERROR_INT

    val cName: String = name

    var currentLogLevel = LOG_LEVEL_INFO
        private set

    fun isLevelEnabled(logLevel: Int): Boolean {
        //currentLogLevel = LOG_LEVEL_INFO
        currentLogLevel = when (kotlin.runCatching { getKoin().get<BotConfig>() }.getOrNull()?.logging?.defaultLevel) {
            Level.Verbose -> LOG_LEVEL_TRACE
            Level.Debug -> LOG_LEVEL_DEBUG
            Level.Info -> LOG_LEVEL_INFO
            CommandLogLevel -> LOG_LEVEL_COMMAND
            Level.Warn -> LOG_LEVEL_WARN
            Level.Error -> LOG_LEVEL_ERROR
            Level.Assert -> LOG_LEVEL_ERROR+10
            else -> LOG_LEVEL_INFO
        }
        return logLevel >= currentLogLevel
    }

    val showErrors: Boolean
        get() = currentLogLevel == LOG_LEVEL_DEBUG || System.getProperty("showErrors").toBoolean()

    private val shortLogName = name?.substring(name.lastIndexOf(".") + 1)

    val unknownLevel = object : Level(-1F) {}

    fun log(levelInt: Int, message: String, throwable: Throwable? = null) {
        val level = when(levelInt) {
            LOG_LEVEL_TRACE -> Level.Verbose
            LOG_LEVEL_DEBUG -> Level.Debug
            LOG_LEVEL_INFO -> Level.Info
            LOG_LEVEL_WARN -> Level.Warn
            LOG_LEVEL_ERROR -> Level.Error
            else -> unknownLevel
        }
        val event = if (throwable == null) SimpleEvent(message) else SimpleThrowableEvent(message, throwable)
        Log4k.log(level, Config(tag = cName), event)
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