/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TokenConfig(val token: String) {
    companion object : ConfigSpec("") {
        val token by optional("ENTER TOKEN HERE")

        fun fromConfig(config: Config) = TokenConfig(config[token])
    }
}