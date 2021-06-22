/*
 * Created by Dirk on 22.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.plugins

import de.dseelp.kotlincord.api.Version
import java.io.File
import java.nio.file.Path
import kotlin.reflect.KClass


@Retention
@Target(AnnotationTarget.CLASS)
annotation class PluginInfo(
    val group: String,
    val name: String,
    val version: String,
    val description: String = "",
    val authors: Array<String>
)

fun PluginInfo.meta(file: File, dataFolder: Path, mainClass: KClass<out Plugin>) =
    PluginMeta(group, name, Version.parse(version), description, authors, file, dataFolder, mainClass)
