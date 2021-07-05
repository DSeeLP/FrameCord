/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.commands.console

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.arguments.StringArgument
import de.dseelp.kommon.command.literal
import de.dseelp.kotlincord.api.Cord
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.ReloadScope
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.command.Sender
import de.dseelp.kotlincord.api.plugins.PluginComponent
import de.dseelp.kotlincord.api.utils.CommandScope
import de.dseelp.kotlincord.core.FakePlugin
import org.koin.core.component.inject

@OptIn(InternalKotlinCordApi::class)
object ReloadCommand : Command<Sender>, PluginComponent<FakePlugin> {
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.CONSOLE)

    val cord: Cord by inject()

    override val node: CommandNode<Sender> = literal("reload") {
        execute {
            sender.sendMessage("You need to provide a reload scope!")
        }
        argument(StringArgument("scope")) {
            map<String, Array<ReloadScope>?>("scope") {
                runCatching { arrayOf(ReloadScope.valueOf(it.uppercase())) }.getOrNull()
                    ?: if (it.lowercase() == "all") ReloadScope.ALL else null
            }

            execute {
                val scopes: Array<ReloadScope>? = get("scope")
                if (scopes == null) {
                    sender.sendMessage("You need to provide a reload scope!")
                    return@execute
                }
                cord.reload(*scopes)
            }
        }
    }

    @InternalKotlinCordApi
    override val plugin: FakePlugin
        get() = FakePlugin
}