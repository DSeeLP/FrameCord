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

package io.github.dseelp.framecord.core.plugin

import io.github.dseelp.framecord.api.plugins.*
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import java.io.DataInputStream
import java.io.File
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

@io.github.dseelp.framecord.api.InternalFrameCordApi
class PluginClassLoaderImpl(file: File, parent: ClassLoader) : PluginClassLoader(file, parent) {

    private val baseDataFolder: Path = io.github.dseelp.framecord.api.PathQualifiers.PLUGIN_LOCATION

    @io.github.dseelp.framecord.api.InternalFrameCordApi
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