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
import de.dseelp.kotlincord.core.commands.InviteCommand
import de.dseelp.kotlincord.core.guild.DbGuildInfos
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
                        SchemaUtils.createMissingTablesAndColumns(InstalledPackages, DbGuildInfos)
                        commit()
                    }
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
        searchCommands("de.dseelp.kotlincord.core")
    }

    fun enable() {
        val inviteConfig = InviteCommand.getConfig()
        if (inviteConfig.enabled && inviteConfig.clientId > 0) {
            register<InviteCommand>()
        }
    }
}
