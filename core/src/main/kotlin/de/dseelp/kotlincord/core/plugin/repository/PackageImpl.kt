/*
 * Created by Dirk on 22.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin.repository

import de.dseelp.kotlincord.api.Version
import de.dseelp.kotlincord.api.plugins.repository.Package
import kotlinx.serialization.Serializable

@Serializable
data class PackageImpl(
    override val groupId: String,
    override val artifactId: String,
    override val authors: String,
    override val versions: Array<Version>
) : Package {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PackageImpl) return false

        if (groupId != other.groupId) return false
        if (artifactId != other.artifactId) return false
        if (authors != other.authors) return false
        if (!versions.contentEquals(other.versions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + artifactId.hashCode()
        result = 31 * result + authors.hashCode()
        result = 31 * result + versions.contentHashCode()
        return result
    }
}