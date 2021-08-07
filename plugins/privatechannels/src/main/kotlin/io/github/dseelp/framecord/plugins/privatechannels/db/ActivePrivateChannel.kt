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

class ActivePrivateChannel(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ActivePrivateChannel>(ActivePrivateChannels)

    var privateChannel by PrivateChannel referencedOn ActivePrivateChannels.privateChannel
    var channelId by ActivePrivateChannels.channelId
    var ownerId by ActivePrivateChannels.ownerId
    var executiveId by ActivePrivateChannels.executiveId
    var customNameTemplate by ActivePrivateChannels.customNameTemplate
    var lastUpdated by ActivePrivateChannels.lastUpdated
    var locked by ActivePrivateChannels.locked
    var userLimit by ActivePrivateChannels.userLimit
}

object ActivePrivateChannels : LongIdTable() {
    val privateChannel = reference("privateChannel", PrivateChannels)
    val channelId = long("channelId")
    val ownerId = long("ownerId")
    val executiveId = long("executiveId").nullable()
    val customNameTemplate = varchar("customNameTemplate", 1000).nullable()
    val lastUpdated = long("lastUpdatedMillis").nullable()
    val locked = bool("locked").default(false)
    val userLimit = short("userLimit")
}