/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.utils.koin

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import org.koin.core.KoinApplication

@InternalKotlinCordApi
object CordKoinContext {
    var app: KoinApplication? = null
}