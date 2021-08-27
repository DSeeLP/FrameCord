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

package io.github.dseelp.framecord.rest.data.responses.dialect

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed class RestError {
    abstract val id: Int
    abstract val message: String
}

fun RestError?.serializable(): RestError? {
    if (this == null) return null
    return if (this is UnserializableRestError) toSerializable()
    else this
}

@Serializable
@SerialName("simple")
open class SimpleRestError(override val id: Int, override val message: String) : RestError() {
    override fun toString(): String {
        return "SimpleRestError(id=$id, message='$message')"
    }
}

@Serializable
@SerialName("detailed")
open class DetailedRestError(override val id: Int, override val message: String, open val details: String) :
    RestError() {
    override fun toString(): String {
        return "DetailedRestError(id=$id, message='$message', details='$details')"
    }
}

@Serializable
@SerialName("detailed-permission")
open class DetailedPermissionRestError(
    override val id: Int,
    override val message: String,
    open val details: String,
    open val missingPermission: Int
) : RestError() {
    override fun toString(): String {
        return "DetailedPermissionRestError(id=$id, message='$message', details='$details', missingPermission=$missingPermission)"
    }
}

@Serializable
@SerialName("permission")
open class PermissionRestError(override val id: Int, override val message: String, open val missingPermission: Int) :
    RestError() {
    override fun toString(): String {
        return "PermissionRestError(id=$id, message='$message', missingPermission=$missingPermission)"
    }
}

class FullRestError(val httpStatus: HttpStatusCode, override val id: Int, override val message: String) : RestError(),
    UnserializableRestError {
    override fun toSerializable(): RestError = SimpleRestError(id, message)
    override fun toString(): String {
        return "FullRestError(httpStatus=$httpStatus, id=$id, message='$message')"
    }
}

class FullDetailedRestError(val httpStatus: HttpStatusCode, id: Int, message: String, details: String) :
    DetailedRestError(id, message, details), UnserializableRestError {
    override fun toSerializable(): RestError = DetailedRestError(id, message, details)
    override fun toString(): String {
        return "FullDetailedRestError(httpStatus=$httpStatus, id=$id, message='$message', details='$details')"
    }
}

class FullPermissionRestError(val httpStatus: HttpStatusCode, id: Int, message: String, missingPermission: Int) :
    PermissionRestError(id, message, missingPermission), UnserializableRestError {
    override fun toSerializable(): RestError = PermissionRestError(id, message, missingPermission)
    override fun toString(): String {
        return "FullPermissionRestError(httpStatus=$httpStatus, id=$id, message='$message', missingPermission=$missingPermission)"
    }
}

class FullDetailedPermissionRestError(
    val httpStatus: HttpStatusCode,
    id: Int,
    message: String,
    details: String,
    missingPermission: Int
) :
    DetailedPermissionRestError(id, message, details, missingPermission), UnserializableRestError {
    override fun toSerializable(): RestError = DetailedPermissionRestError(id, message, details, missingPermission)
    override fun toString(): String {
        return "FullDetailedPermissionRestError(httpStatus=$httpStatus, id=$id, message='$message', details='$details', missingPermission=$missingPermission)"
    }
}


fun FullRestError.detailed(details: String) = FullDetailedRestError(httpStatus, id, message, details)
fun FullPermissionRestError.detailed(details: String) =
    FullDetailedPermissionRestError(httpStatus, id, message, details, missingPermission)

fun FullRestError.permission(missingPermission: Int) =
    FullPermissionRestError(httpStatus, id, message, missingPermission)

fun FullDetailedRestError.permission(missingPermission: Int) =
    FullDetailedPermissionRestError(httpStatus, id, message, details, missingPermission)

fun RestError.full(statusCode: HttpStatusCode): RestError = when (this) {
    is SimpleRestError -> FullRestError(statusCode, id, message)
    is DetailedRestError -> FullDetailedRestError(statusCode, id, message, details)
    is DetailedPermissionRestError -> FullDetailedPermissionRestError(
        statusCode,
        id,
        message,
        details,
        missingPermission
    )
    is PermissionRestError -> FullPermissionRestError(statusCode, id, message, missingPermission)
    is FullRestError -> this
}

fun SimpleRestError.full(statusCode: HttpStatusCode): FullRestError = FullRestError(statusCode, id, message)
fun DetailedRestError.full(statusCode: HttpStatusCode): FullDetailedRestError =
    FullDetailedRestError(statusCode, id, message, details)

fun DetailedPermissionRestError.full(statusCode: HttpStatusCode): FullDetailedPermissionRestError =
    FullDetailedPermissionRestError(statusCode, id, message, details, missingPermission)

fun PermissionRestError.full(statusCode: HttpStatusCode): FullPermissionRestError =
    FullPermissionRestError(statusCode, id, message, missingPermission)

interface UnserializableRestError {
    fun toSerializable(): RestError
}