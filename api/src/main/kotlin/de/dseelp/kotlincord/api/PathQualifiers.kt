/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api

import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import org.koin.core.component.inject
import java.nio.file.Path

interface PathQualifiers {
    val root: Path
    val configLocation: Path
    val pluginLocation: Path

    @OptIn(InternalKotlinCordApi::class)
    companion object : CordKoinComponent {
        private val instance by lazy { inject<PathQualifiers>().value }

        val ROOT = instance.root
        val CONFIG_LOCATION = instance.configLocation
        val PLUGIN_LOCATION = instance.pluginLocation
    }
}