package de.dseelp.kotlincord.frontend.backend

import de.dseelp.kotlincord.api.configs.ConfigFormat
import de.dseelp.kotlincord.api.configs.config
import org.spongepowered.configurate.kotlin.extensions.get
import kotlin.io.path.div

data class BackendConfig(val host: String, val port: Int) {
    companion object {
        private val configStructure = config(ConfigFormat.JSON, BackendPlugin.dataFolder / "config.json") {
            defaults {
                get(BackendConfig("0.0.0.0", 3476))
            }
        }

        fun load(): BackendConfig {
            configStructure.reload()
            return configStructure.node.get<BackendConfig>()!!
        }
    }
}