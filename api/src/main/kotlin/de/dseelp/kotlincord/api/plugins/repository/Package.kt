/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins.repository

import de.dseelp.kotlincord.api.Version

interface Package {
    val groupId: String
    val artifactId: String
    val authors: String
    val versions: Array<Version>
}