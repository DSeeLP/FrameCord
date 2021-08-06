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

package io.github.dseelp.framecord.core.commands.console

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.arguments.StringArgument
import de.dseelp.kommon.command.literal
import io.github.dseelp.framecord.api.command.Command
import io.github.dseelp.framecord.api.command.CommandScope
import io.github.dseelp.framecord.api.command.Sender
import io.github.dseelp.framecord.api.plugins.PluginComponent
import io.github.dseelp.framecord.core.FakePlugin
import org.koin.core.component.inject

@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
object ReloadCommand : Command<Sender>, PluginComponent<FakePlugin> {
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.CONSOLE)
    override val description: String = "Reloads the given scope. Possible Scopes: plugins, settings, all"

    val cord: io.github.dseelp.framecord.api.Cord by inject()

    override val node: CommandNode<Sender> = literal("reload") {
        execute {
            sender.sendMessage("You need to provide a reload scope!")
        }
        argument(StringArgument("scope")) {
            map<String, Array<io.github.dseelp.framecord.api.ReloadScope>?>("scope") {
                runCatching { arrayOf(io.github.dseelp.framecord.api.ReloadScope.valueOf(it.uppercase())) }.getOrNull()
                    ?: if (it.lowercase() == "all") io.github.dseelp.framecord.api.ReloadScope.ALL else null
            }

            execute {
                val scopes: Array<io.github.dseelp.framecord.api.ReloadScope>? = get("scope")
                if (scopes == null) {
                    sender.sendMessage("You need to provide a reload scope!")
                    return@execute
                }
                cord.reload(*scopes)
            }
        }
    }

    @io.github.dseelp.framecord.api.InternalFrameCordApi
    override val plugin: FakePlugin
        get() = FakePlugin
}