/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.utils

import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun HttpResponse.downloadFile(file: File): CompletableDeferred<Long> = coroutineScope {
    val deferred = CompletableDeferred<Long>()
    if (!status.isSuccess()) {
        deferred.complete(status.value * -1L)
    }
    val async = async { content.copyAndClose(file.writeChannel()) }
    async.invokeOnCompletion {
        if (it == null) deferred.complete(async.getCompleted())
        else deferred.completeExceptionally(it)
    }
    return@coroutineScope deferred
}