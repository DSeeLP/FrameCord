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

package de.dseelp.kotlincord.core.listeners

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.ParsedResult
import de.dseelp.kotlincord.api.Bot
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.ReloadScope
import de.dseelp.kotlincord.api.command.ConsoleSender
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.command.PrivateSender
import de.dseelp.kotlincord.api.command.Sender
import de.dseelp.kotlincord.api.event.EventHandle
import de.dseelp.kotlincord.api.events.ConsoleMessageEvent
import de.dseelp.kotlincord.api.events.ReloadEvent
import de.dseelp.kotlincord.api.guild.info
import de.dseelp.kotlincord.api.interactions.ButtonAction
import de.dseelp.kotlincord.api.interactions.SelectionOptionClickContext
import de.dseelp.kotlincord.api.logging.LogManager
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.PluginLoader
import de.dseelp.kotlincord.api.plugins.PluginManager
import de.dseelp.kotlincord.api.utils.CommandUtils
import de.dseelp.kotlincord.api.utils.CommandUtils.execute
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.core.CordImpl
import de.dseelp.kotlincord.core.Core
import de.dseelp.kotlincord.core.FakePlugin
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.InteractionType
import dev.kord.core.entity.component.ButtonComponent
import dev.kord.core.entity.component.SelectMenuComponent
import dev.kord.core.entity.interaction.ComponentInteraction
import dev.kord.core.entity.interaction.SelectMenuInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier
import java.util.*

@OptIn(InternalKotlinCordApi::class)
object CoreListener : CordKoinComponent {

    val pluginService: PluginManager by inject()

    @EventHandle
    suspend fun onReload(event: ReloadEvent) {
        val scopes = event.scopes
        if (scopes.contains(ReloadScope.SETTINGS)) Core.loadConfig()
        if (scopes.contains(ReloadScope.PLUGINS)) {
            CordImpl.reloadPlugins()
        }
    }

    val buttonLog by logger("Buttons")

    private val guildDispatcher by inject<CommandDispatcher<Sender>>(qualifier("guild"))
    private val privateDispatcher by inject<CommandDispatcher<Sender>>(qualifier("private"))
    private val consoleDispatcher: CommandDispatcher<Sender> by inject(qualifier("console"))
    private val loader: PluginLoader by inject()

    val bot: Bot by inject()

    @EventHandle
    suspend fun onMessageReceived(event: MessageCreateEvent) {
        val message = event.message
        val content = message.content
        if (message.author == bot.kord.getSelf()) return
        if (message.embeds.isNotEmpty()) return
        val guild = event.getGuild()
        val (sender, prefix) = if (guild != null) GuildSender(message) to guild.info.prefix else PrivateSender(message) to "!"
        if (!content.startsWith(prefix)) return
        bot.kord.launch {
            (if (event.guildId != null) guildDispatcher else privateDispatcher).execute(
                sender,
                content.replaceFirst(prefix, ""),
                CommandUtils.Actions.noOperation()
            )
        }
    }

    val rootLogger by logger(LogManager.ROOT)

    @EventHandle
    suspend fun onConsoleMessage(event: ConsoleMessageEvent) {
        if (event.message.isBlank() || event.message.isEmpty()) return
        consoleDispatcher.execute(
            ConsoleSender,
            event.message,
            bypassAccess = true,
            actions = object : CommandUtils.Actions<Sender> {
                override suspend fun error(message: String, result: ParsedResult<Sender>?, throwable: Throwable?) {
                    if (result == null) rootLogger.warn("Command could not be found! For help, use the command \"help\".")
                    handleError(message, result, throwable)
                }

                override suspend fun success(result: ParsedResult<Sender>) = Unit

            })
    }

    val interactionLog by logger("Interactions")

    @OptIn(KordPreview::class)
    @EventHandle
    suspend fun onInteractionComponentReceive(event: InteractionCreateEvent) {
        val interaction = event.interaction
        if (interaction.type != InteractionType.Component) return
        interaction as ComponentInteraction
        if (interaction.component == null) return
        val authorId = interaction.message?.author?.id
        if (authorId != null && authorId != bot.kord.selfId) return
        var id = interaction.componentId
        if (!id.startsWith(ButtonAction.QUALIFIER)) {
            if (id.startsWith("cord:"))
                buttonLog.debug("Tried to execute button click event from another instance of KotlinCord")
            else buttonLog.debug("Tried to execute button click event from an unknown button!")
            return
        }
        id = id.replaceFirst(ButtonAction.QUALIFIER, "")
        when (interaction.component) {
            is ButtonComponent -> executeButtonClick(id, event)
            is SelectMenuComponent -> executeSelectMenuClick(id, event)
            else -> interactionLog.debug("Unsupported interaction occured ${if (interaction.component != null) interaction.component!!::class.qualifiedName else "null"}")
        }
    }

    @OptIn(KordPreview::class)
    suspend fun executeSelectMenuClick(id: String, event: InteractionCreateEvent) {
        val interaction = event.interaction as SelectMenuInteraction
        val datas = loader.loadedPlugins + FakePlugin.fakeData
        var found = false
        val splittedId =
            kotlin.runCatching { Base64.getDecoder().decode(id).decodeToString().split(ButtonAction.DELIMITER) }
                .getOrNull() ?: return
        for (data in datas) {
            val plugin = data.plugin ?: continue
            val menu = plugin.selectionMenus.firstOrNull { it.id == splittedId[0] } ?: continue
            found = true
            val clickedOptions = menu.options.filter { interaction.values.contains(it.value) }.toTypedArray()
            val context = SelectionOptionClickContext(event, interaction, clickedOptions)
            if (clickedOptions.size == 1 && !menu.alwaysUseMultiOptionCallback) clickedOptions.first().onClick(context)
            else menu.onMultipleOptionClick(context)
            break
        }
    }

    @OptIn(KordPreview::class)
    suspend fun executeButtonClick(id: String, event: InteractionCreateEvent) {
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