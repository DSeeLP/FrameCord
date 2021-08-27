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

package io.github.dseelp.framecord.api.plugins

import de.dseelp.kommon.command.CommandNode
import io.github.dseelp.framecord.api.Version
import io.github.dseelp.framecord.api.command.Command
import io.github.dseelp.framecord.api.database.DatabaseInfo
import io.github.dseelp.framecord.api.database.DatabaseRegistry
import io.github.dseelp.framecord.api.database.DatabaseScope
import io.github.dseelp.framecord.api.event.EventBus
import io.github.dseelp.framecord.api.interactions.ButtonAction
import io.github.dseelp.framecord.api.interactions.ButtonContext
import io.github.dseelp.framecord.api.interactions.SelectionMenu
import io.github.dseelp.framecord.api.interactions.SelectionMenuBuilder
import io.github.dseelp.framecord.api.logging.logger
import io.github.dseelp.framecord.api.modules.Module
import io.github.dseelp.framecord.api.modules.ModuleManager
import io.github.dseelp.framecord.api.utils.Criterion
import io.github.dseelp.framecord.api.utils.ReflectionUtils
import io.github.dseelp.framecord.api.utils.koin.KoinModules
import io.github.dseelp.framecord.api.utils.register
import org.koin.core.component.inject
import org.koin.dsl.koinApplication
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
abstract class Plugin : PluginComponent<Plugin> {

    override val plugin: Plugin
        get() = this

    private val _meta: PluginMeta? = null

    val eventBus: EventBus by inject()

    val koinApp = koinApplication { }

    init {
        KoinModules.load(this)
    }

    val moduleManager: ModuleManager by inject()

    val databaseRegistry: DatabaseRegistry by inject()

    val meta: PluginMeta
        get() = _meta!!

    val name: String
        get() = meta.name
    val version: Version
        get() = meta.version
    val dataFolder: Path
        get() = meta.dataFolder.also { it.createDirectories() }

    val logger by logger()

    val buttonActions: Array<ButtonAction>
        get() = _buttonActions.toTypedArray()

    private val _buttonActions = mutableListOf<ButtonAction>()

    val selectionMenus: Array<SelectionMenu>
        get() = _selectionMenus.toTypedArray()

    private val _selectionMenus = mutableListOf<SelectionMenu>()

    fun checkModule(id: String, name: String): Module =
        moduleManager.getRegisteredModule(id) ?: moduleManager.registerModule(id, name)

    fun registerButtonAction(name: String, node: CommandNode<ButtonContext>): ButtonAction {

        val action = ButtonAction(this, name, node)
        for (a in _buttonActions) {
            if (a.name.equals(
                    name,
                    true
                )
            ) throw IllegalArgumentException("A button action with this name is already registered!")
        }
        _buttonActions.add(action)
        return action
    }

    fun unregisterButtonAction(action: ButtonAction) {
        _buttonActions.remove(action)
    }

    fun getButtonAction(name: String): ButtonAction? = _buttonActions.firstOrNull { it.name.equals(name, true) }

    fun registerSelectionMenu(menu: SelectionMenu) {
        _selectionMenus.add(menu)
    }

    inline fun registerSelectionMenu(block: SelectionMenuBuilder.() -> Unit): SelectionMenu {
        val menu = SelectionMenuBuilder().apply(block).build(this)
        registerSelectionMenu(menu)
        return menu
    }

    fun unregisterSelectionMenu(menu: SelectionMenu) {
        _selectionMenus.remove(menu)
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Plugin) return false

        if (meta.name.lowercase() != other.meta.name.lowercase()) return false

        return true
    }


    inline fun <reified T : Any> registerListener(listener: T) = eventBus.addClassHandler(this, listener)
    inline fun <reified T : Any> registerListener() = eventBus.addClassHandler<T>(this)

    fun register(command: Command<*>) {
        register(command.node, command.description, command.scopes, command.featureRestricted)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Command<*>> register() = register(T::class as KClass<Command<*>>)

    fun register(clazz: KClass<Command<*>>) {
        val instance = (clazz.objectInstance ?: koinApp.koin.getOrNull()) ?: clazz.createInstance()
        register(instance)
    }

    override fun hashCode(): Int {
        return _meta?.hashCode() ?: 0
    }

    fun checkDataFolder() {
        if (!dataFolder.exists()) dataFolder.toFile().mkdirs()
    }

    suspend fun registerDatabase(info: DatabaseInfo) = databaseRegistry.registerDatabase(this, info)

    fun <T> database(block: DatabaseScope.() -> T) = block.invoke(databaseRegistry.getScope(this))
    suspend fun <T> suspendingDatabase(block: suspend DatabaseScope.() -> T) =
        block.invoke(databaseRegistry.getScope(this))

    fun searchEvents(vararg packages: String) = eventBus.searchPackages(this, *packages)

    fun searchCommands(vararg packages: String) {
        ReflectionUtils.findClasses(packages.toList().toTypedArray(), this) {
            Criterion.isSubClassOf(Command::class).assert()
            Criterion.hasAnnotation<DisableAutoLoad>().assertNot()
        }.onEach { clazz ->
            @Suppress("UNCHECKED_CAST")
            register(clazz as KClass<Command<*>>)
        }
    }

    fun searchCommands(packageName: String) = searchCommands(*arrayOf(packageName).toList().toTypedArray())


}