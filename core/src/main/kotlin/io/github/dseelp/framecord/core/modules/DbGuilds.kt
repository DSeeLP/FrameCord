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

package io.github.dseelp.framecord.core.modules

import dev.kord.common.entity.Snowflake
import io.github.dseelp.framecord.api.asSnowflake
import io.github.dseelp.framecord.api.guild.GuildInfo
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

class DbGuild(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<DbGuild>(DbGuilds) {
        fun findById(guildId: Snowflake) = DbGuild.findById(guildId.value)
        fun new(guildId: Snowflake, init: DbGuild.() -> Unit) = DbGuild.new(guildId.value, init)
    }

    var name by DbGuilds.name
    var botJoined by DbGuilds.botJoined
    var prefix by DbGuilds.prefix

    var enabledModules by DbModule via DbModulesLink
    var enabledFeatures by DbFeature via DbFeaturesLink

    val guildId
        get() = id.value.asSnowflake

    val info
        get() = GuildInfo(guildId, prefix)
}

object DbGuilds : IdTable<Long>("guilds") {
    val name = varchar("name", 255).nullable()
    val botJoined = long("botJoined")
    val prefix = varchar("prefix", 32).default("!")
    override val id: Column<EntityID<Long>> = long("id").entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}