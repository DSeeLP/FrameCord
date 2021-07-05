/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api

enum class ReloadScope {
    PLUGINS,
    SETTINGS;

    companion object {
        val ALL = values()
    }
}