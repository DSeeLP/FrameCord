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

package io.github.dseelp.framecord.rest.client.response

import io.github.dseelp.framecord.rest.data.responses.dialect.RestError
import io.github.dseelp.framecord.rest.data.responses.dialect.RestResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

//@Serializable(RestResultSerializer::class)
sealed class RestResult<T>

//@Serializable
//@SerialName("failed")
data class FailedRestResult<T>(val error: RestError) : RestResult<T>()

//@Serializable
//@SerialName("ok")
data class FineRestResult<T>(val response: T) : RestResult<T>()

val <T> RestResult<T>.fine
    get() = this as FineRestResult<T>

val <T> RestResult<T>.failed
    get() = this as FailedRestResult<T>

class RestResultSerializer<T>(private val dataSerializer: KSerializer<T?>) : KSerializer<RestResult<T>> {
    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): RestResult<T> {
        //val e = decoder.decodeNullableSerializableValue(RestError.serializer().nullable)
        //val v = decoder.decodeNullableSerializableValue(dataSerializer)
        //val response = RestResponse<T>(e, v)
        val response = decoder.decodeSerializableValue(RestResponse.serializer(dataSerializer))
        when {
            response.error == null && response.response == null -> throw SerializationException("")
            response.response != null && response.error != null -> throw SerializationException("")
            response.error != null -> return FailedRestResult(response.error!!)
            response.response != null -> return FineRestResult(response.response!!)
        }
        throw SerializationException()
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RestResult") {
        element<RestError?>("error")
        element("response", dataSerializer.descriptor.nullable, isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: RestResult<T>) {
        val response = when (value) {
            is FailedRestResult -> RestResponse<T>(value.error, null)
            is FineRestResult -> RestResponse(null, value.response)
        }
        encoder.encodeSerializableValue(RestResponse.serializer(dataSerializer), response as RestResponse<T?>)
    }
}