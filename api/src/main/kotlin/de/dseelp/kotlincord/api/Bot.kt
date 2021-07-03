/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api

import dev.kord.core.Kord

interface Bot {
    val kord: Kord
    val isStarted: Boolean
}