package de.dseelp.kotlincord.api.events

import de.dseelp.kotlincord.api.plugins.Plugin

data class PluginDisableEvent(val plugin: Plugin, val type: PluginEventType)
data class PluginEnableEvent(val plugin: Plugin, val type: PluginEventType)

enum class PluginEventType {
    PRE,
    POST
}
