/*
 * Copyright (c) 2021 DSeeLP & FrameCord contributors
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

package io.github.dseelp.framecord.api.utils

import de.dseelp.kommon.command.CommandBuilder
import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.CommandNode
import io.github.dseelp.framecord.api.command.CommandScope
import io.github.dseelp.framecord.api.command.CommandScope.*
import io.github.dseelp.framecord.api.command.Sender
import io.github.dseelp.framecord.api.event.EventHandle
import io.github.dseelp.framecord.api.event.Listener
import io.github.dseelp.framecord.api.events.PluginDisableEvent
import io.github.dseelp.framecord.api.plugins.Plugin
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier

@Listener
@io.github.dseelp.framecord.api.InternalFrameCordApi
object Commands : CordKoinComponent {
    internal val guild: CommandDispatcher<Sender> by inject(qualifier("guild"))
    internal val private: CommandDispatcher<Sender> by inject(qualifier("private"))
    internal val console: CommandDispatcher<Sender> by inject(qualifier("console"))
    internal val thread: CommandDispatcher<Sender> by inject(qualifier("thread"))

    internal val pluginCommands =
        hashMapOf<Plugin, MutableList<Triple<CommandScope, String, CommandNode<out Sender>>>>()

    fun getDescription(scope: CommandScope, commandName: String): String? =
        pluginCommands.values.mapNotNull { entry -> entry.firstOrNull { it.first == scope && it.third.name == commandName } }
            .firstOrNull()?.second

    fun getCommandsForScope(scope: CommandScope): Map<Plugin, List<Triple<CommandScope, String, CommandNode<out Sender>>>> =
        pluginCommands.map { entry -> entry.key to entry.value.filter { it.first == scope } }
            .filterNot { it.second.isEmpty() }.toMap()

    fun unregister(plugin: Plugin) {
        val cmds = pluginCommands[plugin] ?: return
        for (pair in cmds) {
            when (pair.first) {
                GUILD -> guild.unregister(pair.third)
                PRIVATE -> private.unregister(pair.third)
                CONSOLE -> console.unregister(pair.third)
                THREAD -> thread.unregister(pair.third)
            }
        }
    }

    @EventHandle
    fun onPluginDisable(event: PluginDisableEvent) {
        unregister(event.plugin)
    }
}

@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
fun Plugin.register(node: CommandNode<out Sender>, description: String = "", scopes: Array<CommandScope>) {
    for (scope in scopes) {
        when (scope) {
            GUILD -> Commands.guild.register(node)
            PRIVATE -> Commands.private.register(node)
            CONSOLE -> Commands.console.register(node)
            THREAD -> Commands.thread.register(node)
        }
        Commands.pluginCommands.getOrPut(this) { mutableListOf() }.add(Triple(scope, description, node))
    }
}


fun <S : Any> literal(
    name: String,
    aliases: Array<String> = arrayOf(),
    block: CommandBuilder<S>.() -> Unit
): CommandNode<S> =
    CommandBuilder<S>(name, aliases).apply(block).build()
