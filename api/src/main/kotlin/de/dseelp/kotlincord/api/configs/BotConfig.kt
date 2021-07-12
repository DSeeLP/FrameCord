/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.configs

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import de.dseelp.kotlincord.api.randomAlphanumeric
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class BotConfig(val instanceId: String, val debug: Boolean) {
    companion object : ConfigSpec("") {
        val instanceId by optional(randomAlphanumeric(4))
        val debugMode by optional(false)

        fun fromConfig(config: Config): BotConfig = BotConfig(config[instanceId], config[debugMode])
    }
}
