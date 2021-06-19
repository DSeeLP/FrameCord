/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.logging

import de.dseelp.kotlincord.api.plugins.Plugin
import org.slf4j.LoggerFactory

object LogManager {
    const val CORE = "Core"
    const val ROOT = ""
}

fun logger(name: String): Lazy<KLogger> = lazy {
    LoggerFactory.getLogger(name) as KLogger
}

inline fun <reified T : Any> logger(): Lazy<KLogger> = lazy {
    LoggerFactory.getLogger(T::class.simpleName) as KLogger
}

fun Plugin.logger(): Lazy<KLogger> = lazy {
    LoggerFactory.getLogger(meta.name) as KLogger
}