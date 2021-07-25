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

package de.dseelp.kotlincord.api.configs

import com.uchuhimo.konf.source.Loader
import com.uchuhimo.konf.source.Writer
import de.dseelp.kotlincord.api.configs.ConfigFormat.Utils.registerDataClassDiscoverer
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader
import org.spongepowered.configurate.kotlin.commented
import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import java.io.File
import java.nio.file.Path
import kotlin.reflect.KClass

@Deprecated("Replaced with another library konf")
fun config(
    format: ConfigFormat<out ConfigurationLoader<*>>,
    path: Path,
    copyDefaults: Boolean = true,
    configure: ConfigBuilder.() -> Unit
): Config {
    return ConfigBuilder(path).apply(configure).build(format, copyDefaults)
}

@Deprecated("Replaced with another library konf")
fun config(
    format: ConfigFormat<out ConfigurationLoader<*>>,
    path: File,
    copyDefaults: Boolean = true,
    configure: ConfigBuilder.() -> Unit
): Config = config(format, path.toPath(), copyDefaults, configure)

@Deprecated("Replaced with another library konf")
class ConfigBuilder(val path: Path) {
    @Deprecated("Replaced with another library konf")
    var defaults: CommentedConfigurationNode? = null

    @Deprecated("Replaced with another library konf")
    fun defaults(block: CommentedConfigurationNode.() -> Unit) {
        defaults = commented(ConfigurationOptions.defaults().shouldCopyDefaults(true), block)
    }

    @Deprecated("Replaced with another library konf")
    fun build(format: ConfigFormat<out ConfigurationLoader<*>>, copyDefaults: Boolean = true): Config {
        val loader = format.build(path)
        val rootNode = loader.load()
        if (copyDefaults) defaults?.let {
            rootNode.mergeFrom(it)
            loader.save(rootNode)
        }
        return Config(loader, rootNode, defaults)
    }
}

@Deprecated("Replaced with another library konf")
class Config(
    val loader: ConfigurationLoader<*>,
    node: ConfigurationNode,
    val defaults: CommentedConfigurationNode? = null
) {
    @Deprecated("Replaced with another library konf")
    var node = node
        private set

    @Deprecated("Replaced with another library konf")
    fun reload(copyDefaults: Boolean = true) {
        val temp = loader.load()
        if (copyDefaults) {
            temp.mergeFrom(defaults)
            loader.save(temp)
        }
        node = temp
    }

    @Deprecated("Replaced with another library konf")
    fun save() {
        loader.save(node)
    }
}

@Deprecated("Replaced with another library konf")
class ConfigFormat<T : Any>(val clazz: KClass<T>, val initBlock: (path: Path) -> T) {
    companion object {
        @Deprecated("Replaced with another library konf")
        val JSON = ConfigFormat(JacksonConfigurationLoader::class) { path ->
            JacksonConfigurationLoader.builder().path(path)
                .defaultOptions(ConfigurationOptions
                    .defaults()
                    .serializers { builder ->
                        builder.registerDataClassDiscoverer()
                    }
                )
                .build()
        }
    }

    @Deprecated("Replaced with another library konf")
    fun build(path: Path): T = initBlock.invoke(path)

    @Deprecated("Replaced with another library konf")
    object Utils {

        @Deprecated("Replaced with another library konf")
        fun TypeSerializerCollection.Builder.registerDataClassDiscoverer() {
            registerAnnotatedObjects(
                ObjectMapper.factoryBuilder()
                    .addDiscoverer(dataClassFieldDiscoverer())
                    .build()
            )
        }
    }
}

fun Loader.file(path: Path, optional: Boolean = this.optional): com.uchuhimo.konf.Config {
    val file = path.toFile()
    if (!file.exists()) file.createNewFile()
    return file(file, optional)
}

fun Writer.toFile(path: Path) = toFile(path.toFile())


