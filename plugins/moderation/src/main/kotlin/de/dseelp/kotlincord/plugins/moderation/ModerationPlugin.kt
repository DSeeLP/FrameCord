/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.plugins.moderation

import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.PluginAction
import de.dseelp.kotlincord.api.plugins.PluginInfo
import de.dseelp.kotlincord.plugins.moderation.commands.DeleteCommand
import de.dseelp.kotlincord.plugins.moderation.commands.KickCommand

@PluginInfo("Moderation", "0.1", "This is a Moderation Module", ["DSeeLP"])
object ModerationPlugin : Plugin() {

    @PluginAction(PluginAction.Action.ENABLE)
    fun enable() {
        println("Enabling Moderation Plugin")
        register<DeleteCommand>()
        register<KickCommand>()
    }

}