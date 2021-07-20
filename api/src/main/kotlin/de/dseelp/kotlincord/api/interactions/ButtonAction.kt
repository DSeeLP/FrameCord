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

package de.dseelp.kotlincord.api.interactions

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.CommandNode
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.utils.CommandUtils
import de.dseelp.kotlincord.api.utils.CommandUtils.execute
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import dev.kord.common.annotation.KordPreview
import dev.kord.core.entity.interaction.ComponentInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent
import org.koin.core.qualifier.qualifier
import java.util.*

@OptIn(InternalKotlinCordApi::class)
class ButtonAction(plugin: Plugin, val name: String, val node: CommandNode<ButtonContext>) : CordKoinComponent {
    val id
        get() = hashCode().toString()

    private val pluginMeta = plugin.meta

    private val dispatcher = CommandDispatcher<ButtonContext>()

    init {
        dispatcher.register(node.copy(name = id, argumentIdentifier = null, aliases = arrayOf()))
    }

    @OptIn(KordPreview::class)
    suspend fun execute(event: InteractionCreateEvent) {
        val interaction = event.interaction
        if (interaction !is ComponentInteraction) return
        var id = interaction.componentId
        if (!id.startsWith(QUALIFIER)) {
            if (id.startsWith("cord:"))
                log.warn("Tried to execute button click event from another instance of KotlinCord")
            else log.warn("Tried to execute button click event from an unknown button!")
            return
        }
        id = id.replaceFirst(QUALIFIER, "")
        val decodedId = Base64.getDecoder().decode(id).decodeToString()
        if (!decodedId.startsWith(this.id)) return
        dispatcher.execute(
            ButtonContext(this, interaction),
            decodedId.replaceFirst(DELIMITER, " "),
            CommandUtils.Actions.noOperation()
        )
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ButtonAction) return false

        if (name != other.name) return false
        if (node != other.node) return false
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

@OptIn(KordPreview::class)
data class ButtonContext(val action: ButtonAction, val interaction: ComponentInteraction) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ButtonContext) return false

        if (interaction != other.interaction) return false

        return true
    }

    override fun hashCode(): Int {
        return interaction.hashCode()
    }
}