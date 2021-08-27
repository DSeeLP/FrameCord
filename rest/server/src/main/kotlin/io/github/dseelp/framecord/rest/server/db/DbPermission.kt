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

import io.github.dseelp.framecord.rest.data.objects.FullPermission
import io.github.dseelp.framecord.rest.data.objects.SimplePermission
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.mapLazy

class DbPermission(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DbPermission>(DbPermissions)

    var name by DbPermissions.name
    var description by DbPermissions.description

    val simple: SimplePermission
        get() = SimplePermission(id.value)
    val full: FullPermission
        get() = FullPermission(id.value, name, description)
}

fun Iterable<DbPermission>.toFullPermissions() = map { it.full }
fun SizedIterable<DbPermission>.toFullPermissionsLazy() = mapLazy { it.full }
fun Array<DbPermission>.toFullPermissions() = map { it.full }

fun Iterable<DbPermission>.toSimplePermissions() = map { it.simple }
fun SizedIterable<DbPermission>.toSimplePermissionsLazy() = mapLazy { it.simple }
fun Array<DbPermission>.toSimplePermissions() = map { it.simple }


object DbPermissions : IntIdTable("permissions") {
    val name = varchar("name", 255)
    val description = text("description").nullable()
}

object DbPermissionLink : Table("permissionsLink") {
    val user = reference("user", DbUsers)
    val permission = reference("permission", DbPermissions)
}