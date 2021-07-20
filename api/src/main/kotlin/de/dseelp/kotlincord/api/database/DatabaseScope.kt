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

package de.dseelp.kotlincord.api.database

import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@Suppress("UNUSED")
class DatabaseScope(val cordDatabase: CordDatabase) {
    val database: Database
        get() = cordDatabase.exposed


    fun <T> transaction(statement: Transaction.() -> T) =
        org.jetbrains.exposed.sql.transactions.transaction(database, statement)

    suspend fun <T> suspendedTransaction(context: CoroutineDispatcher? = null, statement: suspend Transaction.() -> T) =
        newSuspendedTransaction(context, database, statement = statement)


    suspend fun <T> suspendedTransactionAsync(
        context: CoroutineDispatcher? = null,
        statement: suspend Transaction.() -> T
    ) = org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync(
        context,
        database,
        statement = statement
    )
}