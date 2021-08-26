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
import io.github.dseelp.framecord.api.InternalFrameCordApi
import io.github.dseelp.framecord.api.command.CommandScope
import io.github.dseelp.framecord.api.command.CommandScope.*
import io.github.dseelp.framecord.api.command.Sender
import io.github.dseelp.framecord.api.event.EventHandle
import io.github.dseelp.framecord.api.event.Listener
import io.github.dseelp.framecord.api.events.PluginDisableEvent
import io.github.dseelp.framecord.api.modules.FeatureRestricted
import io.github.dseelp.framecord.api.modules.checkBoolean
import io.github.dseelp.framecord.api.plugins.Plugin
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier

@Listener
@InternalFrameCordApi
object Commands : CordKoinComponent {
    internal val guild: CommandDispatcher<Sender> by inject(qualifier("guild"))
    internal val private: CommandDispatcher<Sender> by inject(qualifier("private"))
    internal val console: CommandDispatcher<Sender> by inject(qualifier("console"))
    internal val thread: CommandDispatcher<Sender> by inject(qualifier("thread"))

    internal val pluginCommands = hashMapOf<Plugin, MutableList<CommandHolder>>()

    fun getDescription(scope: CommandScope, commandName: String, guildId: Long? = null): String? =
        getCommandHolder(scope, commandName, guildId)?.description

    @InternalFrameCordApi
    fun getCommandHolder(scope: CommandScope, commandName: String, guildId: Long? = null): CommandHolder? {
        return pluginCommands.values.mapNotNull { entry ->
            entry.firstOrNull {
                it.scopes.contains(scope) && it.name.lowercase() == commandName.lowercase() && if (guildId == null) true else it.featureRestricted?.checkBoolean(
                    guildId
                ) ?: true
            }
        }
            .firstOrNull()
    }

    fun getCommandsForScope(scope: CommandScope, guildId: Long? = null): Map<Plugin, List<CommandHolder>> {
        return pluginCommands.map { entry -> entry.key to entry.value.filter { it.scopes.contains(scope) } }
            .filterNot { it.second.isEmpty() }.associate {
                it.first to if (guildId == null) it.second else it.second.filter { holder ->
                    holder.featureRestricted?.checkBoolean(guildId) ?: true
                }
            }
    }

    fun unregister(plugin: Plugin) {
        val cmds = pluginCommands[plugin] ?: return
        pluginCommands.remove(plugin)
        for (holder in cmds) {
            val node = holder.node
            for (scope in holder.scopes) {
                when (scope) {
                    GUILD -> guild.unregister(node)
                    PRIVATE -> private.unregister(node)
                    CONSOLE -> console.unregister(node)
                    THREAD -> thread.unregister(node)
                }
            }
        }
    }

    @EventHandle
    fun onPluginDisable(event: PluginDisableEvent) {
        unregister(event.plugin)
    }
}

@OptIn(InternalFrameCordApi::class)
fun Plugin.register(
    node: CommandNode<out Sender>,
    description: String = "",
    scopes: Array<CommandScope>,
    featureRestricted: FeatureRestricted? = null
) {
    if (node.name == null) throw IllegalArgumentException("A command node registered as a command must have a name!")
    for (scope in scopes) {
        when (scope) {
            GUILD -> Commands.guild.register(node)
            PRIVATE -> Commands.private.register(node)
            CONSOLE -> Commands.console.register(node)
            THREAD -> Commands.thread.register(node)
        }
    }
    Commands.pluginCommands.getOrPut(this) { mutableListOf() }
        .add(CommandHolder(this, scopes, description, node.name!!, node, featureRestricted))
}


fun <S : Any> literal(
    name: String,
    aliases: Array<String> = arrayOf(),
    block: CommandBuilder<S>.() -> Unit
): CommandNode<S> =
    CommandBuilder<S>(name, aliases).apply(block).build()
