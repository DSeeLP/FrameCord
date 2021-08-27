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

package io.github.dseelp.framecord.core.listeners

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.ParsedResult
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.InteractionType
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.core.entity.component.ButtonComponent
import dev.kord.core.entity.component.SelectMenuComponent
import dev.kord.core.entity.interaction.SelectMenuInteraction
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import io.github.dseelp.framecord.api.command.*
import io.github.dseelp.framecord.api.event.EventHandle
import io.github.dseelp.framecord.api.events.ConsoleMessageEvent
import io.github.dseelp.framecord.api.events.ReloadEvent
import io.github.dseelp.framecord.api.guild.info
import io.github.dseelp.framecord.api.interactions.ButtonAction
import io.github.dseelp.framecord.api.interactions.SelectionOptionClickContext
import io.github.dseelp.framecord.api.logging.LogManager
import io.github.dseelp.framecord.api.logging.logger
import io.github.dseelp.framecord.api.plugins.PluginLoader
import io.github.dseelp.framecord.api.plugins.PluginManager
import io.github.dseelp.framecord.api.utils.CommandUtils
import io.github.dseelp.framecord.api.utils.CommandUtils.execute
import io.github.dseelp.framecord.api.utils.clientMention
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import io.github.dseelp.framecord.core.CordImpl
import io.github.dseelp.framecord.core.Core
import io.github.dseelp.framecord.core.FakePlugin
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier
import java.util.*

@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
object CoreListener : CordKoinComponent {

    val pluginService: PluginManager by inject()

    @EventHandle
    suspend fun onReload(event: ReloadEvent) {
        val scopes = event.scopes
        if (scopes.contains(io.github.dseelp.framecord.api.ReloadScope.SETTINGS)) Core.loadConfig()
        if (scopes.contains(io.github.dseelp.framecord.api.ReloadScope.PLUGINS)) {
            CordImpl.reloadPlugins()
        }
    }

    val buttonLog by logger("Buttons")

    private val guildDispatcher by inject<CommandDispatcher<Sender>>(qualifier("guild"))
    private val privateDispatcher by inject<CommandDispatcher<Sender>>(qualifier("private"))
    private val consoleDispatcher: CommandDispatcher<Sender> by inject(qualifier("console"))
    private val loader: PluginLoader by inject()

    val bot: io.github.dseelp.framecord.api.Bot by inject()

    @EventHandle
    suspend fun onMessageReceived(event: MessageCreateEvent) {
        val message = event.message
        val content = message.content
        val self = bot.kord.getSelf()
        if (message.author == self || message.author == null) return
        if (message.embeds.isNotEmpty()) return
        val guild = event.getGuild()
        val channel = message.channel.asChannel()
        val (sender, prefix) = if (guild != null) {
            (if (channel is TopGuildMessageChannel) GuildSender(message) else ThreadSender(message)) to guild.info.prefix
        } else PrivateSender(message) to "!"
        val tagPrefix = self.clientMention
        val taggedPrefix = content.startsWith(tagPrefix)
        if (!content.startsWith(prefix) && !taggedPrefix) return
        bot.kord.launch {
            (if (event.guildId != null) guildDispatcher else privateDispatcher).execute(
                sender,
                content.replaceFirst((if (taggedPrefix) "$tagPrefix " else prefix), ""),
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
    suspend fun onInteractionComponentReceive(event: ComponentInteractionCreateEvent) {
        val interaction = event.interaction
        if (interaction.type != InteractionType.Component) return
        if (interaction.component == null) return
        val authorId = interaction.message?.author?.id
        if (authorId != null && authorId != bot.kord.selfId) return
        var id = interaction.componentId
        if (!id.startsWith(ButtonAction.QUALIFIER)) {
            if (id.startsWith("cord:"))
                buttonLog.debug("Tried to execute button click event from another instance of FrameCord")
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
    suspend fun executeButtonClick(id: String, event: ComponentInteractionCreateEvent) {
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