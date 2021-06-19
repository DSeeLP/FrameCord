/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.listeners

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.koin.core.component.inject

@OptIn(InternalKotlinCordApi::class)
object EventBusListener : EventListener, CordKoinComponent {

    val eventBus: EventBus by inject()

    override fun onEvent(event: GenericEvent) {
        eventBus.call(event)
    }

}