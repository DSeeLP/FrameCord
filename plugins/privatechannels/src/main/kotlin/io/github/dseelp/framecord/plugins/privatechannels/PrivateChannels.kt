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

package io.github.dseelp.framecord.plugins.privatechannels

import com.log4k.w
import io.github.dseelp.framecord.api.configs.BotConfig
import io.github.dseelp.framecord.api.database.DatabaseType
import io.github.dseelp.framecord.api.plugins.DatabaseConfig
import io.github.dseelp.framecord.api.plugins.Plugin
import io.github.dseelp.framecord.api.plugins.PluginAction
import io.github.dseelp.framecord.api.plugins.PluginInfo
import io.github.dseelp.framecord.plugins.privatechannels.db.ActivePrivateChannelsTable
import io.github.dseelp.framecord.plugins.privatechannels.db.PrivateChannelsTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.koin.core.component.inject

@PluginInfo(
    "io.github.dseelp.framecord.plugins",
    "PrivateChannels",
    "0.5.0",
    "This is a Private Channel Module",
    ["DSeeLP"]
)
object PrivateChannels : Plugin() {
    val mId = "framecord/privatechannels"

    @PluginAction(PluginAction.Action.ENABLE)
    suspend fun enable() {
        println("Enabling Private Channel")
        checkModule(mId, "PrivateChannels").features
        val config by inject<BotConfig>()
        if (!config.intents.presence) {
            w("The Presence Intent isn't enabled! Without that intent enabled it can't show the games people play in the channel names")
            w("You can enable the intent in the config.json of the bot. You need to enable it in the bot dashboard too.")
        }
        val packageName = "io.github.dseelp.framecord.plugins.privatechannels"
        searchCommands(packageName)
        searchEvents(packageName)
        val defaultDbConfig: DatabaseConfig = getKoin().get<DatabaseConfig>()
            .let { if (it.type == DatabaseType.SQLITE) it.copy(databaseName = "privatechannels") else it }
        checkDataFolder()
        registerDatabase(DatabaseConfig.load(this, defaultDbConfig).toDatabaseInfo(this))
        database {
            transaction {
                SchemaUtils.createMissingTablesAndColumns(PrivateChannelsTable, ActivePrivateChannelsTable)
            }
        }
    }

}