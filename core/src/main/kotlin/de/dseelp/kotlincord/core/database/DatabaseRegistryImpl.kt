/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.database

import com.zaxxer.hikari.HikariDataSource
import de.dseelp.kotlincord.api.database.*
import de.dseelp.kotlincord.api.event.EventHandle
import de.dseelp.kotlincord.api.event.Listener
import de.dseelp.kotlincord.api.events.PluginDisableEvent
import de.dseelp.kotlincord.api.events.PluginEventType
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.core.CordBootstrap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.sql.Database

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
        if (checkClassLoader(plugin::class.java.classLoader) {
                //log.error("This method is not called from the plugin provided.")
            }) throw DatabaseConfigurationException("This method is not called from the plugin provided.")
        mutex.withLock {
            databaseInfo.config.poolName = plugin.name
            databaseInfo.config.validate()
            val dataSource = HikariDataSource(databaseInfo.config)
            val cord = CordDatabase(dataSource, Database.connect(dataSource), databaseInfo)
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