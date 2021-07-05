/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.database

import com.zaxxer.hikari.HikariConfig
import java.nio.file.Path

data class DatabaseInfo(val type: DatabaseType, val config: HikariConfig) {
    companion object {
        fun mariadb(
            username: String,
            password: String,
            databaseName: String,
            host: String = "127.0.0.1",
            port: Int = 3306
        ) = mariadbOrMySQL(DatabaseType.MARIADB, username, password, databaseName, host, port)

        private fun mariadbOrMySQL(
            type: DatabaseType,
            username: String,
            password: String,
            databaseName: String,
            host: String = "127.0.0.1",
            port: Int = 3306
        ): DatabaseInfo {
            val config = HikariConfig()
            config.dataSourceClassName = type.dataSourceClass
            config.username = username
            config.password = password
            config["serverName"] = host
            config["port"] = port
            config["databaseName"] = databaseName
            config["cachePrepStmts"] = true
            config["prepStmtCacheSize"] = 250
            config["prepStmtCacheSqlLimit"] = 2048
            config["useServerPrepStmts"] = true
            config["useLocalSessionState"] = true
            config["rewriteBatchedStatements"] = true
            config["cacheResultSetMetadata"] = true
            config["cacheServerConfiguration"] = true
            config["elideSetAutoCommits"] = true
            config["maintainTimeStats"] = false
            return DatabaseInfo(type, config)
        }

        fun sqlite(path: Path) = sqlite(path.toAbsolutePath().toUri().toString())
        fun sqliteInMemory() = sqlite(":memory:")

        private fun sqlite(pathString: String): DatabaseInfo {
            val config = HikariConfig()
            config.dataSourceClassName = DatabaseType.SQLITE.dataSourceClass
            //config.driverClassName = "org.sqlite.JDBC"
            config["url"] = "jdbc:sqlite:$pathString"
            return DatabaseInfo(DatabaseType.SQLITE, config)
        }

        operator fun HikariConfig.set(propertyName: String, value: Any) = addDataSourceProperty(propertyName, value)
        operator fun HikariConfig.get(propertyName: String): String = dataSourceProperties.getProperty(propertyName)
    }
}