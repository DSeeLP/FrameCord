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

class ActivePrivateChannelEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ActivePrivateChannelEntity>(ActivePrivateChannelsTable)

    var privateChannel by PrivateChannelEntity referencedOn ActivePrivateChannelsTable.privateChannel
    var channelId by ActivePrivateChannelsTable.channelId
    var ownerId by ActivePrivateChannelsTable.ownerId
    var executiveId by ActivePrivateChannelsTable.executiveId
    var customNameTemplate by ActivePrivateChannelsTable.customNameTemplate
    var lastUpdated by ActivePrivateChannelsTable.lastUpdated
    var locked by ActivePrivateChannelsTable.locked
    var userLimit by ActivePrivateChannelsTable.userLimit
}

object ActivePrivateChannelsTable : LongIdTable() {
    val privateChannel = reference("privateChannel", PrivateChannelsTable)
    val channelId = long("channelId")
    val ownerId = long("ownerId")
    val executiveId = long("executiveId").nullable()
    val customNameTemplate = varchar("customNameTemplate", 1000).nullable()
    val lastUpdated = long("lastUpdatedMillis").nullable()
    val locked = bool("locked").default(false)
    val userLimit = short("userLimit").default(0)
}