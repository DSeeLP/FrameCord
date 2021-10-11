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

package io.github.dseelp.framecord.rest.server

import dev.kord.core.entity.Guild
import io.github.dseelp.framecord.rest.data.responses.dialect.FullRestError
import io.github.dseelp.framecord.rest.data.responses.dialect.RestError
import io.github.dseelp.framecord.rest.data.responses.dialect.RestResponse
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

suspend inline fun ApplicationCall.respondError(error: RestError) {
    respondApi(RestResponse<Unit>(error, null))
}

suspend inline fun <reified T> ApplicationCall.respondValue(value: T, status: HttpStatusCode) =
    respondApi(RestResponse(null, value), status)

suspend inline fun <reified T> ApplicationCall.respondApi(
    response: RestResponse<T>,
    httpStatus: HttpStatusCode? = null
) {
    val nResponse = response.serializable()
    val content = (JsonObject::class.java.getDeclaredField("content").apply { isAccessible = true }
        .get(RestServer.json.encodeToJsonElement(nResponse)) as Map<String, JsonElement>)
    val type: (type: String) -> JsonObject = { JsonObject(/*mapOf("type" to JsonPrimitive(it)) + */content) }
    when {
        nResponse.error != null && response.error is FullRestError -> respond(
            (response.error as FullRestError).httpStatus,
            type("failed")
        )
        nResponse.error != null && httpStatus != null -> respond(httpStatus, type("failed"))
        else -> respond(httpStatus ?: HttpStatusCode.NotImplemented, type("ok"))
    }
}

suspend fun ApplicationCall.ok() = respondValue(Unit, HttpStatusCode.OK)
suspend inline fun <reified T : Any> ApplicationCall.ok(value: T) = respondValue(value, HttpStatusCode.OK)

val Guild.client
    get() = io.github.dseelp.framecord.rest.data.objects.Guild(
        id.value, ownerId.value, name, iconHash
    )