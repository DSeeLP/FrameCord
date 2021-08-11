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

package io.github.dseelp.framecord.core.logging

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

open class DbError(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<DbError>(DbErrors)

    var errorClass by DbErrors.errorClass
    var message by DbErrors.message
    var stackTrace by DbErrors.stackTrace
    var time by DbErrors.time
    val suppressedExceptions by DbSuppressedError referrersOn DbSuppressedErrors.error
}

open class DbErrors(name: String) : UUIDTable(name) {
    companion object : DbErrors("errors")

    val errorClass = varchar("errorClazz", 768)
    val message = varchar("message", 768).nullable()
    val stackTrace = text("stacktrace")
    val time = long("time")
}

class DbSuppressedError(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<DbSuppressedError>(DbSuppressedErrors)

    var error by DbError referencedOn DbSuppressedErrors.error
    var errorClass by DbSuppressedErrors.errorClass
    var message by DbSuppressedErrors.message
    var stackTrace by DbSuppressedErrors.stackTrace
    var time by DbSuppressedErrors.time
}

object DbSuppressedErrors : UUIDTable("suppressedErrors") {
    val error = reference("error", DbErrors)
    val errorClass = varchar("errorClazz", 768)
    val message = varchar("message", 768).nullable()
    val stackTrace = text("stacktrace")
    val time = long("time")
}