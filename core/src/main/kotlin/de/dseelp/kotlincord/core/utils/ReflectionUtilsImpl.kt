/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.utils

import de.dseelp.kotlincord.api.utils.CriterionBuilder
import de.dseelp.kotlincord.api.utils.IReflectionUtils
import io.github.classgraph.ClassGraph
import java.util.concurrent.Executors
import kotlin.reflect.KClass

class ReflectionUtilsImpl : IReflectionUtils {
    val scanExecutor = Executors.newFixedThreadPool(4)

    @Suppress("UNCHECKED_CAST")
    override fun findClasses(packages: Array<String>, criteria: CriterionBuilder) =
        ClassGraph().enableClassInfo().acceptPackages(*packages)
            .scan()
            .allClasses
            .map { it.loadClass().kotlin }
            .filter {
                try {
                    criteria.check(it)
                } catch (ex: UnsupportedOperationException) {
                    false
                }
            }
            .toTypedArray() as Array<KClass<Any>>
}