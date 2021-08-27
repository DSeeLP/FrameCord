/*
 * Copyright (c) 2021 DSeeLP & FrameCord contributors
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

package io.github.dseelp.framecord.api.event

import io.github.dseelp.framecord.api.bot
import io.github.dseelp.framecord.api.events.PluginDisableEvent
import io.github.dseelp.framecord.api.events.PluginEventType
import io.github.dseelp.framecord.api.logging.logger
import io.github.dseelp.framecord.api.plugins.Plugin
import io.github.dseelp.framecord.api.plugins.PluginLoader
import io.github.dseelp.framecord.api.utils.Criterion
import io.github.dseelp.framecord.api.utils.ReflectionUtils
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
@Listener
open class EventBus : CordKoinComponent {
    private val handlers = Collections.synchronizedList(mutableListOf<Handler>())

    private val eventExecutorService = Executors.newFixedThreadPool(4)


    suspend fun callAsync(event: Any, async: Boolean = false) {
        val eventClass = event::class
        withContext(bot.coroutineContext) {
            val executeFunction = suspend {
                val handlers = handlers.map { it }
                for (index in 0..handlers.lastIndex) {
                    val handler = handlers.getOrNull(index) ?: continue
                    if (handler is Handler.ClassHandler<*>) handler.invoke(event)
                    else if (handler.clazz == eventClass || eventClass.isSubclassOf(handler.clazz)) handler.invoke(event)
                }
            }
            if (async) bot.launch {
                executeFunction.invoke()
            } else {
                executeFunction.invoke()
            }
        }
    }

    @io.github.dseelp.framecord.api.InternalFrameCordApi
    fun unregister(clazz: KClass<out Plugin>) {
        handlers.removeIf {
            it.plugin::class == clazz
        }
    }

    private val pluginLoader: PluginLoader by inject()
    private val log by logger<EventBus>()

    fun searchPackages(plugin: Plugin? = null, vararg packages: String) {
        ReflectionUtils.findClasses(packages.toList().toTypedArray(), plugin) {
            Criterion.hasAnnotation<Listener>().assert()
        }.onEach { clazz ->
            val p = if (plugin != null) plugin
            else {
                val classLoader = clazz.java.classLoader
                pluginLoader.loadedPlugins.firstOrNull { it.classLoader == classLoader }?.plugin
            }
            if (p == null) {
                log.error("Failed to determine plugin for class ${clazz.qualifiedName}")
                return
            }
            val handler = Handler.ClassHandler(p, clazz)
            if (handlers.find {
                    if (it is Handler.ClassHandler<*>) {
                        it.clazz == clazz
                    } else false
                } == null) handlers.add(handler)
        }
    }

    fun removeHandler(handler: Handler) {
        handlers.remove(handler)
    }

    fun searchPackage(packageName: String, plugin: Plugin? = null) = searchPackages(plugin, packageName)

    fun addHandler(handler: Handler) {
        handlers.add(handler)
    }

    fun addHandler(plugin: Plugin, clazz: KClass<out Any>, instance: Any? = null) {
        handlers.add(Handler.ClassHandler(plugin, clazz, instance))
    }

    inline fun <reified T : Any> addClassHandler(plugin: Plugin, listener: T) {
        addHandler(plugin, listener::class, listener)
    }

    inline fun <reified T : Any> addClassHandler(plugin: Plugin) {
        addHandler(plugin, T::class)
    }

    inline fun <reified T : Any> addHandler(plugin: Plugin, noinline block: (event: T) -> Unit) = addHandler(
        Handler.LambdaHandler(plugin, T::class, block)
    )

    fun removeHandlers(plugin: Plugin) {
        handlers.removeIf { it.plugin == plugin }
    }

    @EventHandle
    @io.github.dseelp.framecord.api.InternalFrameCordApi
    fun onPluginDisable(event: PluginDisableEvent) {
        if (event.type != PluginEventType.POST) return
        unregister(event.plugin::class)
    }

    sealed class Handler(val plugin: Plugin) {
        abstract suspend fun invoke(
            event: Any
        )

        abstract val clazz: KClass<out Any>

        class LambdaHandler<T>(
            plugin: Plugin,
            override val clazz: KClass<out Any>,
            val lambda: (event: T) -> Unit
        ) : Handler(plugin) {
            override suspend fun invoke(
                event: Any
            ) {
                @Suppress("UNCHECKED_CAST")
                lambda.invoke(event as T)
            }
        }

        @OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
        class ClassHandler<T : Any>(plugin: Plugin, override val clazz: KClass<out T>, obj: T? = null) :
            Handler(plugin),
            CordKoinComponent {
            val methods = (clazz.memberFunctions + clazz.declaredMemberFunctions).distinct()
                .filter { it.hasAnnotation<EventHandle>() }
                .map {
                    it to it.parameters
                        .filter { param -> !param.isOptional }
                }
                .mapNotNull { if (it.second.size == 2) it.first to it.second[1] else null }

            val clazzObj = (obj ?: clazz.objectInstance) ?: getKoin().getOrNull<Any>(clazz)

            val methodCache: MutableMap<KClass<*>, List<Pair<KFunction<*>, KParameter>>> = ConcurrentHashMap()

            private val log by logger<EventBus>()

            override suspend fun invoke(event: Any) {
                val eventClass = event::class
                methodCache.getOrPut(eventClass) {
                    methods.filter { it.second.type.classifier == eventClass || eventClass.isSubclassOf(it.second.type.jvmErasure) }
                }.onEach {
                    val method = it.first
                    try {
                        if (method.isSuspend) method.callSuspend(clazzObj, event) else method.call(
                            clazzObj,
                            event
                        )
                    } catch (e: Throwable) {
                        log.error(
                            "Failed to call method ${method.name} in ${clazz.qualifiedName}",
                            e
                        )
                    }
                }
            }

            override fun toString(): String {
                return "ClassHandler(clazz=$clazz)"
            }
        }
    }
}