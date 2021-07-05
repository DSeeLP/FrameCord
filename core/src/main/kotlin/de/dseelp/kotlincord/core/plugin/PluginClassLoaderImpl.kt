/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.PathQualifiers
import de.dseelp.kotlincord.api.plugins.*
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import java.io.DataInputStream
import java.io.File
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

@InternalKotlinCordApi
class PluginClassLoaderImpl(file: File, parent: ClassLoader) : PluginClassLoader(file, parent) {

    private val baseDataFolder: Path = PathQualifiers.PLUGIN_LOCATION

    @InternalKotlinCordApi
    override fun findMeta(): PluginMeta? {
        for (entry in jarFile.entries()) {
            if (entry.isDirectory || !entry.name.endsWith(".class")) continue
            val classFile = run {
                val classFile = runCatching {
                    ClassFile(DataInputStream(jarFile.getInputStream(entry)))
                }
                if (classFile.isFailure) {
                    System.err.println("Failed to load entry ${entry.name} in file ${file.name}!")
                }
                return@run classFile.getOrNull()
            } ?: continue
            if (classFile.superclass != Plugin::class.java.name) continue
            val attribute = classFile.getAttribute(AnnotationsAttribute.visibleTag)!! as AnnotationsAttribute
            val jsannotation = attribute.getAnnotation(PluginInfo::class.java.name)
            if (jsannotation == null) {
                System.err.println("Found class ${classFile.name} in file ${file.name} that extends Plugin but hasn't the PluginInfo annotation!")
                return null
            }
            val jClazz = findClass(classFile.name)
            if (jClazz == null) {
                System.err.println("Failed to find class ${entry.name}!")
                return null
            }
            @Suppress("UNCHECKED_CAST")
            val clazz = jClazz.kotlin as KClass<out Plugin>
            val annotation = clazz.findAnnotation<PluginInfo>()!!
            return annotation.meta(file, baseDataFolder / annotation.name, clazz)
        }
        return null
    }

    private val field = Plugin::class.java.getDeclaredField("_meta").apply {
        isAccessible = true
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun initializePlugin(meta: PluginMeta) {
        if (plugin != null) throw IllegalStateException("Classloader already initialized")
        val instance = meta.mainClass.objectInstance ?: meta.mainClass.createInstance()
        plugin = instance
        field.set(instance, meta)
    }

}