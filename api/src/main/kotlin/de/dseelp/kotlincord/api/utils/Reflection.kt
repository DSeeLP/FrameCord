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

package de.dseelp.kotlincord.api.utils

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf

@OptIn(InternalKotlinCordApi::class)
object ReflectionUtils : CordKoinComponent {
    private val internalUtils: IReflectionUtils by inject()
    fun findClasses(packages: Array<String>, criteria: CriterionBuilder.() -> Unit) =
        internalUtils.findClasses(packages, CriterionBuilder().apply(criteria))

    fun findClasses(packages: Array<String>, criteria: Array<Criterion>) =
        internalUtils.findClasses(packages, CriterionBuilder().apply {
            criteria.onEach {
                it.assert()
            }
        })

    fun findClasses(packageName: String, criteria: CriterionBuilder.() -> Unit) =
        findClasses(arrayOf(packageName), criteria)
}

interface IReflectionUtils {
    fun findClasses(packages: Array<String>, criteria: CriterionBuilder): Array<KClass<Any>>
}

class CriterionBuilder {
    private val not = mutableListOf<Criterion>()
    private val must = mutableListOf<Criterion>()

    fun Criterion.assert() {
        must.add(this)
    }

    fun Criterion.assertNot() {
        not.add(this)
    }

    fun check(clazz: KClass<*>): Boolean = must.all { it.matches(clazz) } && not.all { !it.matches(clazz) }
}

fun interface Criterion {
    fun matches(clazz: KClass<*>): Boolean

    companion object {
        inline fun <reified T : Annotation> hasAnnotation() = Criterion { it.hasAnnotation<T>() }
        fun hasAnnotation(clazz: KClass<*>) = Criterion { it.annotations.firstOrNull { it::class == clazz } != null }
        fun isSubClassOf(clazz: KClass<*>) = Criterion { it.isSubclassOf(clazz) }
        fun isInterface() = Criterion { it.java.isInterface }
        fun isEnum() = Criterion { it.java.isEnum }
        fun isAnnotation() = Criterion { it.java.isAnnotation }
        fun isAbstract() = Criterion(KClass<*>::isAbstract)
        fun isCompanion() = Criterion(KClass<*>::isCompanion)
        fun isData() = Criterion(KClass<*>::isData)
        fun isFinal() = Criterion(KClass<*>::isFinal)
        fun isFun() = Criterion(KClass<*>::isFun)
        fun isInner() = Criterion(KClass<*>::isInner)
        fun isOpen() = Criterion(KClass<*>::isOpen)
        fun isSealed() = Criterion(KClass<*>::isSealed)
        fun isValue() = Criterion(KClass<*>::isValue)
        fun isObject() = Criterion { it.objectInstance != null }
        fun visibility(visibility: KVisibility) = Criterion { it.visibility == visibility }
    }
}