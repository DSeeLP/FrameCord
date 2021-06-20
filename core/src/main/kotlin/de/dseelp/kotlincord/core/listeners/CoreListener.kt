/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.listeners

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.ParsedResult
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.ReloadScope
import de.dseelp.kotlincord.api.buttons.ButtonAction
import de.dseelp.kotlincord.api.command.ConsoleSender
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.command.Sender
import de.dseelp.kotlincord.api.event.EventHandle
import de.dseelp.kotlincord.api.events.ConsoleMessageEvent
import de.dseelp.kotlincord.api.events.ReloadEvent
import de.dseelp.kotlincord.api.logging.LogManager
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.PluginLoader
import de.dseelp.kotlincord.api.plugins.PluginManager
import de.dseelp.kotlincord.api.utils.CommandUtils
import de.dseelp.kotlincord.api.utils.CommandUtils.execute
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.core.Core
import de.dseelp.kotlincord.core.FakePlugin
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier
import java.util.*

@OptIn(InternalKotlinCordApi::class)
object CoreListener : CordKoinComponent {

    val pluginService: PluginManager by inject()

    @EventHandle
    fun onReload(event: ReloadEvent) {
        val scopes = event.scopes
        if (scopes.contains(ReloadScope.SETTINGS)) Core.loadConfig()
        if (scopes.contains(ReloadScope.PLUGINS)) {
            for (index in 0..loader.loadedPlugins.lastIndex) {
                val data = loader.loadedPlugins.getOrNull(index) ?: continue
                pluginService.unload(data)
            }
            val pluginLocation = Core.pathQualifiers.pluginLocation
            val file = pluginLocation.toFile()
            if (!file.exists()) file.mkdir()
            for (path in file.listFiles()!!) {
                val load = Core.pluginService.load(path)
                Core.pluginService.enable(load.plugin!!)
            }
        }
    }

    val buttonLog by logger("Buttons")

    private val guildDispatcher by inject<CommandDispatcher<Sender>>(qualifier("guild"))
    private val privateDispatcher by inject<CommandDispatcher<Sender>>(qualifier("private"))
    private val consoleDispatcher: CommandDispatcher<Sender> by inject(qualifier("console"))
    private val loader: PluginLoader by inject()

    @EventHandle
    fun onMessageReceived(event: MessageReceivedEvent) {
        val fromGuild = event.isFromGuild
        val message = event.message
        val content = message.contentRaw
        if (!content.startsWith("!")) return
        if (event.author == event.jda.selfUser) return
        if (message.embeds.isNotEmpty()) return
        (if (fromGuild) guildDispatcher else privateDispatcher).execute(
            GuildSender(event.jda, message),
            content.replaceFirst("!", ""),
            CommandUtils.Actions.noOperation()
        )
    }

    val rootLogger by logger(LogManager.ROOT)

    @EventHandle
    fun onConsoleMessage(event: ConsoleMessageEvent) {
        if (event.message.isBlank() || event.message.isEmpty()) return
        consoleDispatcher.execute(
            ConsoleSender,
            event.message,
            bypassAccess = true,
            actions = object : CommandUtils.Actions<Sender> {
                override fun error(message: String, result: ParsedResult<Sender>?, throwable: Throwable?) {
                    if (result == null) rootLogger.warn("Command could not be found! For help, use the command \"help\".")
                    throwable?.printStackTrace()
                }

                override fun success(result: ParsedResult<Sender>) = Unit

            })
    }

    @EventHandle
    fun onButtonClick(event: ButtonClickEvent) {
        if (event.message?.author != event.jda.selfUser) return
        var id = event.button!!.id!!
        if (!id.startsWith(ButtonAction.QUALIFIER)) {
            if (id.startsWith("cord:"))
                buttonLog.debug("Tried to execute button click event from another instance of KotlinCord")
            else buttonLog.debug("Tried to execute button click event from an unknown button!")
            return
        }
        id = id.replaceFirst(ButtonAction.QUALIFIER, "")
        val splittedId =
            kotlin.runCatching { Base64.getDecoder().decode(id).decodeToString().split(ButtonAction.DELIMITER) }
                .getOrNull() ?: return
        val datas = loader.loadedPlugins + FakePlugin.fakeData
        var found = false
        for (data in datas) {
            val plugin = data.plugin ?: continue
            val action = plugin.buttonActions.firstOrNull { it.id == splittedId[0] } ?: continue
            found = true
            action.execute(event)
            break
        }
        if (!found) buttonLog.debug("No Button with the id ${splittedId[0]} was found!")
    }
}