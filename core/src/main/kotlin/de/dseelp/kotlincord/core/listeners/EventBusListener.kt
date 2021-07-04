/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.listeners

import de.dseelp.kotlincord.api.Bot
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import dev.kord.core.event.Event
import dev.kord.core.on
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.subscribe
import kotlinx.coroutines.launch
import org.koin.core.component.inject

@OptIn(InternalKotlinCordApi::class)
object EventBusListener: CordKoinComponent {
    val eventBus: EventBus by inject()
    val bot: Bot by inject()

    init {
        bot.kord.events.onEach {
            println(it)
            eventBus.callAsync(it)
        }.launchIn(bot.kord)
    }
}