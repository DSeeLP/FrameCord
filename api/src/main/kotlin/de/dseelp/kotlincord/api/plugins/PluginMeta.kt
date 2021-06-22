/*
 * Created by Dirk on 22.6.2021.
 * © Copyright by DSeeLP
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
