/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.utils.koin

import de.dseelp.kotlincord.api.Cord
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import org.koin.core.Koin
import org.koin.core.module.Module


@InternalKotlinCordApi
interface CordKoinComponent : BaseKoinComponent {
    override fun getKoin(): Koin = CordKoinContext.app?.koin!!

    override fun loadKoinModules(modules: List<Module>) {
        getKoin().get<Cord>().getPlugin().loadKoinModules(modules)
        getKoin().loadModules(modules)
    }

    override fun unloadKoinModules(modules: List<Module>) {
        getKoin().unloadModules(modules)
    }
}