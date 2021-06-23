/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.future.asDeferred
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.concurrent.Task
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

fun <T> RestAction<T>.submitAsync(shouldQueue: Boolean = true) = submit(shouldQueue).asDeferred()

@ExperimentalTime
suspend fun <T> RestAction<T>.submitAfterAsync(duration: Duration, shouldQueue: Boolean = true) = coroutineScope {
    return@coroutineScope async(jda.dispatcher) {
        delay(duration)
        return@async submit(shouldQueue)
    }
}

suspend fun <T> RestAction<T>.await(): T = submit().await()

@ExperimentalTime
suspend fun <T> RestAction<T>.completeAfter(duration: Duration): T {
    delay(duration)
    return complete()
}

@ExperimentalTime
suspend fun <T> RestAction<T>.queueAfter(duration: Duration, consumer: (T) -> Unit) = coroutineScope {
    launch(jda.dispatcher) {
        delay(duration)
        consumer.invoke(await())
    }
}

val JDA.dispatcher: CoroutineDispatcher
    get() = rateLimitPool.asCoroutineDispatcher()

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> Task<T>.await() = suspendCancellableCoroutine<T> {
    it.invokeOnCancellation { cancel() }
    onSuccess { r -> it.resume(r) {} }
    onError { e -> it.resumeWithException(e) }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> CompletableFuture<T>.await() = suspendCancellableCoroutine<T> {
    it.invokeOnCancellation { cancel(true) }
    whenComplete { r, e ->
        when {
            e != null -> it.resumeWithException(e)
            else -> it.resume(r) {}
        }
    }
}