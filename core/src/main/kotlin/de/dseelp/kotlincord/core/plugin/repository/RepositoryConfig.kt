/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin.repository

import de.dseelp.kotlincord.api.configs.Config
import de.dseelp.kotlincord.api.configs.ConfigFormat
import de.dseelp.kotlincord.api.configs.config
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.nio.file.Path

@ConfigSerializable
data class RepositoryConfig(val repositories: Array<String>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RepositoryConfig) return false

        if (!repositories.contentEquals(other.repositories)) return false

        return true
    }

    override fun hashCode(): Int {
        return repositories.contentHashCode()
    }

    companion object {
        private fun loadCfg(file: Path) = config(ConfigFormat.JSON, file) {
            defaults {
                get(RepositoryConfig(arrayOf()))
            }
        }

        fun load(file: Path): RepositoryConfig {
            return loadCfg(file).node.get<RepositoryConfig>()!!
        }

        private fun Config.get(): RepositoryConfig = node.get<RepositoryConfig>()!!

        fun add(file: Path, repository: String) {
            val cfg = loadCfg(file)
            val config = cfg.get()
            cfg.node.set(RepositoryConfig(config.repositories + repository))
            cfg.save()
        }

        fun remove(file: Path, repository: String) {
            val cfg = loadCfg(file)
            val config = cfg.get()
            cfg.node.set(RepositoryConfig(config.repositories.filter { it != repository }.toTypedArray()))
            cfg.save()
        }
    }
}
