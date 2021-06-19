/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api

import net.dv8tion.jda.api.sharding.ShardManager

interface Bot {
    val shardManager: ShardManager
    val isStarted: Boolean
}