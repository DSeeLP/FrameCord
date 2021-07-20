/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
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