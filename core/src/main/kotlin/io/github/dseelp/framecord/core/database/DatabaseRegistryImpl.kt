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

package io.github.dseelp.framecord.core.database

import com.zaxxer.hikari.HikariDataSource
import io.github.dseelp.framecord.api.database.*
import io.github.dseelp.framecord.api.event.EventHandle
import io.github.dseelp.framecord.api.event.Listener
import io.github.dseelp.framecord.api.events.PluginDisableEvent
import io.github.dseelp.framecord.api.events.PluginEventType
import io.github.dseelp.framecord.api.logging.logger
import io.github.dseelp.framecord.api.plugins.Plugin
import io.github.dseelp.framecord.core.CordBootstrap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager

@Listener
class DatabaseRegistryImpl : DatabaseRegistry {
    val log by logger<DatabaseRegistry>()
    val databases = mutableMapOf<Plugin, CordDatabase>()
    val databaseScopes = mutableMapOf<Plugin, DatabaseScope>()
    val mutex = Mutex()

    @EventHandle
    @Suppress("UNUSED")
    suspend fun onPluginDisable(event: PluginDisableEvent) {
        if (event.type != PluginEventType.POST) return
        if (!hasDatabase(event.plugin)) return
        unregister(event.plugin)
    }

    override suspend fun registerDatabase(plugin: Plugin, databaseInfo: DatabaseInfo): CordDatabase {
        /*if (checkClassLoader(plugin::class.java.classLoader) {
                //log.error("This method is not called from the plugin provided.")
            }) throw DatabaseConfigurationException("This method is not called from the plugin provided.")*/
        mutex.withLock {
            databaseInfo.config.poolName = plugin.name
            databaseInfo.config.validate()
            val dataSource = HikariDataSource(databaseInfo.config)
            val defaultDb = TransactionManager.defaultDatabase
            val cord = CordDatabase(dataSource, Database.connect(dataSource), databaseInfo)
            if (defaultDb != null) TransactionManager.defaultDatabase = defaultDb
            TransactionManager.defaultDatabase
            databases[plugin] = cord
            databaseScopes[plugin] = DatabaseScope(cord)
            return cord
        }
    }

    override fun get(plugin: Plugin): CordDatabase = databases[plugin]
        ?: throw NoDatabaseFoundException("There is no registered database for the plugin ${plugin.name}")

    override fun hasDatabase(plugin: Plugin): Boolean = databases.containsKey(plugin)

    private inline fun checkClassLoader(expectedLoader: ClassLoader, action: () -> Unit): Boolean =
        if (StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).callerClass.classLoader != expectedLoader) {
            action.invoke()
            true
        } else false

    override suspend fun unregister(plugin: Plugin) {
        if (checkClassLoader(CordBootstrap::class.java.classLoader) {
                log.error("Only the core can unregister a Database.")
            }) return
        mutex.withLock {
            if (!hasDatabase(plugin)) {
                log.error("There is no database defined for the plugin ${plugin.name}")
            }
            val database = get(plugin)
            if (!database.isClosed) database.close()
            databases.remove(plugin)
            databaseScopes.remove(plugin)
        }

    }

    override fun getScope(plugin: Plugin) = databaseScopes[plugin]
        ?: throw NoDatabaseFoundException("There is no registered database for the plugin ${plugin.name}")
}