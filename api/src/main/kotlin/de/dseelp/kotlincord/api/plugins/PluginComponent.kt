/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.utils.koin.BaseKoinComponent
import de.dseelp.kotlincord.api.utils.koin.registerKoinModules
import org.koin.core.Koin
import org.koin.core.module.Module

interface PluginComponent<P : Plugin> : BaseKoinComponent {
    @InternalKotlinCordApi
    val plugin: P

    @OptIn(InternalKotlinCordApi::class)
    override fun getKoin(): Koin = plugin.koinApp.koin

    @OptIn(InternalKotlinCordApi::class)
    override fun loadKoinModules(modules: List<Module>) = plugin.registerKoinModules(modules)

    override fun unloadKoinModules(modules: List<Module>) = throw UnsupportedOperationException()
}