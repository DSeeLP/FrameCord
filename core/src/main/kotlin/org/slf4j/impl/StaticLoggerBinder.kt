/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package org.slf4j.impl

import org.slf4j.ILoggerFactory
import org.slf4j.spi.LoggerFactoryBinder

class StaticLoggerBinder private constructor() : LoggerFactoryBinder {
    override fun getLoggerFactory(): ILoggerFactory = CordLoggerFactory

    override fun getLoggerFactoryClassStr(): String = loggerFactoryClassStrConst

    companion object {
        @JvmStatic
        @get:JvmName("getSingleton")
        val singleton = StaticLoggerBinder()

        @JvmField
        val REQUESTED_API_VERSION = "1.6.99"
        private val loggerFactoryClassStrConst = CordLoggerFactory::class.java.name
    }
}