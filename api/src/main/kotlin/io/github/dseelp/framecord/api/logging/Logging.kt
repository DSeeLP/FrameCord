package io.github.dseelp.framecord.api.logging

import com.log4k.*
import com.log4k.Level.Verbose

private val cmdLevel = object : Level(2.1F) {}

val CommandLogLevel = cmdLevel


/**
 * Send an [CommandLogLevel] log [message].
 */
inline fun <reified T : Any> T.cmd(
    message: String,
    config: Config = configuration(),
    log4k: Log4kI = Log4k
) =
    log4k.log(CommandLogLevel, config, SimpleEvent(message))

/**
 * Send a [CommandLogLevel] log [String].
 *
 * Example of use:
 * ```
 *    "This is the message".(cmd())()
 * ```
 */
inline fun <reified T : Any> T.cmd(
    config: Config = configuration(T::class),
    log4k: Log4kI = Log4k
): String.() -> Unit = {
    log4k.log(CommandLogLevel, config, SimpleEvent(this))
}

/**
 * Send a [CommandLogLevel] log [String].
 *
 * Example of use:
 * ```
 *    "This is the message".cmd<T>()
 * ```
 */
inline fun <reified T : Any> String.cmd(
    config: Config = configuration(T::class),
    log4k: Log4kI = Log4k
) {
    log4k.log(CommandLogLevel, config, SimpleEvent(this))
}

/**
 * Send a [CommandLogLevel] log [message] and [throwable].
 */
inline fun <reified T : Any> T.cmd(
    message: String, throwable: Throwable,
    config: Config = configuration(),
    log4k: Log4kI = Log4k
) =
    log4k.log(CommandLogLevel, config, SimpleThrowableEvent(message, throwable))

/**
 * Send a [Verbose] log [String] and [Throwable].
 *
 * Example of use:
 * ```
 *    "This is the message".(cmd(error))()
 * ```
 */
inline fun <reified T : Any> T.cmd(
    throwable: Throwable,
    config: Config = configuration(T::class),
    log4k: Log4kI = Log4k
): String.() -> Unit = {
    log4k.log(CommandLogLevel, config, SimpleThrowableEvent(this, throwable))
}

/**
 * Send a [CommandLogLevel] log [message] and [Throwable].
 *
 * Example of use:
 * ```
 *    error.cmd<T>("This is the message")
 * ```
 */
inline fun <reified T : Any> Throwable.cmd(
    message: String,
    config: Config = configuration(T::class),
    log4k: Log4kI = Log4k
) {
    log4k.log(CommandLogLevel, config, SimpleThrowableEvent(message, this))
}

/**
 * Send a [CommandLogLevel] log [event].
 */
inline fun <reified T : Any> T.cmd(
    event: Event,
    config: Config = configuration(),
    log4k: Log4kI = Log4k
) =
    log4k.log(CommandLogLevel, config, event)

/**
 * Send a [CommandLogLevel] log [Event].
 *
 * Example of use:
 * ```
 *    EventImpl().cmd<T>()
 * ```
 */
inline fun <reified T : Any> Event.cmd(
    config: Config = configuration(T::class),
    log4k: Log4kI = Log4k
) {
    log4k.log(CommandLogLevel, config, this)
}