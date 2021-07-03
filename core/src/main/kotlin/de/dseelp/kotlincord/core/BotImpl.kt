/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import de.dseelp.kotlincord.api.Bot
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.core.listeners.EventBusListener
import dev.kord.core.Kord
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier

@OptIn(InternalKotlinCordApi::class)
object BotImpl : Bot, CordKoinComponent {
    override val kord: Kord
        get() = _kord!!
    override val isStarted: Boolean
        get() = TODO("Not yet implemented")

    private val eventBus: EventBus by inject()

    var _kord: Kord? = null

    suspend fun start() {
        val token by inject<String>(qualifier("token"))
        _kord = Kord(token)
        kord.login()
        EventBusListener
    }
}