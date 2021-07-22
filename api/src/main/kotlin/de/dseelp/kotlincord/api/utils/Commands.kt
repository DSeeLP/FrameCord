/*
 * Copyright (c) 2021 DSeeLP & KotlinCord contributors
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

fun <S : Any> literal(
    name: String,
    aliases: Array<String> = arrayOf(),
    block: CommandBuilder<S>.() -> Unit
): CommandNode<S> =
    CommandBuilder<S>(name, aliases).apply(block).build()
