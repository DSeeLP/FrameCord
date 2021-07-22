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

package de.dseelp.kotlincord.api.plugins

import de.dseelp.kotlincord.api.Version
import java.io.File
import java.nio.file.Path
import kotlin.reflect.KClass

data class PluginMeta(
    val group: String,
    val name: String,
    val version: Version,
    val description: String,
    val authors: Array<String>,
    val file: File,
    val dataFolder: Path,
    val mainClass: KClass<out Plugin>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PluginMeta) return false

        if (name != other.name) return false
        if (version != other.version) return false
        if (description != other.description) return false
        if (!authors.contentEquals(other.authors)) return false
        if (file != other.file) return false
        if (dataFolder != other.dataFolder) return false
        if (mainClass != other.mainClass) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + mainClass.qualifiedName.hashCode()
        return result
    }

}
