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

import de.dseelp.oauth2.discord.api.DiscordClient
import io.github.dseelp.framecord.api.InternalFrameCordApi
import io.github.dseelp.framecord.api.configs.BotConfig
import io.github.dseelp.framecord.api.logging.logger
import io.github.dseelp.framecord.api.plugins.Plugin
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import io.github.dseelp.framecord.rest.data.responses.dialect.RestErrors
import io.github.dseelp.framecord.rest.server.modules.NotAuthenticatedException
import io.github.dseelp.framecord.rest.server.modules.securityModule
import io.github.dseelp.framecord.rest.server.sessions.DeviceSession
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

@OptIn(InternalFrameCordApi::class)
object RestServer : CordKoinComponent {
    fun installApplicationModule(module: Application.() -> Unit) {
        server.application.apply(module)
    }

    fun startRestServer(plugin: Plugin) {
        val config: BotConfig.RestConfig = getKoin().get<BotConfig>().rest
        if (!config.enabled) return
        if (this::restPlugin.isInitialized) return
        restPlugin = plugin
        baseUrl = Url(getKoin().get<BotConfig>().rest.redirectUrl)

        val server = embeddedServer(Netty, host = config.host, port = config.port) {
            install(IgnoreTrailingSlash)
            install(ContentNegotiation) {
                json(json = Json { prettyPrint = true })
            }
            install(CallLogging) {
                this.level = Level.DEBUG
                this.logger = logger<RestServer>().value
            }
            install(StatusPages) {
                this.exception<NotAuthenticatedException> {
                    call.respondError(RestErrors.Unauthorized)
                }

                status(HttpStatusCode.NotFound) {
                    call.respondError(RestErrors.NotFound)
                }
                exception<Throwable> {
                    call.respondError(RestErrors.InternalServerError)
                    throw it
                }
            }
            install(Sessions) {
                cookie<DeviceSession>("deviceId") {
                    this.cookie.secure = baseUrl.protocol == URLProtocol.HTTPS
                    this.cookie.encoding = CookieEncoding.RAW
                }
            }

            securityModule()
        }
        server.start()
        _server = server
    }

    lateinit var baseUrl: Url
        private set

    private var _server: ApplicationEngine? = null

    val server: ApplicationEngine
        get() = _server ?: throw IllegalStateException("The rest server hasn't started yet")

    val oauthHttp2Client: DiscordClient
        get() = getKoin().get()

    internal lateinit var restPlugin: Plugin
        private set
}