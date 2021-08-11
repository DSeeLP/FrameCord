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

package io.github.dseelp.framecord.rest.data.responses.dialect

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("simple")
sealed class RestError {
    abstract val id: Int
    abstract val message: String
}

//@Serializable
//class SimpleRestError(override val id: Int, override val message: String): RestError()

@Serializable
@SerialName("detailed")
open class DetailedRestError(override val id: Int, override val message: String, open val details: String): RestError()

class FullRestError(val httpStatus: HttpStatusCode, override val id: Int, override val message: String): RestError()
class FullDetailedRestError(val httpStatus: HttpStatusCode, id: Int, message: String, details: String): DetailedRestError(id, message, details)


fun FullRestError.detailed(details: String) = FullDetailedRestError(httpStatus, id, message, details)