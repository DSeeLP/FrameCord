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

package io.github.dseelp.framecord.core.utils

import io.github.classgraph.ClassGraph
import io.github.dseelp.framecord.api.plugins.Plugin
import io.github.dseelp.framecord.api.utils.CriterionBuilder
import io.github.dseelp.framecord.api.utils.IReflectionUtils
import io.github.dseelp.framecord.api.utils.koin.CordKoinComponent
import java.util.concurrent.Executors
import kotlin.reflect.KClass

@OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
class ReflectionUtilsImpl : IReflectionUtils, CordKoinComponent {
    val scanExecutor = Executors.newFixedThreadPool(4)
    override fun findClasses(
        packages: Array<String>,
        criteria: CriterionBuilder,
        plugins: Array<Plugin>
    ): Array<KClass<Any>> = findClasses(packages, criteria, plugins.map { it::class.java.classLoader }.toTypedArray())


    @Suppress("UNCHECKED_CAST")
    override fun findClasses(
        packages: Array<String>,
        criteria: CriterionBuilder,
        classLoaders: Array<ClassLoader>
    ): Array<KClass<Any>> {
        return ClassGraph().enableClassInfo().acceptPackages(*packages)
            .apply { classLoaders.forEach { addClassLoader(it) } }
            .scan()
            .allClasses
            .filter { packages.any { n -> it.packageName.startsWith(n) } }
            .filter { !it.name.contains('$') }
            .mapNotNull { kotlin.runCatching { it.loadClass().kotlin }.getOrNull() }
            .filter {
                try {
                    criteria.check(it)
                } catch (ex: UnsupportedOperationException) {
                    false
                }
            }
            .toTypedArray() as Array<KClass<Any>>
    }

}