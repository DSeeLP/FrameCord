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

package de.dseelp.kotlincord.core.commands.console

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.arguments.StringArgument
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.PathQualifiers
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.command.ConsoleSender
import de.dseelp.kotlincord.api.plugins.PluginData
import de.dseelp.kotlincord.api.plugins.PluginLoader
import de.dseelp.kotlincord.api.plugins.PluginManager
import de.dseelp.kotlincord.api.utils.CommandScope
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.api.utils.literal
import org.koin.core.component.inject
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.exists

@OptIn(InternalKotlinCordApi::class)
object PluginCommand : Command<ConsoleSender>, CordKoinComponent {
    val loader: PluginLoader by inject()
    val manager: PluginManager by inject()
    override val node: CommandNode<ConsoleSender> = literal("plugins", arrayOf("plugin")) {
        literal("list") {
            execute {
                for (data in loader.loadedPlugins) {
                    val meta = data.meta ?: continue
                    sender.sendMessage(
                        """
                        ${meta.name}
                            Description: ${meta.description}
                            Version: ${meta.version}
                            Authors: ${meta.authors.joinToString(", ")}
                    """.trimIndent()
                    )
                }
            }
        }

        literal("unload") {
            execute {
                sender.sendMessage("You need to provide the name of the plugin you want to unload.")
            }


            argument(StringArgument("plugin")) {
                map<String, PluginData?>("plugin") { pName ->
                    loader.loadedPlugins.firstOrNull { it.meta?.name?.lowercase() == pName.lowercase() }
                }
                execute {
                    val pluginData: PluginData? = get("plugin")
                    if (pluginData == null) {
                        sender.sendMessage("There is no plugin called ${get<String>("plugin")}")
                        return@execute
                    }
                    manager.unload(pluginData)
                    sender.sendMessage("The plugin was unloaded")
                }
            }
        }
        literal("load") {
            execute {
                sender.sendMessage("You need to provide the name of the plugin you want to load.")
            }

            argument(StringArgument("file")) {
                map<String, Path>("file") { name ->
                    PathQualifiers.PLUGIN_LOCATION / name
                }
                execute {
                    val path: Path = get("file")
                    if (!path.exists()) {
                        sender.sendMessage("The path provided doesn't exist!")
                        return@execute
                    }
                    val data = manager.load(path)
                    manager.enable(data.plugin!!)
                    sender.sendMessage("The plugin was loaded")
                }
            }
        }
    }
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.CONSOLE)
}