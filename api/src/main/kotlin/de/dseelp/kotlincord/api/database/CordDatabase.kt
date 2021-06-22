/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.database

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager

class CordDatabase(
    val datasource: HikariDataSource,
    val exposed: Database,
    val databaseInfo: DatabaseInfo,
    val job: Job = SupervisorJob(),
    val coroutineScope: CoroutineScope = CoroutineScope(job + Dispatchers.IO)
) {
    var isClosed: Boolean = false
        private set

    suspend fun close() {
        if (isClosed) return
        TransactionManager.closeAndUnregister(exposed)
        datasource.close()
        job.cancelAndJoin()
        coroutineScope.cancel()
        isClosed = true
    }
}