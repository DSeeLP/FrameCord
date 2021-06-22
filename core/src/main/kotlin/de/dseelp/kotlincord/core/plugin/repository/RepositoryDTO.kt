/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin.repository

import kotlinx.serialization.Serializable

@Serializable
data class RepositoryDTO(val name: String, val packages: Array<RepositoryIndexImpl>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RepositoryDTO) return false

        if (name != other.name) return false
        if (!packages.contentEquals(other.packages)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + packages.contentHashCode()
        return result
    }
}
