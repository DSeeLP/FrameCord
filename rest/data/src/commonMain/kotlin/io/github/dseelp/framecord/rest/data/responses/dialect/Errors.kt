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

object RestErrors {
    val Unauthorized = FullRestError(HttpStatusCode.Unauthorized, 401, "Not authorized")
    val Forbidden = FullRestError(HttpStatusCode.Forbidden, 403, "Forbidden")
    val NotFound = FullRestError(HttpStatusCode.NotFound, 404, "Not found")
    val InternalServerError = FullRestError(HttpStatusCode.InternalServerError, 500, "An internal error occurred")
}

infix fun RestError.matches(other: RestError): Boolean {
    val idMatches = id == other.id
    if (!idMatches) return false
    val p = getPermission()
    val op = other.getPermission()
    if (p != null && op != null) return p == op
    if (p == null && op != null) return false
    if (p != null && op == null) return true
    return false
}

private fun RestError.getPermission(): Int? {
    return when (this) {
        is PermissionRestError -> missingPermission
        is DetailedPermissionRestError -> missingPermission
        is FullPermissionRestError -> missingPermission
        is FullDetailedPermissionRestError -> missingPermission
        else -> null
    }
}