/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.buttons

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.literal
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.utils.CommandUtils
import de.dseelp.kotlincord.api.utils.CommandUtils.execute
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import org.koin.core.qualifier.qualifier
import java.util.*

@OptIn(InternalKotlinCordApi::class)
class ButtonAction(plugin: Plugin, val name: String, val nodes: Array<CommandNode<ButtonContext>>) : CordKoinComponent {
    val id
        get() = hashCode().toString()

    private val pluginMeta = plugin.meta

    private val dispatcher = CommandDispatcher<ButtonContext>()

    init {
        dispatcher.register(literal(id) {
            for (node in nodes) {
                node(node)
            }
        })
    }

    fun execute(event: ButtonClickEvent) {
        var id = event.button!!.id!!
        if (!id.startsWith(QUALIFIER)) {
            if (id.startsWith("cord:"))
                log.warn("Tried to execute button click event from another instance of KotlinCord")
            else log.warn("Tried to execute button click event from an unknown button!")
            return
        }
        id = id.replaceFirst(QUALIFIER, "")
        val decodedId = Base64.getDecoder().decode(id).decodeToString()
        if (!decodedId.startsWith(this.id)) return
        event.deferEdit().queue()
        dispatcher.execute(
            ButtonContext(this, event),
            decodedId.replaceFirst(DELIMITER, " "),
            CommandUtils.Actions.noOperation()
        )
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ButtonAction) return false

        if (name != other.name) return false
        if (!nodes.contentEquals(other.nodes)) return false
        if (pluginMeta != other.pluginMeta) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + pluginMeta.hashCode()
        return result
    }

    companion object : CordKoinComponent {
        const val DELIMITER = "|:|"

        val QUALIFIER: String = "cord:${getKoin().get<String>(qualifier("instanceId"))}:"

        val log by logger("Buttons")
    }
}

data class ButtonContext(val action: ButtonAction, val event: ButtonClickEvent) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ButtonContext) return false

        if (event != other.event) return false

        return true
    }

    override fun hashCode(): Int {
        return event.hashCode()
    }
}