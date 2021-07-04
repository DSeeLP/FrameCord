/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.event

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.bot
import de.dseelp.kotlincord.api.events.PluginDisableEvent
import de.dseelp.kotlincord.api.events.PluginEventType
import de.dseelp.kotlincord.api.logging.logger
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.PluginLoader
import de.dseelp.kotlincord.api.utils.Criterion
import de.dseelp.kotlincord.api.utils.ReflectionUtils
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import kotlinx.coroutines.*
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf

@OptIn(InternalKotlinCordApi::class)
@Listener
class EventBus : CordKoinComponent {
    private val handlers = mutableListOf<Handler>()

    private val eventExecutorService = Executors.newFixedThreadPool(4)


    suspend fun callAsync(event: Any, async: Boolean = false) {
        val eventClass = event::class
        withContext(bot.coroutineContext) {
            val executeFunction = suspend {
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

    @InternalKotlinCordApi
    fun unregister(clazz: KClass<out Plugin>) {
        handlers.removeIf {
            it.plugin::class == clazz
        }
    }

    private val pluginLoader: PluginLoader by inject()
    private val log by logger<EventBus>()

    fun searchPackages(plugin: Plugin? = null, vararg packages: String) {
        ReflectionUtils.findClasses(packages.toList().toTypedArray()) {
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
            addHandler(Handler.ClassHandler(p, clazz))
        }
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
    @InternalKotlinCordApi
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

        @OptIn(InternalKotlinCordApi::class)
        class ClassHandler<T : Any>(plugin: Plugin, override val clazz: KClass<out T>, obj: T? = null) :
            Handler(plugin),
            CordKoinComponent {
            val methods = clazz.declaredMemberFunctions
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
                    methods.filter { it.second.type.classifier == eventClass }
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
                            if (e.cause != null) e.cause else e
                        )
                    }
                }
            }
        }
    }
}