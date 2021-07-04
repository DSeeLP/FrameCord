/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import de.dseelp.kotlincord.api.Bot
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.bot
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.core.listeners.EventBusListener
import dev.kord.core.Kord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier
import kotlin.coroutines.CoroutineContext

@OptIn(InternalKotlinCordApi::class)
object BotImpl : Bot, CordKoinComponent {
    override val kord: Kord
        get() = _kord!!
    override val isStarted: Boolean
        get() = TODO("Not yet implemented")
    override val job: Job = SupervisorJob()
    override val coroutineContext: CoroutineContext = job + Dispatchers.Default

    var _kord: Kord? = null

    val logger by logger<Bot>()

    suspend fun start() {
        val token by inject<String>(qualifier("token"))
        _kord = Kord(token)
        bot.launch {
            kord.login()
        }
        //EventBusListener
        logger.info("Startup complete")
    }
}