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

package io.github.dseelp.framecord.api.plugins

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.json.toJson
import io.github.dseelp.framecord.api.configs.file
import io.github.dseelp.framecord.api.configs.toFile
import io.github.dseelp.framecord.api.database.DatabaseInfo
import io.github.dseelp.framecord.api.database.DatabaseType
import io.github.dseelp.framecord.api.database.DatabaseType.MARIADB
import io.github.dseelp.framecord.api.database.DatabaseType.SQLITE
import java.nio.file.Path
import kotlin.io.path.div

data class DatabaseConfig(
    val type: DatabaseType,
    val username: String,
    val password: String,
    val databaseName: String,
    val host: String = "127.0.0.1",
    val port: Int = 3306
) {

    class Spec(default: DatabaseConfig) : ConfigSpec("") {
        val type by optional(default.type)
        val username by optional(default.username)
        val password by optional(default.password)
        val databaseName by optional(default.databaseName)
        val host by optional(default.host)
        val port by optional(default.port)
    }

    companion object {
        fun defaultDatabaseConfig(plugin: Plugin) = DatabaseConfig(SQLITE, "user", "password", plugin.name)

        fun load(
            plugin: Plugin,
            default: DatabaseConfig = defaultDatabaseConfig(plugin),
            configPath: Path = plugin.dataFolder / "database.json",
        ): DatabaseConfig {
            val spec = Spec(default)
            val cfg = Config { addSpec(spec) }.from.json.file(configPath, false)
            cfg.toJson.toFile(configPath)
            return DatabaseConfig(
                cfg[spec.type],
                cfg[spec.username],
                cfg[spec.password],
                cfg[spec.databaseName],
                cfg[spec.host],
                cfg[spec.port]
            )
        }
    }

    fun toDatabaseInfo(plugin: Plugin): DatabaseInfo = when (type) {
        MARIADB -> DatabaseInfo.mariadb(username, password, databaseName, host, port)
        SQLITE -> DatabaseInfo.sqlite(plugin.dataFolder / "${databaseName}.db")
    }
}
