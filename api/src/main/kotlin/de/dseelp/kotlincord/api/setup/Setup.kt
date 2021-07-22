/*
 * Copyright (c) 2021 DSeeLP & KotlinCord contributors
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
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.PluginComponent
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import org.koin.core.component.inject

class Setup<P : Plugin> constructor(
    override val plugin: P,
    val channel: GuildMessageChannel,
    val steps: Array<SetupStep>,
    val onCompletion: suspend (result: SetupResult) -> Unit,
    val checkAccess: suspend (member: Member, channel: GuildMessageChannel) -> Boolean = { _, _ -> true }
) : PluginComponent<P> {
    private var _isStarted = false
    private var _isCompleted = false
    private var _wasStopped = false
    val isStarted by ::_isStarted
    val isCompleted by ::_isCompleted
    val wasStopped by ::_wasStopped
    val isDone
        get() = isCompleted || wasStopped

    var currentStepIndex: Int = 0
        private set
    var currentStep: SetupStep? = null
        private set
    private var handler: EventBus.Handler.ClassHandler<*>? = null
    private var currentMessage: Message? = null
    val results = arrayOfNulls<Any>(steps.size)

    private val eventBus: EventBus by inject()

    init {
        if (steps.isEmpty()) throw IllegalArgumentException("A setup needs at least one step!")
    }

    private suspend fun setStep(index: Int) {
        handler?.let { eventBus.removeHandler(it) }
        currentStep?.cancel(currentMessage!!)
        currentStepIndex = index
        val step = steps[index]
        currentStep = step
        handler = EventBus.Handler.ClassHandler(plugin, step::class, step)
        eventBus.addHandler(handler!!)
        currentMessage = currentStep?.send(this, channel, checkAccess)
    }

    suspend fun completeStep(result: Any?) {
        if (!isStarted || isDone) throw IllegalStateException("A step can only be completed when the setup is started!")
        if (currentStep == null) return

        /*if (StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).callerClass.originalClass != currentStep!!::class.java) throw IllegalAccessException(
            "This method can only be called from the current step"
        )*/
        results[currentStepIndex] = result
        if (currentStepIndex != steps.lastIndex)
            setStep(currentStepIndex + 1)
        else {
            _isCompleted = true
            _isStarted = false
            callComplete()
        }
    }

    /*private val Class<*>.originalClass: Class<*>
        get() {
            return if (isAnonymousClass) {
                if (interfaces.isEmpty()) superclass else interfaces[0]
            } else {
                this
            }
        }*/

    @OptIn(InternalKotlinCordApi::class)
    suspend fun cancelSetup() {
        if (!isStarted || isDone) throw IllegalStateException("A step can only be cancelled when the setup is started!")
        if (currentStep == null) return
        currentMessage?.let { currentStep!!.cancel(it) }
        inject<SetupManager>().value.removeSetup(this)
    }

    @OptIn(InternalKotlinCordApi::class)
    suspend fun start(autoRegister: Boolean = false) {
        if (isStarted) throw IllegalStateException("The setup is already started")
        if (isDone) throw IllegalStateException("The setup is already completed/done!")
        val setupManager: SetupManager by inject()
        if (!setupManager.isRegistered(this)) {
            if (autoRegister)
                setupManager.addSetup(this)
            else
                throw IllegalStateException("A setup must be registered before it can be started")
        }
        _isStarted = true
        setStep(0)
    }

    internal suspend fun stop() {
        if (isDone) throw IllegalStateException("The setup is already completed/done!")
        _isStarted = false
        _wasStopped = true
        callComplete(currentStepIndex)
    }

    private suspend fun callComplete(lastStepIndex: Int? = null) {
        handler?.let { eventBus.removeHandler(it) }
        currentStep?.cancel(currentMessage!!)
        onCompletion(
            SetupResult(
                wasStopped,
                isCompleted,
                channel,
                results,
                steps,
                if (lastStepIndex != null) steps[lastStepIndex] else null,
                lastStepIndex
            )
        )
    }
}