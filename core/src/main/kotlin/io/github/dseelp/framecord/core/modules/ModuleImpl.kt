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

import io.github.dseelp.framecord.api.modules.Feature
import io.github.dseelp.framecord.api.modules.Module
import kotlinx.coroutines.flow.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class ModuleImpl(id: String, override val name: String) : Module {
    override val id: String = id.lowercase()
    private val dbModule by lazy {
        transaction {
            DbModule.findById(id.lowercase())
                ?: throw IllegalStateException("This module does not exist in the database")
        }
    }
    override val numericId: Long by lazy { transaction { dbModule.numericId } }
    override val features: Flow<Feature>
        get() = transaction {
            dbModule.features.asFlow().map { FeatureImpl(this@ModuleImpl, it.id.value.lowercase(), name) }
        }

    override suspend fun disable(guildId: Long) = newSuspendedTransaction {
        features.onEach {
            it.disable(guildId)
        }.collect()
        val guild = DbGuild.findById(guildId) ?: return@newSuspendedTransaction
        if (!dbModule.guilds.contains(guild)) return@newSuspendedTransaction
        DbModulesLink.deleteWhere {
            (DbModulesLink.module eq dbModule.id) and (DbModulesLink.guild eq guild.id)
        }
    }

    override suspend fun enable(guildId: Long) = newSuspendedTransaction {
        features.onEach {
            it.enable(guildId)
        }.collect()
        val guild = DbGuild.findById(guildId) ?: return@newSuspendedTransaction
        if (dbModule.guilds.contains(guild)) return@newSuspendedTransaction
        DbModulesLink.insert {
            it[module] = dbModule.id
            it[this.guild] = guild.id
        }
    }

    override fun isEnabled(guildId: Long): Boolean {
        val id = EntityID(guildId, DbGuilds)
        val count = transaction {
            DbModulesLink.select {
                DbModulesLink.module eq dbModule.id and (DbModulesLink.guild eq id)
            }.count()
        }
        return count != 0L
    }

    override fun registerFeature(feature: Feature) {
        transaction {
            if (DbFeature.findById(feature.id) != null) throw IllegalStateException("A feature with this id is already registered")
            DbFeature.new(feature.id) {
                module = dbModule
                name = feature.name
                guilds = dbModule.guilds
            }
        }
    }
}