package de.dseelp.kotlincord.frontend.backend

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.plugins.PluginComponent

interface BackendComponent: PluginComponent<BackendPlugin> {
    @InternalKotlinCordApi
    override val plugin: BackendPlugin
        get() = BackendPlugin
}