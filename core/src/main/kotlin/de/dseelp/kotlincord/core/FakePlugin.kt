/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.Version
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.PluginData
import de.dseelp.kotlincord.api.plugins.PluginMeta
import de.dseelp.kotlincord.api.utils.koin.KoinModules
import de.dseelp.kotlincord.core.commands.console.PluginCommand
import de.dseelp.kotlincord.core.commands.console.ReloadCommand
import de.dseelp.kotlincord.core.commands.console.ShutdownCommand
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

@InternalKotlinCordApi
object FakePlugin : Plugin() {
    private val file = File("")

    val coreVersion: Version = CordBootstrap.version
    val fakeData: PluginData

    init {
        loadKoinModules(CordBootstrap.defaultModules)
        KoinModules.load(this)
        fakeData = PluginData(
            ClassLoader.getSystemClassLoader(), file, PluginMeta(
                "KotlinCord",
                coreVersion,
                "This is a fake plugin instance for registering Listeners and Commands in KotlinCord",
                arrayOf("DSeeLP"),
                file,
                Path("") / "config",
                this::class
            ),
            this
        )
        (Plugin::class.declaredMemberProperties.first { it.name == "_meta" }.javaField!!.apply { isAccessible = true }
            .set(
                this,
                fakeData.meta
            ))
        eventBus.searchPackage("de.dseelp.kotlincord.core", FakePlugin)
        eventBus.searchPackage("de.dseelp.kotlincord.api", FakePlugin)
        register<ReloadCommand>()
        register<PluginCommand>()
        register<ShutdownCommand>()
    }
}