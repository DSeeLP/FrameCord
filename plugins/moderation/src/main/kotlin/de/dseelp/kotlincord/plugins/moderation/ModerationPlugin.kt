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

package de.dseelp.kotlincord.plugins.moderation

import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.PluginAction
import de.dseelp.kotlincord.api.plugins.PluginInfo
import de.dseelp.kotlincord.plugins.moderation.commands.DeleteCommand
import de.dseelp.kotlincord.plugins.moderation.commands.KickCommand

@PluginInfo("io.github.dseelp.kotlincord.plugins", "Moderation", "0.1", "This is a Moderation Module", ["DSeeLP"])
object ModerationPlugin : Plugin() {

    @PluginAction(PluginAction.Action.ENABLE)
    fun enable() {
        println("Enabling Moderation Plugin")
        register<DeleteCommand>()
        register<KickCommand>()
    }

}