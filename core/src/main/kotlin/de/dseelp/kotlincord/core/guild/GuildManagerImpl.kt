/*
 * Copyright (c) 2021 DSeeLP & KotlinCord contributors
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

package de.dseelp.kotlincord.core.guild

import de.dseelp.kotlincord.api.guild.GuildInfo
import de.dseelp.kotlincord.api.guild.GuildManager
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.sql.transactions.transaction

open class GuildManagerImpl : GuildManager {
    override fun getGuildInfo(guildId: Snowflake): GuildInfo = transaction { findInfo(guildId).info }

    fun findInfo(guildId: Snowflake) = DbGuildInfo.findById(guildId) ?: DbGuildInfo.new(guildId) {
        prefix = "!"
    }

    override fun setGuildInfo(info: GuildInfo): Unit = transaction {
        findInfo(info.guildId).apply {
            prefix = info.prefix
        }
    }
}