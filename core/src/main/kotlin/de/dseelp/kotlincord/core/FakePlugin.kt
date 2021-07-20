/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.Version
import de.dseelp.kotlincord.api.event.Listener
import de.dseelp.kotlincord.api.plugins.DatabaseConfig
import de.dseelp.kotlincord.api.plugins.Plugin
import de.dseelp.kotlincord.api.plugins.PluginData
import de.dseelp.kotlincord.api.plugins.PluginMeta
import de.dseelp.kotlincord.api.plugins.repository.RepositoryManager
import de.dseelp.kotlincord.api.utils.koin.KoinModules
import de.dseelp.kotlincord.core.plugin.repository.data.InstalledPackages
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
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
                Path("") / "local",
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
            runBlocking {
                val db = registerDatabase(
                    DatabaseConfig.load(
                        this@FakePlugin,
                        default = DatabaseConfig.defaultDatabaseConfig(this@FakePlugin).copy(databaseName = "cord")
                    ).toDatabaseInfo(this@FakePlugin)
                )
                loadKoinModules(module {
                    single(named("cord")) { db }
                })
                database {
                    transaction {
                        SchemaUtils.createMissingTablesAndColumns(InstalledPackages)
                        commit()
                    }
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
        searchCommands("de.dseelp.kotlincord.core")
    }
}
