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
import io.github.dseelp.framecord.api.modules.Module
import io.github.dseelp.framecord.api.modules.ModuleManager
import io.github.dseelp.framecord.rest.server.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object ModuleManagerImpl : ModuleManager {
    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(DbGuilds, DbModules, DbModulesLink, DbFeatures, DbFeaturesLink)
        }
    }

    override fun registerModule(id: String, name: String): Module {
        val module = ModuleImpl(id, name)
        transaction {
            if (DbModule.findById(id) != null) throw IllegalStateException("A module with the id $id already exists")
            DbModule.new(id) {
                this.name = name
            }
        }
        return module
    }

    override fun unregisterModule(id: String) {
        transaction {
            val module = DbModule.findById(id) ?: throw IllegalStateException("A module with the id $id doesn't exist")
            module.delete()
        }
    }

    override fun getRegisteredModules(): Flow<Module> {
        return transaction {
            DbModule.all().asFlow().map { ModuleImpl(it.id.value, it.name) }
        }
    }

    override fun getEnabledModules(guildId: Snowflake): Flow<Module> {
        return (DbGuild.findById(guildId.value)
            ?: throw IllegalStateException("A guild with the id ${guildId.value} doesn't exist")).enabledModules.asFlow()
            .map { ModuleImpl(it.id.value, it.name) }
    }

    override fun isModuleRegistered(id: String): Boolean {
        return transaction {
            DbModule.findById(id) != null
        }
    }

}