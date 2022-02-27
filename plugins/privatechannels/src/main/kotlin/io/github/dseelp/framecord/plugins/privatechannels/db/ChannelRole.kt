package io.github.dseelp.framecord.plugins.privatechannels.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

class ChannelRole(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ChannelRole>(ChannelRoles)

    var name by ChannelRoles.name
    var canBan by ChannelRoles.canBan
    var canUnban by ChannelRoles.canUnban
    var canKick by ChannelRoles.canKick
    var canRename by ChannelRoles.canRename
    var priority by ChannelRole optionalReferencedOn ChannelRoles.priority
}


object ChannelRoles : LongIdTable() {
    val name = varchar("name", 255)
    val canBan = bool("canBan").default(false)
    val canUnban = bool("canUnban").default(false)
    val canKick = bool("canKick").default(false)
    val canRename = bool("canRename").default(false)
    val priority = reference("priority", ChannelRoles).nullable()
}

enum class RolePermission {
    KICK,
    BAN,
    UNBAN,
    RENAME
}