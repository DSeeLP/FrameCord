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

package io.github.dseelp.framecord.plugins.privatechannels.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

class PrivateChannelEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PrivateChannelEntity>(PrivateChannelsTable)

    var guildId by PrivateChannelsTable.guildId
    var joinChannelId by PrivateChannelsTable.joinChannelId
    var nameTemplate by PrivateChannelsTable.nameTemplate
    var defaultGame by PrivateChannelsTable.defaultGame
}

@OptIn(ExperimentalUnsignedTypes::class)
object PrivateChannelsTable : LongIdTable() {
    val guildId = ulong("guildId")
    val joinChannelId = ulong("joinChannelId")
    val nameTemplate = varchar("nameTemplate", 1000).default("%user%'s Room")
    val defaultGame = varchar("defaultGame", 100).default("a Game")
}