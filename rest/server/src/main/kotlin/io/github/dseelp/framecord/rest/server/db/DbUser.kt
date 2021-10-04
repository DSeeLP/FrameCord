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

package io.github.dseelp.framecord.rest.server.db

import io.github.dseelp.framecord.rest.data.objects.User
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class DbUser(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<DbUser>(DbUsers)

    var name by DbUsers.name
    var discriminator by DbUsers.discriminator
    var avatarHash by DbUsers.avatarHash
    var refreshToken by DbUsers.refreshToken
    var accessToken by DbUsers.accessToken
    var expirationTime by DbUsers.expirationTime
    var lastLogin by DbUsers.lastLogin
    var permissions by DbPermission via DbPermissionLink
    /*val guildIds
        get() = ungzip(Base64.getDecoder().decode(guilds)).split(";").map { it.toLong() }*/

    var guildIds: List<Long>
        get() {
            return DbGuildLink.select { DbGuildLink.user eq id }.map { it[DbGuildLink.guildId] }
        }
        set(value) {
            DbGuildLink.deleteWhere { DbGuildLink.user eq id }
            DbGuildLink.batchInsert(value) {
                this[DbGuildLink.user] = id
                this[DbGuildLink.guildId] = it
            }
        }

    /*fun setGuildIds(ids: List<Long>) {
        guilds = Base64.getEncoder().encodeToString(gzip(ids.joinToString(";")))
    }*/

    fun addPermissions(vararg permissions: DbPermission) {
        this.permissions = SizedCollection(this.permissions + permissions)
    }

    fun removePermissions(vararg permissions: DbPermission) {
        this.permissions = SizedCollection(this.permissions - permissions)
    }

    val clientUser: User
        get() = transaction {
            User(
                this@DbUser.id.value,
                name,
                discriminator,
                "UNSUPPORTED", //TODO: Add real avatar url here
                permissions.toSimplePermissions().toTypedArray()
            )
        }
}

object DbUsers : LongIdTable("users") {
    val name = varchar("name", 32)
    val discriminator = integer("discriminator")
    val avatarHash = text("avatarUrl", eagerLoading = true).nullable()
    val refreshToken = text("refreshToken", eagerLoading = true)
    val accessToken = text("accessToken", eagerLoading = true)
    val expirationTime = long("expirationTime")
    val lastLogin = long("lastLogin")
    //val guilds = text("guilds")
}
