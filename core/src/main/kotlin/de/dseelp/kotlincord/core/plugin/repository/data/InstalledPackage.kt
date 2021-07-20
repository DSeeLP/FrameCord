/*
 * Copyright (c) 2021 KotlinCord team & contributors
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

package de.dseelp.kotlincord.core.plugin.repository.data

import de.dseelp.kotlincord.api.Version
import de.dseelp.kotlincord.api.plugins.repository.Package
import de.dseelp.kotlincord.api.version
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.and
import java.util.*

class InstalledPackage(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<InstalledPackage>(InstalledPackages) {
        fun findByPackage(`package`: Package<*>) =
            find { (InstalledPackages.groupId eq `package`.groupId) and (InstalledPackages.artifactId eq `package`.artifactId) }
    }

    var groupId by InstalledPackages.groupId
    var artifactId by InstalledPackages.artifactId
    var version by InstalledPackages.version

    val dto: InstalledPackageDTO
        get() = InstalledPackageDTO(id.value, groupId, artifactId, version)
}

data class InstalledPackageDTO(val id: UUID, val groupId: String, val artifactId: String, val version: Version)

object InstalledPackages : UUIDTable() {
    val groupId = varchar("groupId", 512)
    val artifactId = varchar("artifactId", 32)
    val version = version("version")
}