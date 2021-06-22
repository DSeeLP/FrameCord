/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package org.slf4j.impl

import de.dseelp.kotlincord.api.logging.KLogger
import org.slf4j.ILoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object CordLoggerFactory : ILoggerFactory {

    val loggerMap: ConcurrentMap<String, KLogger> = ConcurrentHashMap()

    init {
        //SimpleLogger.lazyInit()
    }

    fun reset() {
        loggerMap.clear()
    }

    override fun getLogger(name: String): KLogger {
        val cordLogger = loggerMap[name]
        return if (cordLogger != null) {
            cordLogger
        } else {
            val newInstance: KLogger = CordLogger(name)
            val oldInstance = loggerMap.putIfAbsent(name, newInstance)
            oldInstance ?: newInstance
        }
    }
}