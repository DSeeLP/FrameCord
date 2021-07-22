/*
 * Copyright (c) 2021 KotlinCord team & contributors
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dseelp.kotlincord.api.setup

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.event.EventHandle
import de.dseelp.kotlincord.api.event.Listener
import de.dseelp.kotlincord.api.events.PluginDisableEvent
import de.dseelp.kotlincord.api.events.PluginEventType
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Listener
@InternalKotlinCordApi
class SetupManager : CordKoinComponent {
    private val setups = mutableListOf<Setup<*>>()
    private val mutex = Mutex()

    suspend fun isRegistered(setup: Setup<*>): Boolean = mutex.withLock {
        setups.contains(setup)
    }

    suspend fun <P : Plugin> addSetup(setup: Setup<P>) {
        if (isRegistered(setup)) throw IllegalStateException("A setup can't be registered twice!")
        if (setup.isStarted || setup.isDone) throw IllegalStateException("A setup must not be started or completed!")
        setups.add(setup)
    }

    suspend fun removeSetup(setup: Setup<*>) {
        if (!isRegistered(setup)) throw IllegalStateException("A setup must be registered to be removed!")
        if (setup.isDone) {
            setups.remove(setup)
        } else {
            setup.stop()
            setups.remove(setup)
        }
    }

    suspend fun removeAllSetups(plugin: Plugin) = mutex.withLock {
        setups.removeIf {
            val removed = it.plugin == plugin
            if (removed) runBlocking { it.stop() }
            removed
        }
    }

    @EventHandle
    suspend fun onPluginDisable(event: PluginDisableEvent) {
        if (event.type != PluginEventType.POST) return
        removeAllSetups(event.plugin)
    }
}