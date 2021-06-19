/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api

import de.dseelp.kommon.command.CommandDispatcher
import de.dseelp.kommon.command.CommandNode
import de.dseelp.kotlincord.api.command.Sender
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
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
