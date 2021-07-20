/*
 * Copyright (c) 2021 KotlinCord team & contributors
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