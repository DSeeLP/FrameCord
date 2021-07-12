package de.dseelp.kotlincord.api.interactions

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.randomAlphanumeric
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.optional.Optional
import dev.kord.core.entity.interaction.SelectMenuInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.component.SelectMenuBuilder
import dev.kord.rest.builder.component.SelectOptionBuilder
import org.koin.core.qualifier.qualifier
import java.util.*

data class SelectionMenu(
    val plugin: Plugin,
    val options: Array<SelectionOption>,
    val allowedValues: IntRange = 1..1,
    val placeholder: String? = null
) {

    val randomId by lazy { randomAlphanumeric(8) }
    val id by lazy {
        QUALIFIER + hashCode()
    }

    @OptIn(KordPreview::class)
    val discordComponentBuilder by lazy {
        val discordOptions = options.map { it.discordOptionBuilder }
        val compressed = Base64.getEncoder().encodeToString(id.encodeToByteArray())
        SelectMenuBuilder(QUALIFIER + compressed).apply {
            options.addAll(discordOptions)
            allowedValues = this@SelectionMenu.allowedValues
            placeholder = this@SelectionMenu.placeholder
        }
    }

    private fun <T : Any> missingOptional(value: T?): Optional<T> = if (value == null) Optional() else Optional(value)


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SelectionMenu) return false

        if (plugin != other.plugin) return false
        if (!options.contentEquals(other.options)) return false
        if (allowedValues != other.allowedValues) return false
        if (placeholder != other.placeholder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = plugin.hashCode()
        result = 31 * result + options.contentHashCode()
        result = 31 * result + randomId.hashCode()
        result = 31 * result + allowedValues.hashCode()
        result = 31 * result + (placeholder?.hashCode() ?: 0)
        return result
    }


    @OptIn(InternalKotlinCordApi::class)
    companion object : CordKoinComponent {
        const val DELIMITER = "|:|"

        val QUALIFIER: String = "cord:${getKoin().get<String>(qualifier("instanceId"))}:"

        val log by logger("Buttons")
    }
}

@KordPreview
data class SelectionOption(
    val label: String,
    val value: String,
    val description: String? = null,
    val emoji: DiscordPartialEmoji? = null,
    val default: Boolean? = null,
    val onClick: suspend SelectionOptionClickContext.() -> Unit
) {
    val discordOptionBuilder by lazy {
        SelectOptionBuilder(label, value).apply {
            description = this@SelectionOption.description
            emoji = this@SelectionOption.emoji
            default = this@SelectionOption.default
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SelectionOption) return false

        if (label != other.label) return false
        if (value != other.value) return false
        if (description != other.description) return false
        if (emoji != other.emoji) return false
        if (default != other.default) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (emoji?.hashCode() ?: 0)
        result = 31 * result + (default?.hashCode() ?: 0)
        return result
    }
}

class SelectionMenuBuilder {
    var options = mutableListOf<SelectionOption>()
    var allowedValues: IntRange = 1..1
    var placeholder: String? = null

    fun option(label: String, value: String, block: SelectionOptionBuilder.() -> Unit) {
        options.add(SelectionOptionBuilder(label, value).apply(block).build())
    }

    fun build(plugin: Plugin) =
        SelectionMenu(plugin, options.toTypedArray(), allowedValues, placeholder)
}

class SelectionOptionBuilder(var label: String, var value: String) {
    var description: String? = null
    var emoji: DiscordPartialEmoji? = null
    var default: Boolean? = null
    var onClick: (suspend SelectionOptionClickContext.() -> Unit) = {}

    fun onClick(block: suspend SelectionOptionClickContext.() -> Unit) {
        onClick = block
    }

    fun build() = SelectionOption(label, value, description, emoji, default, onClick)
}

data class SelectionOptionClickContext(
    val event: InteractionCreateEvent,
    val interaction: SelectMenuInteraction,
    val selected: Array<SelectionOption>
) {
    val kord get() = event.kord
    val shard get() = event.shard

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SelectionOptionClickContext) return false

        if (event != other.event) return false
        if (interaction != other.interaction) return false
        if (!selected.contentEquals(other.selected)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = event.hashCode()
        result = 31 * result + interaction.hashCode()
        result = 31 * result + selected.contentHashCode()
        return result
    }
}