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

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import io.github.dseelp.framecord.api.InternalFrameCordApi
import io.github.dseelp.framecord.api.asSnowflake
import io.github.dseelp.framecord.api.bot
import io.github.dseelp.framecord.api.modules.ModuleManager
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import io.github.dseelp.framecord.rest.data.responses.GuildResponse
import io.github.dseelp.framecord.rest.data.responses.MultipleGuildResponse
import io.github.dseelp.framecord.rest.data.responses.dialect.RestErrors
import io.github.dseelp.framecord.rest.data.responses.dialect.detailed
import io.github.dseelp.framecord.rest.server.client
import io.github.dseelp.framecord.rest.server.ok
import io.github.dseelp.framecord.rest.server.respondError
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toCollection
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.inject

@OptIn(InternalFrameCordApi::class)
object GuildsModule : CordKoinComponent {
    val moduleManager: ModuleManager by inject()
    fun Application.guildModule() {
        fun ApplicationCall.guildId(): Long? = parameters["gid"]?.toLongOrNull()
        suspend fun ApplicationCall.checkGuildId(): Snowflake? {
            val id = guildId()
            if (id == null) {
                respondError(RestErrors.NotFound.detailed("No guild Id specified or it is invalid"))
                return null
            }
            val result = transaction {
                val user = getDbUser() ?: return@transaction null
                user.guildIds.firstOrNull { it == id } != null
            }
            return if (result == null || result == false) {
                respondError(RestErrors.Forbidden.detailed("You don't have access to that guild!"))
                null
            } else id.asSnowflake
        }

        suspend fun ApplicationCall.guild(): Guild? = checkGuildId()?.let { bot.kord.getGuild(it) }
        routing {
            authenticate("user") {
                route("guilds") {
                    get {
                        newSuspendedTransaction {
                            val guildIds = call.getDbUser()!!.guildIds
                            call.ok(MultipleGuildResponse(bot.kord.guilds.filter { guild -> guild.id.value in guildIds }
                                .map { it.client }.toCollection(mutableListOf()).toTypedArray()))
                        }
                    }
                    route("{gid}") {
                        get {
                            val guild = call.guild() ?: return@get
                            //val dbGuild = DbGuild.findById(guild.id.value)!!
                            call.ok(GuildResponse(guild.client))
                        }
                        route("modules") {
                            get {
                                val guild = call.guild() ?: return@get
                                moduleManager.getEnabledModules(guild.id)
                            }
                        }
                    }
                }
            }
        }
    }
}