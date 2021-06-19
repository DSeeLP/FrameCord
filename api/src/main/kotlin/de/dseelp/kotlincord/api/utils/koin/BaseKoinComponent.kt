/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.utils.koin

import org.koin.core.component.KoinComponent
import org.koin.core.module.Module

interface BaseKoinComponent : KoinComponent {
    fun loadKoinModules(module: Module) = loadKoinModules(listOf(module))

    fun loadKoinModules(modules: List<Module>) = getKoin().loadModules(modules)

    /**
     * unload Koin modules from global Koin context
     */
    fun unloadKoinModules(module: Module) = unloadKoinModules(listOf(module))

    /**
     * unload Koin modules from global Koin context
     */
    fun unloadKoinModules(modules: List<Module>) = getKoin().unloadModules(modules)
}