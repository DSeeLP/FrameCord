/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.utils

import de.dseelp.kommon.command.CommandBuilder
import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.CommandNode
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.command.Sender
import de.dseelp.kotlincord.api.event.EventHandle
import de.dseelp.kotlincord.api.event.Listener
import de.dseelp.kotlincord.api.events.PluginDisableEvent
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.utils.CommandScope.*
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier

@Listener
@InternalKotlinCordApi
object Commands : CordKoinComponent {
    internal val guild: CommandDispatcher<Sender> by inject(qualifier("guild"))
    internal val private: CommandDispatcher<Sender> by inject(qualifier("private"))
    internal val console: CommandDispatcher<Sender> by inject(qualifier("console"))

    val pluginCommands = hashMapOf<Plugin, MutableList<Pair<CommandScope, CommandNode<out Sender>>>>()

    fun unregister(plugin: Plugin) {
        for (pair in pluginCommands[plugin]!!) {
            when (pair.first) {
                GUILD -> guild.unregister(pair.second)
                PRIVATE -> private.unregister(pair.second)
                CONSOLE -> console.unregister(pair.second)
            }
        }
    }

    @EventHandle
    fun onPluginDisable(event: PluginDisableEvent) {
        unregister(event.plugin)
    }
}

@JvmName("registervArray")
fun Plugin.register(node: CommandNode<Sender>, scopes: Array<CommandScope>) = register(node, *scopes)

@OptIn(InternalKotlinCordApi::class)
fun Plugin.register(node: CommandNode<out Sender>, vararg scopes: CommandScope) {
    for (scope in scopes) {
        when (scope) {
            GUILD -> Commands.guild.register(node)
            PRIVATE -> Commands.private.register(node)
            CONSOLE -> Commands.console.register(node)
        }
        Commands.pluginCommands.getOrPut(this) { mutableListOf() }.add(scope to node)
    }
}


enum class CommandScope {
    GUILD,
    PRIVATE,
    CONSOLE;

    companion object {
        val ALL = values()
    }
}

fun <S : Any> literal(name: String, aliases: Array<String>, block: CommandBuilder<S>.() -> Unit): CommandNode<S> =
    CommandBuilder<S>(name, aliases).apply(block).build()
