/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.listeners

import de.dseelp.kotlincord.api.Bot
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject

@OptIn(InternalKotlinCordApi::class)
object EventBusListener: CordKoinComponent {
    val eventBus: EventBus by inject()
    val bot: Bot by inject()

    init {
        bot.kord.events.onEach {
            eventBus.callAsync(it)
        }.launchIn(bot.kord)
    }
}