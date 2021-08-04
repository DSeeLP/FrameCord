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

package de.dseelp.kotlincord.plugins.privatechannels

import de.dseelp.kotlincord.api.configs.BotConfig
import de.dseelp.kotlincord.api.plugins.DatabaseConfig
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.PluginAction
import de.dseelp.kotlincord.api.plugins.PluginInfo
import de.dseelp.kotlincord.plugins.privatechannels.db.ActivePrivateChannels
import de.dseelp.kotlincord.plugins.privatechannels.db.PrivateChannels
import org.jetbrains.exposed.sql.SchemaUtils
import org.koin.core.component.inject

@PluginInfo(
    "io.github.dseelp.kotlincord.plugins",
    "PrivateChannels",
    "0.3",
    "This is a Private Channel Module",
    ["DSeeLP"]
)
object PrivateChannelPlugin : Plugin() {

    @PluginAction(PluginAction.Action.ENABLE)
    suspend fun enable() {
        println("Enabling Private Channel")
        val config by inject<BotConfig>()
        if (!config.intents.presence) {
            logger.warn("The Presence Intent isn't enabled! Without that intent enabled it can't show the games people play in the channel names")
            logger.warn("You can enable the intent in the config.json of the bot. You need to enable it in the bot dashboard too.")
        }
        val packageName = "de.dseelp.kotlincord.plugins.privatechannels"
        searchCommands(packageName)
        searchEvents(packageName)
        val defaultDbConfig: DatabaseConfig by inject()
        checkDataFolder()
        registerDatabase(DatabaseConfig.load(this, defaultDbConfig).toDatabaseInfo(this))
        database {
            transaction {
                SchemaUtils.createMissingTablesAndColumns(PrivateChannels, ActivePrivateChannels)
            }
        }
    }

}