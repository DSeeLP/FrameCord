/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.configs

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class BotConfig(val instanceId: String, val debug: Boolean)
