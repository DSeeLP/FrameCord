package de.dseelp.kotlincord.api.plugins

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.json.toJson
import de.dseelp.kotlincord.api.configs.file
import de.dseelp.kotlincord.api.configs.toFile
import de.dseelp.kotlincord.api.database.DatabaseInfo
import de.dseelp.kotlincord.api.database.DatabaseType
import de.dseelp.kotlincord.api.database.DatabaseType.MARIADB
import de.dseelp.kotlincord.api.database.DatabaseType.SQLITE
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
