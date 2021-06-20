/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.event

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf

class EventBus {
    private val handlers = mutableListOf<Handler>()

    fun call(event: Any) {
        val eventClass = event::class
        for (index in 0..handlers.lastIndex) {
            val handler = handlers.getOrNull(index) ?: continue
            if (handler is Handler.ClassHandler<*>) handler.invoke(event)
            else if (handler.clazz == eventClass || eventClass.isSubclassOf(handler.clazz)) handler.invoke(event)
        }
    }

    @InternalKotlinCordApi
    fun unregister(clazz: KClass<out Plugin>) {
        handlers.removeIf {
            it.plugin::class == clazz
        }
    }

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

    sealed class Handler(val plugin: Plugin) {
        abstract fun invoke(
            event: Any
        )

        abstract val clazz: KClass<out Any>

        class LambdaHandler<T>(
            plugin: Plugin,
            override val clazz: KClass<out Any>,
            val lambda: (event: T) -> Unit
        ) : Handler(plugin) {
            override fun invoke(
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

            override fun invoke(event: Any) {
                val eventClass = event::class
                methodCache.getOrPut(eventClass) {
                    methods.filter { it.second.type.classifier == eventClass }
                }.onEach {
                    val method = it.first
                    kotlin.runCatching { method.call(clazzObj, event) }.exceptionOrNull()?.cause?.printStackTrace()
                }
            }
        }
    }
}