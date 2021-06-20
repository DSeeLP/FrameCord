/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.configs

import de.dseelp.kotlincord.api.configs.ConfigFormat.Utils.registerDataClassDiscoverer
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.gson.GsonConfigurationLoader
import org.spongepowered.configurate.kotlin.commented
import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import java.io.File
import java.nio.file.Path
import kotlin.reflect.KClass

fun config(
    format: ConfigFormat<out ConfigurationLoader<*>>,
    path: Path,
    copyDefaults: Boolean = true,
    configure: ConfigBuilder.() -> Unit
): Config {
    return ConfigBuilder(path).apply(configure).build(format, copyDefaults)
}

fun config(
    format: ConfigFormat<out ConfigurationLoader<*>>,
    path: File,
    copyDefaults: Boolean = true,
    configure: ConfigBuilder.() -> Unit
): Config = config(format, path.toPath(), copyDefaults, configure)

class ConfigBuilder(val path: Path) {
    var defaults: CommentedConfigurationNode? = null
    fun defaults(block: CommentedConfigurationNode.() -> Unit) {
        defaults = commented(ConfigurationOptions.defaults().shouldCopyDefaults(true), block)
    }

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

class Config(
    val loader: ConfigurationLoader<*>,
    node: ConfigurationNode,
    val defaults: CommentedConfigurationNode? = null
) {
    var node = node
        private set

    fun reload(copyDefaults: Boolean = true) {
        val temp = loader.load()
        if (copyDefaults) {
            temp.mergeFrom(defaults)
            loader.save(temp)
        }
        node = temp
    }

    fun save() {
        loader.save(node)
    }
}

class ConfigFormat<T : Any>(val clazz: KClass<T>, val initBlock: (path: Path) -> T) {
    companion object {
        val JSON = ConfigFormat(GsonConfigurationLoader::class) { path ->
            GsonConfigurationLoader.builder().path(path)
                .defaultOptions(ConfigurationOptions
                    .defaults()
                    .serializers { builder ->
                        builder.registerDataClassDiscoverer()
                    }
                )
                .build()
        }
    }

    fun build(path: Path): T = initBlock.invoke(path)

    object Utils {
        fun TypeSerializerCollection.Builder.registerDataClassDiscoverer() {
            registerAnnotatedObjects(
                ObjectMapper.factoryBuilder()
                    .addDiscoverer(dataClassFieldDiscoverer())
                    .build()
            )
        }
    }
}


