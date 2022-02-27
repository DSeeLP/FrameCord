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

package io.github.dseelp.framecord.api.modules

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import kotlinx.coroutines.flow.Flow

/**
 * A module provides functionality and can be disabled/enabled per guild
 */
interface Module {
    val id: String
    val name: String
    val numericId: Long

    val features: Flow<Feature>

    suspend fun disable(guild: Guild) = disable(guild.id.value)
    suspend fun disable(guildId: Snowflake) = disable(guildId.value)
    suspend fun disable(guildId: ULong)

    suspend fun enable(guildId: Snowflake) = enable(guildId.value)
    suspend fun enable(guildId: ULong)

    suspend fun enable(guild: Guild) = enable(guild.id.value)

    fun isEnabled(guild: Guild): Boolean = isEnabled(guild.id.value)
    fun isEnabled(guildId: Snowflake): Boolean = isEnabled(guildId.value)
    fun isEnabled(guildId: ULong): Boolean

    fun registerFeature(feature: Feature)
}