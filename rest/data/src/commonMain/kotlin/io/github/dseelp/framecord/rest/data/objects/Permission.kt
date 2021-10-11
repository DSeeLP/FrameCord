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

package io.github.dseelp.framecord.rest.data.objects

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@Serializable
sealed class Permission {
    abstract val offset: Int

    val raw: Long
        get() = 1L shl offset

    companion object {
        operator fun invoke(id: Int) = SimplePermission(id)

        fun isApplied(effectivePermissions: Long, vararg permissions: Permission) =
            isApplied(effectivePermissions, permissions.raw)

        fun isApplied(effectivePermissions: Long, rawPerms: Long) = (effectivePermissions and rawPerms) == rawPerms

        fun getRaw(vararg permissions: Permission): Long {
            var raw = 0L
            for (permission in permissions) {
                if (permission != unknownPermission) raw = raw or permission.raw
            }
            return raw
        }

        fun getAllApplied(effectiveFlags: Long): Array<Permission> {
            val applied = mutableListOf<Permission>()
            for (value in getPermissions()) {
                if (isApplied(effectiveFlags, value.raw)) applied.add(value)
            }
            return applied.toTypedArray()
        }

        var getPermissions: () -> Array<FullPermission> = { arrayOf() }
    }
}

@Serializable
@SerialName("simple")
data class SimplePermission(override val offset: Int) : Permission()

@Serializable
@SerialName("full")
data class FullPermission(override val offset: Int, val name: String, val description: String? = null) : Permission()

fun Array<out Permission>.toFullPermissions(): Array<FullPermission> {
    return map {
        when (it) {
            is SimplePermission -> FullPermission(it.offset, "")
            is FullPermission -> it
        }
    }.toTypedArray()
}

fun List<out Permission>.toFullPermissions(): List<FullPermission> {
    return map {
        when (it) {
            is SimplePermission -> FullPermission(it.offset, "")
            is FullPermission -> it
        }
    }
}

val unknownPermission = Permission(-1)

val Array<out Permission>.raw
    get() = Permission.getRaw(*this)

val Array<out Permission>.bitwise: Long
    get() {
        val mapped = filter { it != unknownPermission }.map { it.raw }
        var result = 0L
        for (l in mapped) {
            result = result or l
        }
        return result
    }


object PermissionArraySerializer : KSerializer<Array<out Permission>> {
    override fun deserialize(decoder: Decoder): Array<out Permission> {
        return Permission.getAllApplied(decoder.decodeLong())
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("permisisons", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Array<out Permission>) {
        encoder.encodeLong(value.bitwise)
    }
}
