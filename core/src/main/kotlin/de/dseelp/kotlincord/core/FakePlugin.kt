/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.Version
import de.dseelp.kotlincord.api.database.DatabaseInfo
import de.dseelp.kotlincord.api.database.DatabaseRegistry
import de.dseelp.kotlincord.api.event.Listener
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.PluginData
import de.dseelp.kotlincord.api.plugins.PluginMeta
import de.dseelp.kotlincord.api.plugins.repository.RepositoryManager
import de.dseelp.kotlincord.api.utils.koin.KoinModules
import de.dseelp.kotlincord.core.commands.console.PluginCommand
import de.dseelp.kotlincord.core.commands.console.ReloadCommand
import de.dseelp.kotlincord.core.commands.console.RepositoryCommand
import de.dseelp.kotlincord.core.commands.console.ShutdownCommand
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

@Listener
@InternalKotlinCordApi
object FakePlugin : Plugin() {
    private val file = File("")

    val coreVersion: Version = CordBootstrap.version
    val fakeData: PluginData

    val repositoryManager: RepositoryManager by inject()
    val databaseRegistry: DatabaseRegistry by inject()

    init {
        loadKoinModules(CordBootstrap.defaultModules)
        KoinModules.load(this)
        fakeData = PluginData(
            ClassLoader.getSystemClassLoader(), file, PluginMeta(
                "io.github.dseelp.kotlincord",
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
        try {
            repositoryManager.reloadRepositories()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        try {
            runBlocking {
                val db = databaseRegistry.registerDatabase(this@FakePlugin, DatabaseInfo.sqlite(Path("") / "cord.db"))
                loadKoinModules(module {
                    single(named("cord")) { db }
                })
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
        register<ReloadCommand>()
        register<PluginCommand>()
        register<ShutdownCommand>()
        register<RepositoryCommand>()
    }
}
