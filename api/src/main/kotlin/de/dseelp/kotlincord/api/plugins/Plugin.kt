/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.Version
import de.dseelp.kotlincord.api.buttons.ButtonAction
import de.dseelp.kotlincord.api.buttons.ButtonContext
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.database.DatabaseInfo
import de.dseelp.kotlincord.api.database.DatabaseRegistry
import de.dseelp.kotlincord.api.database.DatabaseScope
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.utils.koin.KoinModules
import de.dseelp.kotlincord.api.utils.register
import org.koin.core.component.inject
import org.koin.dsl.koinApplication
import java.nio.file.Path
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
        get() = meta.dataFolder

    val logger by logger()

    val buttonActions: Array<ButtonAction>
        get() = _buttonActions.toTypedArray()

    private val _buttonActions = mutableListOf<ButtonAction>()

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

    fun getButtonAction(name: String): ButtonAction? = _buttonActions.firstOrNull { it.name.equals(name, true) }

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

    inline fun <reified T : Command<*>> register() {
        val instance = (T::class.objectInstance ?: koinApp.koin.getOrNull()) ?: T::class.createInstance()
        register(instance)
    }

    override fun hashCode(): Int {
        return _meta?.hashCode() ?: 0
    }

    suspend fun registerDatabase(info: DatabaseInfo) = databaseRegistry.registerDatabase(this, info)

    fun <T> database(block: DatabaseScope.() -> T) = block.invoke(databaseRegistry.getScope(this))


}