/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins.repository

import de.dseelp.kotlincord.api.Version
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class PackageVersion(
    val version: Version,
    val minCoreVersion: Version,
    val maxCoreVersion: Version,
    val supportsLatest: Boolean = false
) : BasePackageVersion<PackageVersion> {
    @Transient
    val coreVersionRange = minCoreVersion..maxCoreVersion

    override fun compareTo(other: PackageVersion): Int = version.compareTo(other.version)
}
