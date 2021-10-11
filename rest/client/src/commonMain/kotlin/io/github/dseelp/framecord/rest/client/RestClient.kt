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

import io.github.dseelp.framecord.rest.client.response.FailedRestResult
import io.github.dseelp.framecord.rest.client.response.FineRestResult
import io.github.dseelp.framecord.rest.client.response.RestResult
import io.github.dseelp.framecord.rest.client.response.fine
import io.github.dseelp.framecord.rest.data.objects.Permission
import io.github.dseelp.framecord.rest.data.objects.User
import io.github.dseelp.framecord.rest.data.responses.PermissionResponse
import io.github.dseelp.framecord.rest.data.responses.UserResponse
import io.github.dseelp.framecord.rest.data.responses.dialect.RestErrors
import io.github.dseelp.framecord.rest.data.responses.dialect.matches
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class RestClient(
    val baseUrl: Url,
    val customBuilder: HttpClientConfig<*>.() -> Unit = {},
) {
    lateinit var deviceToken: String
        private set

    lateinit var httpClient: HttpClient
        private set

    var isAuthorized: Boolean = false
        private set

    lateinit var user: User
        private set

    private fun buildClient(token: String) = HttpClient {
        kotlinx.serialization.json.Json
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            })
        }
        customBuilder(this)
        install(HttpCookies) {
            default {
                addCookie(baseUrl, Cookie("deviceId", token, httpOnly = true))
            }
        }
        defaultRequest {
            host = baseUrl.host
            port = baseUrl.port
        }
        expectSuccess = false
    }

    suspend fun authorizeToken(deviceToken: String): Boolean {
        val client = buildClient(deviceToken)
        val response = client.get<HttpResponse>() {
            url {
                pathComponents("security", "permissions")
            }
        }
        val result = response.toResult<PermissionResponse>()
        return if (result is FineRestResult) {
            Permission.getPermissions = { result.response.permissions }
            httpClient = client
            this.deviceToken = deviceToken
            this.isAuthorized = true
            user = getUser().fine.response.user
            true
        } else false
    }

    @OptIn(ExperimentalContracts::class)
    internal inline fun <T> checkResult(result: RestResult<T>, action: (result: RestResult<T>) -> Unit) {
        contract {
            callsInPlace(action, InvocationKind.AT_MOST_ONCE)
        }
        return if (result is FailedRestResult && result.error matches RestErrors.Unauthorized) {
            isAuthorized = false
            deviceToken = ""
        } else action(result)
    }

    internal fun checkAuthorization() {
        if (!isAuthorized) throw IllegalStateException("RestClient not authorized!")
    }

    private suspend fun updatePermisssionsUnsafe() {
        val response = httpClient.get<HttpResponse>() {
            url {
                pathComponents("security", "permissions")
            }
        }
        val result = response.toResult<PermissionResponse>()
        if (result is FineRestResult) {
            Permission.getPermissions = { result.response.permissions }
            user = getUser().fine.response.user
        }
    }

    suspend fun updatePermissions() {
        checkAuthorization()
        updatePermisssionsUnsafe()
    }

    suspend fun getUser(): RestResult<UserResponse> {
        checkAuthorization()
        val response = httpClient.get<HttpResponse> {
            url {
                //takeFrom(baseUrl)
                pathComponents("security")
            }
        }
        return response.toResult()
    }

    suspend fun invalidateSession() {
        checkAuthorization()
        val response = httpClient.delete<HttpResponse> {
            url {
                //takeFrom(baseUrl)
                pathComponents("security")
            }
        }
        val result = response.toResult<Unit>()
        if (result is FineRestResult) {
            isAuthorized = true
            deviceToken = ""
        }
    }
}