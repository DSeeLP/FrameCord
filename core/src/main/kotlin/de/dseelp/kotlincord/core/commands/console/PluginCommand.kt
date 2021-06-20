/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
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