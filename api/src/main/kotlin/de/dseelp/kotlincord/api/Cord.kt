/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api

import de.dseelp.kotlincord.api.plugins.Plugin

interface Cord {
    suspend fun reload(vararg scopes: ReloadScope)

    suspend fun shutdown()

    @InternalKotlinCordApi
    suspend fun shutdown(unloadPlugins: Boolean)

    fun getPlugin(): Plugin

    suspend fun reloadPlugins()
}