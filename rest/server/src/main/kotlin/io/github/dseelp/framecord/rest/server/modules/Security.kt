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

import de.dseelp.oauth2.discord.api.DiscordClient
import de.dseelp.oauth2.discord.api.authentication.DiscordOauth2Response
import de.dseelp.oauth2.discord.api.utils.Scope
import io.github.dseelp.framecord.api.configs.BotConfig
import io.github.dseelp.framecord.rest.data.objects.Permission
import io.github.dseelp.framecord.rest.data.responses.PermissionResponse
import io.github.dseelp.framecord.rest.data.responses.UserResponse
import io.github.dseelp.framecord.rest.data.responses.dialect.RestErrors
import io.github.dseelp.framecord.rest.data.responses.dialect.detailed
import io.github.dseelp.framecord.rest.server.RestServer
import io.github.dseelp.framecord.rest.server.db.*
import io.github.dseelp.framecord.rest.server.refreshUser
import io.github.dseelp.framecord.rest.server.respondError
import io.github.dseelp.framecord.rest.server.respondValue
import io.github.dseelp.framecord.rest.server.sessions.DeviceSession
import io.github.dseelp.framecord.rest.server.utils.UserPrincipal
import io.github.dseelp.framecord.rest.server.utils.principal
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.inject
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

fun Application.securityModule() {
    transaction {
        SchemaUtils.createMissingTablesAndColumns(DbUsers, DbUserSessions, DbPermissions, DbPermissionLink, DbGuildLink)
        DbPermission.findById(1) ?: DbPermission.new(1) {
            this.name = "Admin"
            this.description = "Gives admin access over the entire system"
        }
    }
    val config by RestServer.inject<BotConfig>()
    val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                this.ignoreUnknownKeys = true
            })
        }
        expectSuccess = false
    }
    val redirectUri = URLBuilder(config.rest.redirectUrl).apply {
        pathComponents("security", "oauth2")
    }
    val client = DiscordClient(httpClient, config.clientId.toString(), config.clientSecret, redirectUri.buildString())
    RestServer.loadKoinModules(module {
        single { client }
    })
    val oauth2Link = generateOAuth2Link()
    install(Authentication) {
        session<DeviceSession>("user") {
            validate { credentials ->
                getDbUser()?.clientUser?.principal
            }
            challenge {
                val mode = call.parameters["mode"]?.toIntOrNull()
                when (mode) {
                    2 -> call.respondRedirect(generateOAuth2Link())
                    1 -> call.respondError(RestErrors.Unauthorized)
                    else -> call.respondError(RestErrors.Unauthorized)
                }
            }
        }
    }
    println("Bot OAuth2 Link: $oauth2Link")

    routing {
        route("security") {
            authenticate("user") {
                get {
                    call.respondValue(UserResponse(call.userPrincipal().user), HttpStatusCode.OK)
                }
                get("permissions") {
                    call.respondValue(PermissionResponse(Permission.getPermissions()), HttpStatusCode.OK)
                }
                delete {
                    transaction {
                        call.getUserSession()?.delete()
                    }
                    call.respondValue(Unit, HttpStatusCode.OK)
                }
            }

            client.configureKtorRoute(this, "oauth2", errorHandler = {
                call.respondCodeError()
            }) {
                if (it is DiscordOauth2Response.FailedDiscordOauth2Response) {
                    call.respondCodeError()
                    return@configureKtorRoute
                }
                val session = it.generateSession(client)
                val user = session.getUser()
                val idLong = user.id.toLong()
                val db = newSuspendedTransaction {
                    var created = false
                    val db = DbUser.findById(idLong) ?: run {
                        created = true
                        DbUser.new(idLong) {
                            accessToken = session.accessToken
                            refreshToken = session.refreshToken
                            expirationTime = session.expirationTime.toEpochMilliseconds()
                            lastLogin = System.currentTimeMillis()
                            name = user.username
                            discriminator = user.discriminator.toInt()
                            avatarHash = user.avatar
                        }
                    }
                    if (!created) {
                        db.accessToken = session.accessToken
                        db.refreshToken = session.refreshToken
                    }
                    db
                }
                val userSession = newSuspendedTransaction {
                    println("Request")
                    db.refreshUser()
                    DbUserSession.new {
                        this.user = db
                        this.lastUse = System.currentTimeMillis()
                        this.creationTime = System.currentTimeMillis()
                    }
                }
                call.sessions.set(DeviceSession(userSession.token))
                call.respondValue(Unit, HttpStatusCode.OK)
            }
        }
    }
}

fun generateOAuth2Link(): String {
    return RestServer.oauthHttp2Client.generateAuthorizationUrl(Scope.IDENTIFY, Scope.GUILDS)
}

private suspend fun ApplicationCall.respondCodeError() {
    respondError(RestErrors.Forbidden.detailed("Code invalid"))
}

val ApplicationCall.isAuthenticated: Boolean
    get() = getUserSession() != null

fun ApplicationCall.getUserSession(): DbUserSession? {
    val session = sessions.get<DeviceSession>() ?: return null
    return transaction {
        val s = DbUserSession.find { DbUserSessions.token eq session.token }.firstOrNull() ?: return@transaction null
        if (System.currentTimeMillis() > s.lastUse + TimeUnit.DAYS.toMillis(2)) {
            s.delete()
            return@transaction null
        }
        s.load(DbUserSession::user)
    }
}

fun ApplicationCall.hasPermission(permissionId: Int) =
    getDbUser()?.permissions?.firstOrNull { it.id.value == permissionId } != null

fun ApplicationCall.hasPermission(permission: Permission) = hasPermission(permission.offset)

fun ApplicationCall.getDbUser(): DbUser? {
    return getUserSession()?.let {
        transaction { it.user }
    }
}

fun ApplicationCall.userPrincipal(): UserPrincipal = principal() ?: throw NotAuthenticatedException()

class NotAuthenticatedException : Exception()