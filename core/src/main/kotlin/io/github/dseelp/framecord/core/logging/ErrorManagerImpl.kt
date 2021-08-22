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

package io.github.dseelp.framecord.core.logging

import io.github.dseelp.framecord.api.logging.ErrorManager
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object ErrorManagerImpl : ErrorManager {
    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(DbErrors, DbSuppressedErrors)
        }
    }

    override fun dispatch(throwable: Throwable): UUID? {
        try {
            val className = throwable::class.qualifiedName ?: return null
            return transaction {
                val calcTime = System.currentTimeMillis()
                val err = DbError.new {
                    errorClass = className
                    message = throwable.message
                    stackTrace = throwable.stackTraceToString()
                    time = calcTime
                }
                throwable.cause?.let { throwable.addSuppressed(it) }
                throwable.suppressedExceptions.forEach {
                    DbSuppressedError.new {
                        error = err
                        errorClass = it::class.qualifiedName!!
                        stackTrace = it.stackTraceToString()
                        message = it.message
                        time = calcTime
                    }
                }
                commit()
                return@transaction err.id.value
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            return null
        }
    }

}