/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.database

enum class DatabaseType(val dataSourceClass: String) {
    MARIADB("org.mariadb.jdbc.MariaDbDataSource"),
    SQLITE("org.sqlite.SQLiteDataSource"),
}