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

package de.dseelp.kotlincord.api.guild

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.guild.GuildUtils.guildManager
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import org.koin.core.component.inject

interface GuildManager {
    fun getGuildInfo(guildId: Snowflake): GuildInfo
    fun setGuildInfo(info: GuildInfo)
}

@OptIn(InternalKotlinCordApi::class)
private object GuildUtils : CordKoinComponent {
    val guildManager: GuildManager by inject()
}

var GuildBehavior.info: GuildInfo
    get() = guildManager.getGuildInfo(id)
    set(value) = guildManager.setGuildInfo(value)