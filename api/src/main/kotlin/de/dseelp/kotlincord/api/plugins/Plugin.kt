/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.Version
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.database.DatabaseInfo
import de.dseelp.kotlincord.api.database.DatabaseRegistry
import de.dseelp.kotlincord.api.database.DatabaseScope
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.interactions.ButtonAction
import de.dseelp.kotlincord.api.interactions.ButtonContext
import de.dseelp.kotlincord.api.interactions.SelectionMenu
import de.dseelp.kotlincord.api.interactions.SelectionMenuBuilder
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.utils.Criterion
import de.dseelp.kotlincord.api.utils.ReflectionUtils
import de.dseelp.kotlincord.api.utils.koin.KoinModules
import de.dseelp.kotlincord.api.utils.register
import org.koin.core.component.inject
import org.koin.dsl.koinApplication
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@OptIn(InternalKotlinCordApi::class)
abstract class Plugin : PluginComponent<Plugin> {

    @InternalKotlinCordApi
    override val plugin: Plugin
        get() = this

    private val _meta: PluginMeta? = null

    val eventBus: EventBus by inject()

    val koinApp = koinApplication { }

    init {
        KoinModules.load(this)
    }

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

    fun registerSelectionMenu(block: SelectionMenuBuilder.() -> Unit): SelectionMenu {
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
        register(command.node, *command.scopes)
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

    suspend fun registerDatabase(info: DatabaseInfo) = databaseRegistry.registerDatabase(this, info)

    fun <T> database(block: DatabaseScope.() -> T) = block.invoke(databaseRegistry.getScope(this))

    fun searchEvents(vararg packages: String) = eventBus.searchPackages(this, *packages)

    fun searchCommands(vararg packages: String) {
        ReflectionUtils.findClasses(packages.toList().toTypedArray()) {
            Criterion.isSubClassOf(Command::class).assert()
        }.onEach { clazz ->
            register(clazz as KClass<Command<*>>)
        }
    }

    fun searchCommands(packageName: String) = searchCommands(*arrayOf(packageName).toList().toTypedArray())


}