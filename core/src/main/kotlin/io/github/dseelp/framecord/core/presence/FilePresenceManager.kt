package io.github.dseelp.framecord.core.presence

import io.github.dseelp.framecord.api.presence.Presence
import io.github.dseelp.framecord.api.presence.PresenceManager
import io.github.dseelp.framecord.core.utils.PresenceConfig

object FilePresenceManager : PresenceManager {
    private var config = PresenceConfig.load()
    fun reload() {
        config = PresenceConfig.load()
    }

    private var presences: Array<Presence>
        get() = config.presences
        set(value) = updateConfig(value)

    override fun createPresence(presence: Presence): UByte {
        val id = nextId()
        presences += presence
        return id
    }

    override fun editPresence(id: UByte, presence: Presence) {
        val list = presences.toMutableList()
        list[id.toInt()] = presence
        if (presences.size != list.size) return
        presences = list.toTypedArray()
    }

    override fun deletePresence(id: UByte, presence: Presence) {
        val list = presences.toMutableList()
        list.removeAt(id.toInt())
        presences = list.toTypedArray()
    }

    override fun getPresence(id: UByte): Presence? = runCatching { presences[id.toInt()] }.getOrNull()

    override fun nextId(): UByte {
        val config = PresenceConfig.load()
        return (config.presences.count() + 1).toUByte()
    }

    override fun getAll(): Array<Presence> = presences

    fun updateConfig(presences: Array<Presence>) {
        val cfg = PresenceConfig(presences)
        config = cfg
        PresenceConfig.write(config)
    }
}