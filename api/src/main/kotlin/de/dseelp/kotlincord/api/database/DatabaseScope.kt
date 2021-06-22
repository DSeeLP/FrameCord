/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.database

import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction

@Suppress("UNUSED")
class DatabaseScope(val cordDatabase: CordDatabase) {
    val database: Database
        get() = cordDatabase.exposed


    fun <T> transaction(statement: Transaction.() -> T) =
        org.jetbrains.exposed.sql.transactions.transaction(database, statement)

    suspend fun <T> suspendedTransactionAsync(
        context: CoroutineDispatcher? = null,
        statement: suspend Transaction.() -> T
    ) = org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync(
        context,
        database,
        statement = statement
    )
}