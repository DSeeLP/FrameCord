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
        val repositories by optional(arrayOf("https://cord.tdrstudios.de/"))
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
