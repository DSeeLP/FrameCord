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
import io.github.dseelp.framecord.rest.server.db.DbFeature
import io.github.dseelp.framecord.rest.server.db.DbModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.sql.transactions.transaction

class ModuleImpl(override val id: String, override val name: String) : Module {
    private val dbModule = (DbModule.findById(id) ?: throw IllegalStateException("This module does not exist in the database"))
    override val features: Flow<Feature>
        get() = transaction {
            dbModule.features.asFlow().map { FeatureImpl(this@ModuleImpl, id, name) }
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