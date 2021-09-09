package io.github.dseelp.framecord.api.placeholders

import io.github.dseelp.framecord.api.plugins.Plugin

object PlaceholderManager {
    private val placeholders = mutableMapOf<Plugin, MutableList<Placeholder>>()
    fun registerPlaceholder(plugin: Plugin, placeholder: Placeholder) {
        val list = placeholders[plugin] ?: mutableListOf()
        list.add(placeholder)
        placeholders[plugin] = list
    }

    fun registerPlaceholder(plugin: Plugin, name: String, type: PlaceholderType, factory: (PlaceholderContext) -> Any?) = registerPlaceholder(plugin, Placeholder(type, name, factory))

    fun unregisterPlaceholders(plugin: Plugin) {
        placeholders.remove(plugin)
    }

    fun getPlaceholders(plugin: Plugin, type: PlaceholderType? = null): Array<Placeholder> {
        val list = placeholders.getOrDefault(plugin, emptyList())
        return if (type == null) list.toTypedArray()
        else list.filter { it.type == type }.toTypedArray()
    }

    fun getPlaceholders(type: PlaceholderType? = null): Array<Placeholder> {
        val merged = mutableListOf<Placeholder>()
        placeholders.values.forEach {
            merged.addAll(it)
        }
        return if (type == null) merged.distinctBy { it.name.lowercase() }.toTypedArray()
        else merged.filter { it.type == type }.distinctBy { it.name.lowercase() }.toTypedArray()
    }
    fun replacePlaceholders(arguments: Map<String, Any>, type: PlaceholderType, message: String): String {
        val context = PlaceholderContext(arguments)
        val placeholders = getPlaceholders(type)
        if (placeholders.isEmpty()) return message
        var msg = message
        for (placeholder in placeholders) {
            val data = placeholder.factory(context) ?: continue
            msg = msg.replace("%${placeholder.name}%", data.toString(), ignoreCase = true)
        }
        return msg
    }
}