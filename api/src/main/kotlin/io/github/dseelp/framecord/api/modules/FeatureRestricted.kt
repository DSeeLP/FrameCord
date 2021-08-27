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

import io.github.dseelp.framecord.api.bot
import io.github.dseelp.framecord.api.modules.FeatureRestricted.Type.FEATURE
import io.github.dseelp.framecord.api.modules.FeatureRestricted.Type.MODULE


data class FeatureRestricted(val type: Type, val id: String) {
    enum class Type {
        FEATURE,
        MODULE
    }
}


fun FeatureRestricted.checkBoolean(guildId: Long, moduleManager: ModuleManager = bot.moduleManager) = when (type) {
    FEATURE -> moduleManager.isFeatureEnabled(id, guildId)
    MODULE -> moduleManager.isModuleEnabled(id, guildId)
}

fun FeatureRestricted.check(guildId: Long, moduleManager: ModuleManager = bot.moduleManager) =
    if (checkBoolean(guildId, moduleManager)) Unit else null

fun checkFeature(guildId: Long, id: String, moduleManager: ModuleManager = bot.moduleManager) =
    FeatureRestricted(FEATURE, id).check(guildId, moduleManager)

fun checkModule(guildId: Long, id: String, moduleManager: ModuleManager = bot.moduleManager) =
    FeatureRestricted(MODULE, id).check(guildId, moduleManager)

fun checkFeatureBoolean(guildId: Long, id: String, moduleManager: ModuleManager = bot.moduleManager) =
    FeatureRestricted(FEATURE, id).checkBoolean(guildId, moduleManager)

fun checkModuleBoolean(guildId: Long, id: String, moduleManager: ModuleManager = bot.moduleManager) =
    FeatureRestricted(MODULE, id).checkBoolean(guildId, moduleManager)
