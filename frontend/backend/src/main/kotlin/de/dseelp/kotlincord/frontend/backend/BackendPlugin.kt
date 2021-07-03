package de.dseelp.kotlincord.frontend.backend

import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.PluginAction
import de.dseelp.kotlincord.api.plugins.PluginAction.Action
import de.dseelp.kotlincord.api.utils.koin.registerKoinModules
import org.koin.dsl.module

object BackendPlugin: Plugin() {

    @PluginAction(Action.ENABLE)
    fun enable() {
        registerKoinModules(module {
            single { BackendConfig.load() }
        })
    }

    @PluginAction(Action.DISABLE)
    fun disable() {

    }

}