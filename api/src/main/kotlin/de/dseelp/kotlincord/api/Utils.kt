/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.CommandNode
import de.dseelp.kotlincord.api.command.Sender
import de.dseelp.kotlincord.api.interactions.ButtonAction
import de.dseelp.kotlincord.api.interactions.SelectionMenu
import de.dseelp.kotlincord.api.interactions.SelectionMenuBuilder
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.utils.koin.CordKoinContext
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.*
import dev.kord.core.behavior.interaction.ComponentInteractionBehavior
import dev.kord.core.behavior.interaction.EphemeralInteractionResponseBehavior
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.interaction.UpdateMessageInteractionResponseCreateBuilder
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import java.io.ByteArrayOutputStream
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.round
import kotlin.random.Random

inline fun <reified T : Any> merge(vararg arrays: Array<T>): Array<T> {
    var result: Array<T?> = arrayOfNulls(0)
    for (array in arrays) {
        result += array
    }
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}

@JvmName("mergeNullable")
inline fun <reified T> merge(vararg arrays: Array<T>): Array<T?> {
    var result: Array<T?> = arrayOfNulls(0)
    for (array in arrays) {
        result += array
    }
    @Suppress("UNCHECKED_CAST")
    return result
}

fun validateVersion(versionString: String, lazyMessage: () -> String) {
    try {
        Version.parse(versionString).toString()
    } catch (t: Throwable) {
        throw IllegalStateException(lazyMessage.invoke(), t)
    }
}

fun randomAlphanumeric(
    size: Int,
    lowercase: Boolean = true,
    uppercase: Boolean = true,
    numbers: Boolean = true,
    custom: Array<Char> = arrayOf(),
    random: Random = Random
): String {
    val lowerString = "abcdefghijklmnopqrstuvwxyz"
    val upperString = lowerString.uppercase()
    val numberString = "01234567890"
    val lower = lowerString.toCharArray().toTypedArray()
    val upper = upperString.toCharArray().toTypedArray()
    val numberArray = numberString.toCharArray().toTypedArray()
    var allowed = custom
    if (lowercase) allowed += lower
    if (uppercase) allowed += upper
    if (numbers) allowed += numberArray
    if (allowed.isEmpty()) throw IllegalArgumentException("You must allow some sort of characters!")
    var array = arrayOf<Char>()
    for (i in 1..size) {
        val c = allowed[random.nextInt(allowed.lastIndex)]
        array += c
    }
    return array.joinToString("")
}

fun gzip(content: String): ByteArray {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
    return bos.toByteArray()
}

fun ungzip(content: ByteArray): String =
    GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }

typealias Node = CommandNode<Sender>
typealias Dispatcher = CommandDispatcher<Sender>

fun isValidUrl(urlString: String): Boolean {
    try {
        val regex: Pattern =
            Pattern.compile("^(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
        val matcher: Matcher = regex.matcher(urlString)
        if (!matcher.find()) {
            throw URISyntaxException(urlString, "")
        }
        val url = URL(urlString)
        url.toURI()
    } catch (e: MalformedURLException) {
        return false
    } catch (e: URISyntaxException) {
        return false
    }
    return true
}

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

class VersionColumnType : VarCharColumnType(), IColumnType {
    override fun notNullValueToDB(value: Any): Any {
        return when (value) {
            is String -> value
            is Version -> value.toString()
            else -> error("Unexpected value: $value of ${value::class.qualifiedName}")
        }
    }

    override fun valueFromDB(value: Any): Any {
        return when (value) {
            is Version -> value
            is String -> Version.parse(value)
            else -> valueFromDB(value.toString())
        }
    }
}

fun Table.version(name: String): Column<Version> = registerColumn(name, VersionColumnType())

val String.asSnowflake
    get() = Snowflake(this)

val Long.asSnowflake
    get() = Snowflake(this)

val Instant.asSnowflake
    get() = Snowflake(this)

suspend fun Member.checkPermissions(vararg permissions: Permission) = checkPermissions(Permissions {
    permissions.onEach { +it }
})

suspend fun Member.checkPermissions(permissions: Permissions) = getPermissions().contains(Permission.Administrator) || getPermissions().contains(permissions)

suspend fun Member.checkPermissions(channel: GuildChannel, vararg permissions: Permission) = checkPermissions(channel, Permissions {
    permissions.onEach { +it }
})

suspend fun Member.checkPermissions(channel: GuildChannel, permissions: Permissions) = channel.getEffectivePermissions(id).let {
    it.contains(permissions) || it.contains(Permission.Administrator)
}

@KordPreview
@OptIn(ExperimentalContracts::class)
suspend fun ComponentInteractionBehavior.acknowledgeEphemeralUpdateCreate(
    builder: UpdateMessageInteractionResponseCreateBuilder.() -> Unit
): EphemeralInteractionResponseBehavior {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }

    val request = UpdateMessageInteractionResponseCreateBuilder(
        flags = MessageFlags(MessageFlag.Ephemeral)
    ).apply(builder).toRequest()

    kord.rest.interaction.createInteractionResponse(
        id,
        token,
        request
    )

    return EphemeralInteractionResponseBehavior(applicationId, token, kord)
}

@OptIn(KordPreview::class)
fun ActionRowBuilder.action(
    action: ButtonAction,
    style: ButtonStyle,
    command: String,
    label: String? = null,
    emoji: DiscordPartialEmoji? = null
) {
    if (style == ButtonStyle.Link) throw UnsupportedOperationException("Link Buttons are not support as actions!")
    val id = action.id + ButtonAction.DELIMITER + command
    val compressed = Base64.getEncoder().encodeToString(id.encodeToByteArray())
    interactionButton(style, ButtonAction.QUALIFIER + compressed) {
        this.label = label
        this.emoji = emoji
    }
}

@OptIn(KordPreview::class)
fun ActionRowBuilder.selectionMenu(
    selectionMenu: SelectionMenu
) {
    components.add(selectionMenu.discordComponentBuilder)
}

@OptIn(KordPreview::class)
fun ActionRowBuilder.selectionMenu(
    plugin: Plugin,
    block: SelectionMenuBuilder.() -> Unit
) {
    val menu = plugin.registerSelectionMenu(block)
    selectionMenu(menu)
}

@OptIn(InternalKotlinCordApi::class)
val bot: Bot
    get() = CordKoinContext.app!!.koin.get()