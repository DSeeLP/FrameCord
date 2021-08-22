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

import de.dseelp.oauth2.discord.api.DiscordSession
import de.dseelp.oauth2.discord.api.utils.Scope
import dev.kord.common.entity.Permission
import dev.kord.core.entity.Guild
import io.github.dseelp.framecord.api.asSnowflake
import io.github.dseelp.framecord.api.bot
import io.github.dseelp.framecord.api.checkPermissions
import io.github.dseelp.framecord.rest.data.objects.User
import io.github.dseelp.framecord.rest.server.db.DbUser
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toCollection
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun User.getAdminGuilds(): List<Guild> {
    return newSuspendedTransaction {
        val db = DbUser.findById(this@getAdminGuilds.id) ?: throw IllegalStateException("User is not in database")
        val idLong = db.id.value
        val id = idLong.asSnowflake
        val mutualGuilds = mutableListOf<Guild>()
        bot.kord.guilds.filter { it.getMemberOrNull(id) != null }.toCollection(mutualGuilds)
        val adminGuilds = mutualGuilds.filter { it.getMember(id).checkPermissions(Permission.Administrator) }
        return@newSuspendedTransaction adminGuilds
    }
}

suspend fun DbUser.refreshUser() = newSuspendedTransaction {
    val session = DiscordSession(
        RestServer.oauthHttp2Client,
        accessToken,
        refreshToken,
        arrayOf(Scope.IDENTIFY, Scope.GUILDS),
        "token",
        Instant.fromEpochMilliseconds(expirationTime)
    ).refresh()
    session.getGuilds().filter { guild -> guild.permissions.any { perm -> perm.offset == 3 } }.map { id.toLong() }
    val dcUser = session.getUser()
    val user = this@refreshUser
    user.accessToken = session.accessToken
    user.refreshToken = session.refreshToken
    user.name = dcUser.username
    user.discriminator = dcUser.discriminator.toInt()
    user.avatarHash = dcUser.avatar
}