/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins

annotation class PluginAction(val action: Action) {
    enum class Action {
        LOAD,
        ENABLE,
        DISABLE
    }
}
