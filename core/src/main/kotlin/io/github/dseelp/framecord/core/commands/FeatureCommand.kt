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

package io.github.dseelp.framecord.core.commands

import de.dseelp.kommon.command.CommandNode
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.SelectMenuBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import io.github.dseelp.framecord.api.InternalFrameCordApi
import io.github.dseelp.framecord.api.checkPermissions
import io.github.dseelp.framecord.api.command.*
import io.github.dseelp.framecord.api.interactions.ButtonAction
import io.github.dseelp.framecord.api.interactions.SelectionMenu
import io.github.dseelp.framecord.api.interactions.SelectionOptionClickContext
import io.github.dseelp.framecord.api.modules.Feature
import io.github.dseelp.framecord.api.modules.Module
import io.github.dseelp.framecord.api.modules.ModuleManager
import io.github.dseelp.framecord.api.randomAlphanumeric
import io.github.dseelp.framecord.api.selectionMenu
import io.github.dseelp.framecord.api.utils.deleteAfter
import io.github.dseelp.framecord.api.utils.footer
import io.github.dseelp.framecord.api.utils.literal
import io.github.dseelp.framecord.api.utils.red
import io.github.dseelp.framecord.core.FakePlugin
import io.github.dseelp.framecord.core.plugin.FakePluginComponent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds

class FeatureCommand : Command<GuildChannelSender<GuildMessageChannel>>, FakePluginComponent {
    private val moduleManager: ModuleManager by inject()

    override val scopes: Array<CommandScope> = arrayOf(CommandScope.GUILD)

    @OptIn(InternalFrameCordApi::class, KordPreview::class, kotlin.time.ExperimentalTime::class)
    override val node: CommandNode<GuildChannelSender<GuildMessageChannel>> = literal("modules") {
        checkAccess {
            sender.getMember().checkPermissions(Permission.ManageGuild)
        }
        noAccess {
            sender.createEmbed {
                title = "Permission denied"
                color = Color.red
                description = "You need the ManageGuild Permission to use this command"
                footer = sender.getMember().footer()
            }.deleteAfter(seconds(10))
        }
        execute {
            val memberId = sender.getMember().id
            var message: Message? = null
            var stateSelector: SelectionMenu? = null
            var selectedModule: Module? = null
            var selectedFeature: Feature? = null
            var state = State.NONE
            val mutex = Mutex()
            var buttonAction: ButtonAction? = null
            var disabled = false
            fun ActionRowBuilder.generateButton() {
                interactionButton(ButtonStyle.Success, buttonAction!!.encodedId("complete")) {
                    this.label = "Complete"
                    this.disabled = disabled
                }
            }

            fun generateStateBuilder(): SelectMenuBuilder {
                return stateSelector!!.discordComponentBuilder().apply {
                    if (state == State.NONE) return@apply
                    options.onEach {
                        it.default = false
                        if (it.value == state.name.lowercase()) it.default = true
                    }
                }
            }

            var enabledSelector: SelectionMenu? = null
            suspend fun generateEnabledBuilder(): SelectMenuBuilder {
                val value =
                    if (selectedModule != null) selectedModule!!.isEnabled(sender.getGuild().id) else if (selectedFeature != null) selectedFeature!!.isEnabled(
                        sender.getGuild().id
                    ) else false
                return enabledSelector!!.discordComponentBuilder().apply {
                    options.onEach {
                        it.default = false
                        if (it.value == "enabled" && value) it.default = true
                        if (it.value == "disabled" && !value) it.default = true
                    }
                }
            }

            var modulesSelector: SelectionMenu? = null
            fun generateModuleBuilder(): SelectMenuBuilder {
                return modulesSelector!!.discordComponentBuilder().apply {
                    if (selectedModule == null) return@apply
                    options.onEach {
                        it.default = false
                        if (it.value == selectedModule!!.id) it.default = true
                    }
                }
            }
            modulesSelector = FakePlugin.registerSelectionMenu {
                moduleManager.getRegisteredModules().onEach {
                    option(it.name, it.id) {
                        onClick {
                            selectedModule = it
                            val acknowledge = interaction.acknowledgePublicDeferredMessageUpdate()
                            if (memberId != interaction.user.id) return@onClick
                            acknowledge.edit {
                                components = mutableListOf()
                                actionRow {
                                    components.add(generateStateBuilder())
                                }
                                actionRow {
                                    components.add(generateModuleBuilder())
                                }
                                actionRow {
                                    components.add(generateEnabledBuilder())
                                }
                                actionRow {
                                    generateButton()
                                }
                            }
                        }
                    }
                }.collect()
            }
            enabledSelector = FakePlugin.registerSelectionMenu {
                val action: suspend SelectionOptionClickContext.(id: Boolean) -> Unit = { id ->
                    val acknowledge = interaction.acknowledgePublicDeferredMessageUpdate()
                    if (memberId == interaction.user.id) {
                        if (id) {
                            selectedFeature?.enable(sender.getGuild().id)
                            selectedModule?.enable(sender.getGuild().id)
                        } else {
                            selectedFeature?.disable(sender.getGuild().id)
                            selectedModule?.disable(sender.getGuild().id)
                        }
                        acknowledge.edit {
                            components = mutableListOf()
                            actionRow {
                                components.add(generateStateBuilder())
                            }
                            actionRow {
                                components.add(generateModuleBuilder())
                            }
                            actionRow {
                                components.add(generateEnabledBuilder())
                            }
                            actionRow {
                                generateButton()
                            }
                        }
                    }
                }
                val actions = mapOf("Enabled" to "enabled", "Disabled" to "disabled")
                for (pair in actions) {
                    option(pair.key, pair.value) {
                        onClick {
                            if (memberId != interaction.user.id) return@onClick
                            action.invoke(this, pair.value == "enabled")
                        }
                    }
                }
            }
            suspend fun setState(value: State): Unit = mutex.withLock {
                if (state == value) return@withLock
                state = value
                when (value) {
                    State.NONE -> {

                    }
                    State.MODULES -> {
                        selectedModule = null
                        selectedFeature = null
                        message!!.edit {
                            components = mutableListOf()
                            actionRow {
                                components.add(generateStateBuilder())
                            }
                            actionRow {
                                selectionMenu(modulesSelector)
                            }
                            actionRow {
                                generateButton()
                            }
                        }
                    }
                    State.FEATURES -> {
                        selectedModule = null
                        selectedFeature = null
                        message!!.edit {
                            components = mutableListOf()
                            actionRow {
                                components.add(generateStateBuilder())
                            }
                            actionRow {
                                generateButton()
                            }
                        }
                    }
                }
            }
            stateSelector = FakePlugin.registerSelectionMenu {
                alwaysUseMultiOptionCallback = false

                option("Modules", "modules") {
                    onClick {
                        if (memberId != interaction.user.id) return@onClick
                        interaction.acknowledgePublicDeferredMessageUpdate()
                        setState(State.MODULES)
                    }
                }
                option("Features", "features") {
                    onClick {
                        if (memberId != interaction.user.id) return@onClick
                        interaction.acknowledgePublicDeferredMessageUpdate()
                        setState(State.FEATURES)
                    }
                }
            }
            fun deleteAll() {
                FakePlugin.unregisterSelectionMenu(stateSelector)
                FakePlugin.unregisterSelectionMenu(modulesSelector)
                FakePlugin.unregisterSelectionMenu(enabledSelector)
            }
            buttonAction = FakePlugin.registerButtonAction(
                randomAlphanumeric(8),
                io.github.dseelp.framecord.api.utils.literal("") {
                    literal("complete") {
                        checkAccess {
                            memberId == sender.interaction.user.id
                        }
                        execute {
                            sender.interaction.acknowledgePublicDeferredMessageUpdate()
                            disabled = true
                            deleteAll()
                        }
                    }
                })
            message = sender.createMessage {
                embed {
                    title = "Module Settings"
                }
                actionRow {
                    selectionMenu(stateSelector)
                }
                actionRow {
                    generateButton()
                }
            }
        }
    }

    private enum class State {
        NONE,
        MODULES,
        FEATURES
    }
}
