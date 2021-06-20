/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api

import de.dseelp.kotlincord.api.plugins.Plugin

interface Cord {
    fun reload(vararg scopes: ReloadScope)

    fun shutdown()

    @InternalKotlinCordApi
    fun shutdown(unloadPlugins: Boolean)

    fun getPlugin(): Plugin
}