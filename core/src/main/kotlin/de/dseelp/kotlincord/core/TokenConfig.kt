/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TokenConfig(val token: String)