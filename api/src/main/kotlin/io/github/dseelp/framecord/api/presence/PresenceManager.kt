package io.github.dseelp.framecord.api.presence

interface PresenceManager {
    fun createPresence(presence: Presence): UByte
    fun editPresence(id: UByte, presence: Presence)
    fun deletePresence(id: UByte, presence: Presence)
    fun getPresence(id: UByte): Presence?
    operator fun get(id: UByte) = getPresence(id)
    fun nextId(): UByte
    fun getAll(): Array<Presence>
}