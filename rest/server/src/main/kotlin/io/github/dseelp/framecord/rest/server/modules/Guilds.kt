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

package io.github.dseelp.framecord.rest.server.modules

import io.github.dseelp.framecord.rest.data.responses.dialect.RestErrors
import io.github.dseelp.framecord.rest.data.responses.dialect.detailed
import io.github.dseelp.framecord.rest.server.ok
import io.github.dseelp.framecord.rest.server.respondError
import io.ktor.application.*
import io.ktor.routing.*

fun Application.guildModule() {
    fun ApplicationCall.guildId(): Long? = parameters["gid"]?.toLongOrNull()
    suspend fun ApplicationCall.noGuildIdError() =
        respondError(RestErrors.NotFound.detailed("No guild Id specified or it is invalid"))
    routing {
        route("guilds") {
            route("{gid}") {
                get {
                    val guildId = call.guildId()
                    if (guildId == null) {
                        call.noGuildIdError()
                        return@get
                    }
                    call.ok()
                }
            }
        }
    }
}