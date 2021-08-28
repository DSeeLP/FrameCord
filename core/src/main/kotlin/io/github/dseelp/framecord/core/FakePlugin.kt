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

package io.github.dseelp.framecord.core

import io.github.dseelp.framecord.api.Version
import io.github.dseelp.framecord.api.configs.BotConfig
import io.github.dseelp.framecord.api.event.Listener
import io.github.dseelp.framecord.api.plugins.DatabaseConfig
import io.github.dseelp.framecord.api.plugins.Plugin
import io.github.dseelp.framecord.api.plugins.PluginData
import io.github.dseelp.framecord.api.plugins.PluginMeta
import io.github.dseelp.framecord.api.plugins.repository.RepositoryManager
import io.github.dseelp.framecord.api.utils.koin.KoinModules
import io.github.dseelp.framecord.core.commands.InviteCommand
import io.github.dseelp.framecord.core.modules.ModuleManagerImpl
import io.github.dseelp.framecord.core.plugin.repository.data.InstalledPackages
import io.github.dseelp.framecord.rest.server.RestServer
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
@io.github.dseelp.framecord.api.InternalFrameCordApi
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
                "io.github.dseelp.framecord",
                "FrameCord",
                coreVersion,
                "This is a fake plugin instance for registering Listeners and Commands in FrameCord",
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
        eventBus.searchPackage("io.github.dseelp.framecord.core", FakePlugin)
        eventBus.searchPackage("io.github.dseelp.framecord.api", FakePlugin)
        val dbConfig = DatabaseConfig.load(
            this@FakePlugin,
            default = DatabaseConfig.defaultDatabaseConfig(this@FakePlugin).copy(databaseName = "cord")
        )
        try {
            runBlocking {
                val db = registerDatabase(
                    dbConfig.toDatabaseInfo(this@FakePlugin)
                )
                loadKoinModules(module {
                    single(named("cord")) { db }
                    single { dbConfig }
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
        ModuleManagerImpl
        searchCommands("io.github.dseelp.framecord.core")
    }

    fun enable() {
        val config = InviteCommand.getConfig()
        if (config.invite.enabled && getKoin().get<BotConfig>().clientId > 0) {
            register<InviteCommand>()
        }
        if (config.rest.enabled)
            RestServer.startRestServer(this)
    }
}
