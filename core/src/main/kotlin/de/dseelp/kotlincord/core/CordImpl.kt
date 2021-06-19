/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import de.dseelp.kotlincord.api.Cord
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.ReloadScope
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.events.ReloadEvent
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat

@OptIn(InternalKotlinCordApi::class)
object CordImpl : Cord, CordKoinComponent {
    val eventBus by inject<EventBus>()
    override fun reload(vararg scopes: ReloadScope) {
        //TODO: Add logic to reload here
        println("Reloading")
        eventBus.call(ReloadEvent(scopes.toList().toTypedArray()))
    }

    override fun getPlugin(): Plugin = FakePlugin

    val formatter = SimpleDateFormat("dd.MM HH:mm:ss")
}