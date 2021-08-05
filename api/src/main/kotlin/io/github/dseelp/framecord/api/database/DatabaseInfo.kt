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

package io.github.dseelp.framecord.api.database

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
            return DatabaseInfo(type, config)
        }

        fun sqlite(path: Path) = sqlite(path.toAbsolutePath().toUri().toString())
        fun sqliteInMemory() = sqlite(":memory:")

        private fun sqlite(pathString: String): DatabaseInfo {
            val config = HikariConfig()
            config.dataSourceClassName = DatabaseType.SQLITE.dataSourceClass
            config["url"] = "jdbc:sqlite:$pathString"
            return DatabaseInfo(DatabaseType.SQLITE, config)
        }

        operator fun HikariConfig.set(propertyName: String, value: Any) = addDataSourceProperty(propertyName, value)
        operator fun HikariConfig.get(propertyName: String): String = dataSourceProperties.getProperty(propertyName)
    }
}