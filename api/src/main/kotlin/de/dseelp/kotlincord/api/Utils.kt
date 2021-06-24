/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.CommandNode
import de.dseelp.kotlincord.api.command.Sender
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import java.io.ByteArrayOutputStream
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.StandardCharsets.UTF_8
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.round
import kotlin.random.Random


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
    custom: Array<Char> = arrayOf()
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
        val c = allowed[Random.nextInt(allowed.lastIndex)]
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
