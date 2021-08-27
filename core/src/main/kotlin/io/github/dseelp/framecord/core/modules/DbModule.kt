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

import io.github.dseelp.framecord.api.utils.StringEntity
import io.github.dseelp.framecord.api.utils.StringEntityClass
import io.github.dseelp.framecord.api.utils.VarCharIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Table
import kotlin.random.Random

class DbModule(id: EntityID<String>) : StringEntity(id) {
    companion object : StringEntityClass<DbModule>(DbModules) {
        fun findByNumericId(id: Long) = find { DbModules.numericId eq id }
    }

    var name by DbModules.name
    var numericId by DbModules.numericId

    val guilds by DbGuild via DbModulesLink
    val features by DbFeature referrersOn DbFeatures.module
}

object DbModules : VarCharIdTable(255, "modules") {
    val name = varchar("name", 255)
    val numericId = long("numericId").clientDefault {
        val value = Random(Random.nextLong()).nextLong()
        if (value < 0) value * -1 else value
    }
}

object DbModulesLink : Table("modulesLink") {
    val module = reference("module", DbModules)
    val guild = reference("guild", DbGuilds)
}

