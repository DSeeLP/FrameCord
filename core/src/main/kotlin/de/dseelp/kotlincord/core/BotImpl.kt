/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import de.dseelp.kotlincord.api.Bot
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.core.listeners.EventBusListener
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier

@OptIn(InternalKotlinCordApi::class)
object BotImpl : Bot, CordKoinComponent {
    override val shardManager: ShardManager
        get() = _shardManager!!
    override val isStarted: Boolean
        get() = TODO("Not yet implemented")

    private val eventBus: EventBus by inject()

    var _shardManager: ShardManager? = null

    fun start() {
        val token by inject<String>(qualifier("token"))
        _shardManager = DefaultShardManagerBuilder.createDefault(token).addEventListeners(EventBusListener).build()
        shardManager.setStatus(OnlineStatus.ONLINE)
    }
}