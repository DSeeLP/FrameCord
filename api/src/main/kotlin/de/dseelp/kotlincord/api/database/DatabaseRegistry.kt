/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.database

import de.dseelp.kotlincord.api.plugins.Plugin

interface DatabaseRegistry {
    suspend fun registerDatabase(plugin: Plugin, databaseInfo: DatabaseInfo): CordDatabase

    operator fun get(plugin: Plugin): CordDatabase

    fun hasDatabase(plugin: Plugin): Boolean

    suspend fun unregister(plugin: Plugin)
}