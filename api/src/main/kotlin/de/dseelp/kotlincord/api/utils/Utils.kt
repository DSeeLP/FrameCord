/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.utils

inline fun <reified T : Any> merge(vararg arrays: Array<T>): Array<T> {
    var result: Array<T?> = arrayOfNulls(0)
    for (array in arrays) {
        result += array
    }
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}