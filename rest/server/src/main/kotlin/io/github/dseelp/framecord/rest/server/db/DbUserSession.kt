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

import io.github.dseelp.framecord.api.randomAlphanumeric
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.security.SecureRandom
import java.util.*
import kotlin.random.asKotlinRandom

class DbUserSession(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<DbUserSession>(DbUserSessions)

    var user by DbUser referencedOn DbUserSessions.user
    var lastUse by DbUserSessions.lastUse
    var creationTime by DbUserSessions.creationTime
    var token by DbUserSessions.token
}

val secureJRandom = SecureRandom()
val secureRandom = secureJRandom.asKotlinRandom()

object DbUserSessions : UUIDTable("userSessions") {
    val user = reference("user", DbUsers)
    val lastUse = long("lastUse")
    val creationTime = long("creationTime")
    val token = text("token").uniqueIndex().apply {
        defaultValueFun = {
            randomAlphanumeric(128, random = secureRandom)
        }
    }
}