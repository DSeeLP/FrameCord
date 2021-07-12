/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.plugin.repository

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.json.toJson
import de.dseelp.kotlincord.api.configs.file
import de.dseelp.kotlincord.api.configs.toFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private object Spec : ConfigSpec("") {
        val repositories by optional(arrayOf<String>())
    }

    companion object {

        private var configs = mapOf<Path, Config>()
        private val mutex = Mutex()

        private val baseConfig = Config { addSpec(Spec) }

        private suspend fun loadCfg(file: Path): Config = mutex.withLock {
            val config = configs[file] ?: run {
                val cfg = baseConfig.from.json.file(file)
                configs = configs + (file to cfg)
                cfg
            }
            config.toJson.toFile(file)
            config
        }

        private suspend fun saveCfg(path: Path) {
            saveCfg(loadCfg(path), path)
        }

        private suspend fun saveCfg(config: Config, path: Path) = mutex.withLock {
            config.toJson.toFile(path)
        }

        suspend fun load(file: Path): RepositoryConfig {
            val config = loadCfg(file)
            return RepositoryConfig(config[Spec.repositories])
        }

        suspend fun add(file: Path, vararg repositories: String) {
            val cfg = loadCfg(file)
            set(file, (cfg[Spec.repositories] + repositories).distinct().toTypedArray(), cfg)
        }

        suspend fun remove(file: Path, vararg repositories: String) {
            val cfg = loadCfg(file)
            set(file, cfg[Spec.repositories].filter { !repositories.contains(it) }.distinct().toTypedArray(), cfg)
        }

        suspend fun set(file: Path, repositories: Array<String>, config: Config? = null) {
            val cfg = config ?: loadCfg(file)
            cfg[Spec.repositories] = repositories
            saveCfg(cfg, file)
        }
    }
}
