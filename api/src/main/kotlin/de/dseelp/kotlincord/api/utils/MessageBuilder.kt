/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.utils

import de.dseelp.kotlincord.api.buttons.ButtonAction
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import net.dv8tion.jda.api.entities.Emoji
import net.dv8tion.jda.api.entities.Message.MentionType
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import net.dv8tion.jda.api.interactions.components.ButtonStyle
import java.awt.Color
import java.util.*

fun messageBuilder(block: MessageBuilder.() -> Unit) = MessageBuilder().apply(block).build()

fun embed(block: EmbedBuilder.() -> Unit) = embedBuilder(block).build()

fun embedBuilder(block: EmbedBuilder.() -> Unit) = EmbedBuilder().apply(block)

class MessageBuilder {
    var isTTS = false
    private var _embed: MessageEmbed? = null
    var embed
        get() = _embed ?: throw IllegalStateException("The embed for this message isn't set!")
        set(value) {
            _embed = value
        }

    fun embed(block: EmbedBuilder.() -> Unit) {
        embed = de.dseelp.kotlincord.api.utils.embed(block)
    }

    var content: String = ""

    var allowedMentions = MentionType.values()

    fun mentions(block: MentionBuilder.() -> Unit) {
        allowedMentions = MentionBuilder().apply(block).allowed.toTypedArray()
    }

    var mentionedUsers = arrayOf<String>()
    var mentionedRoles = arrayOf<String>()

    fun mentionUsers(vararg users: String) {
        mentionedUsers += users
    }

    fun mentionRoles(vararg roles: String) {
        mentionedRoles += roles
    }

    fun mentionRoles(vararg roles: Long) = mentionRoles(*roles.map { it.toString() }.toTypedArray())
    fun mentionUsers(vararg roles: Long) = mentionUsers(*roles.map { it.toString() }.toTypedArray())

    val actionRows = mutableListOf<ActionRow>()

    fun actionRow(block: ActionRowBuilder.() -> Unit) {
        actionRows.add(ActionRowBuilder().apply(block).build())
    }

    class ActionRowBuilder {
        val buttons = mutableListOf<Button>()
        operator fun Button.unaryPlus() {
            buttons.add(this)
        }

        fun action(
            action: ButtonAction,
            style: ButtonStyle,
            command: String,
            label: String? = null,
            emoji: Emoji? = null
        ) {
            val id = action.id + ButtonAction.DELIMITER + command
            val compressed = Base64.getEncoder().encodeToString(id.encodeToByteArray())
            +Button.of(
                style,
                ButtonAction.QUALIFIER + compressed,
                label,
                emoji
            )
        }

        fun build() = ActionRow.of(buttons)
    }

    class MentionBuilder {
        var allowed = mutableListOf<MentionType>()
        fun allow(type: MentionType) {
            allowed.add(type)
        }

        fun allowUsers() = allow(MentionType.USER)
        fun allowRoles() = allow(MentionType.ROLE)
        fun allowChannels() = allow(MentionType.CHANNEL)
        fun allowEmotes() = allow(MentionType.EMOTE)
        fun allowHere() = allow(MentionType.HERE)
        fun allowEveryone() = allow(MentionType.EVERYONE)
    }

    fun build(): net.dv8tion.jda.api.MessageBuilder {
        val jda = net.dv8tion.jda.api.MessageBuilder()
        jda.setTTS(isTTS)
        if (_embed != null) jda.setEmbed(_embed)
        jda.setAllowedMentions(allowedMentions.toMutableList())
        jda.setActionRows(actionRows)
        jda.setContent(content)
        return jda
    }
}

fun embedField(block: EmbedBuilder.Field.FieldBuilder.() -> Unit) =
    EmbedBuilder.Field.FieldBuilder().apply(block).build()

fun embedAuthor(block: EmbedBuilder.Author.AuthorBuilder.() -> Unit) =
    EmbedBuilder.Author.AuthorBuilder().apply(block).build()

class EmbedBuilder {
    private var _title: String? = null
    var title: String
        get() = _title ?: throw IllegalStateException("The title for this embed isn't set!")
        set(value) {
            _title = value
        }

    private var _description: String? = null
    var description: String
        get() = _description ?: throw IllegalStateException("The description for this embed isn't set!")
        set(value) {
            _description = value
        }

    private var _url: String? = null
    var url: String
        get() = _url ?: throw IllegalStateException("The url for this embed isn't set!")
        set(value) {
            _url = value
        }

    private var _image: String? = null
    var image: String
        get() = _image ?: throw IllegalStateException("The image for this embed isn't set!")
        set(value) {
            _image = value
        }

    private var _color: Color? = null
    var color: Color
        get() = _color ?: throw IllegalStateException("The color for this embed isn't set!")
        set(value) {
            _color = value
        }

    private var _timestamp: Instant? = null
    var timestamp: Instant
        get() = _timestamp ?: throw IllegalStateException("The timestamp for this embed isn't set!")
        set(value) {
            _timestamp = value
        }

    private var _thumbnail: String? = null
    var thumbnail: String
        get() = _thumbnail ?: throw IllegalStateException("The thumbnail for this embed isn't set!")
        set(value) {
            _thumbnail = value
        }

    private var _author: Author? = null
    var author: Author
        get() = _author ?: throw IllegalStateException("The author for this embed isn't set!")
        set(value) {
            _author = value
        }

    private var _footer: Footer? = null
    var footer: Footer
        get() = _footer ?: throw IllegalStateException("The footer for this embed isn't set!")
        set(value) {
            _footer = value
        }

    fun author(name: String? = null, url: String? = null, iconUrl: String? = null) {
        author = Author(name, url, iconUrl)
    }

    fun author(block: Author.AuthorBuilder.() -> Unit) {
        author = embedAuthor(block)
    }

    val fields = mutableListOf<Field>()

    fun field(block: Field.FieldBuilder.() -> Unit) = field(Field.FieldBuilder().apply(block).build())

    fun field(title: String, content: String, inline: Boolean = false) = field(Field(title, content, inline))

    fun field(field: Field) {
        fields.add(field)
    }

    fun clearFields() = fields.clear()

    data class Field(val title: String, val content: String, val inline: Boolean = false) {
        val jda = MessageEmbed.Field(title, content, inline)

        class FieldBuilder {
            var title: String = ""
            var content: String = ""
            var inline: Boolean = false
            fun build() = Field(title, content, inline)
        }

        companion object {
            val BLANK = Field("", "")
            val BLANK_INLINE = Field("", "", true)
        }
    }

    data class Author(val name: String? = null, val url: String? = null, val iconUrl: String? = null) {
        val jda = MessageEmbed.AuthorInfo(name, url, iconUrl, null)

        class AuthorBuilder {
            var name: String? = null
            var url: String? = null
            var iconUrl: String? = null
            fun build(): Author = Author(name, url, iconUrl)
        }
    }

    data class Footer(val text: String, val iconUrl: String? = null) {
        val jda = MessageEmbed.Footer(text, iconUrl, null)
    }

    val jda: net.dv8tion.jda.api.EmbedBuilder
        get() {
            val jda = net.dv8tion.jda.api.EmbedBuilder()
            if (_title != null && _url != null) jda.setTitle(_title, _url)
            else if (_title != null) jda.setTitle(_title)
            else if (_url != null) jda.setTitle(null, _title)
            if (_description != null) jda.setDescription(_description)
            if (_image != null) jda.setImage(image)
            if (_color != null) jda.setColor(color)
            if (_timestamp != null) jda.setTimestamp(_timestamp?.toJavaInstant())
            if (_thumbnail != null) jda.setThumbnail(_thumbnail)
            if (_author != null) jda.setAuthor(author.name, author.url, author.iconUrl)
            if (_footer != null) jda.setFooter(_footer?.text, _footer?.iconUrl)
            jda.fields.addAll(fields.map { it.jda })
            return jda
        }

    fun build() = jda.build()
}