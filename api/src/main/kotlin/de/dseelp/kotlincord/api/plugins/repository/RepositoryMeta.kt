package de.dseelp.kotlincord.api.plugins.repository

import kotlinx.serialization.Serializable

@Serializable
data class RepositoryMeta(val type: String, val version: Int, val name: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RepositoryMeta) return false

        if (type != other.type) return false
        if (version != other.version) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + version
        result = 31 * result + name.hashCode()
        return result
    }
}