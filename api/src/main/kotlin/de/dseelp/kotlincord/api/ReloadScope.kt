/*
 * Created by Dirk on 19.6.2021.
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