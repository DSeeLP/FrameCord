/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.logging

import org.slf4j.Logger

interface KLogger : Logger {
    fun isCommandEnabled(): Boolean
    fun command(msg: String)

    fun command(format: String, arg: Any)

    fun command(format: String, arg1: Any, arg2: Any)

    fun command(format: String, vararg arguments: Any)

    fun command(msg: String, throwable: Throwable?)
}