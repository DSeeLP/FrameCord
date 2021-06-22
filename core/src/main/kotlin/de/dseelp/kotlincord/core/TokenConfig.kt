/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TokenConfig(val token: String)