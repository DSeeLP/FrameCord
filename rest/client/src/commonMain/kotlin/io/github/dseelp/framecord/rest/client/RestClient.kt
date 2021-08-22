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

package io.github.dseelp.framecord.rest.client

import io.github.dseelp.framecord.rest.client.response.RestResult
import io.github.dseelp.framecord.rest.data.responses.UserResponse
import io.ktor.client.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class RestClient(
    val baseUrl: Url, val deviceToken: String, val httpClient: HttpClient = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(HttpCookies) {
            this.default {
                addCookie(baseUrl, Cookie("deviceId", deviceToken, httpOnly = true))
            }
        }
        expectSuccess = false
    }
) {
    suspend fun getUser(): RestResult<UserResponse> {
        val response = httpClient.get<HttpResponse> {
            url {
                takeFrom(baseUrl)
                pathComponents("security")
            }
        }
        return response.toResult()
    }

    suspend fun invalidateSession() {
        val response = httpClient.delete<HttpResponse> {
            url {
                takeFrom(baseUrl)
                pathComponents("security")
            }
        }
        println(response.toResult<Unit>())
    }
}