/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins.repository

import de.dseelp.kotlincord.api.Version

interface Package<V : BasePackageVersion<V>> {
    val groupId: String
    val artifactId: String
    val authors: String
    val versions: Array<V>

    suspend fun install(version: Version, update: Boolean = false, returnImmediately: Boolean = false)
    suspend fun installLatest()
    fun uninstall()

    val isInstalled: Boolean
    val installedVersion: Version?
    fun isCoreVersionSupported(version: Version): Boolean =
        findVersion(version)?.let { isCoreVersionSupported(it) } ?: false

    fun isCoreVersionSupported(packageVersion: PackageVersion): Boolean

    fun findVersion(version: Version): PackageVersion?
}
