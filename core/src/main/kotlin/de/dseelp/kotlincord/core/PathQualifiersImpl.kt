/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import de.dseelp.kotlincord.api.PathQualifiers
import org.koin.dsl.module
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div

object PathQualifiersImpl : PathQualifiers {
    override val root: Path = Path("")
    override val configLocation: Path = root / "local"
    override val pluginLocation: Path = root / "plugins"

    val module = module {
        single<PathQualifiers> { this@PathQualifiersImpl }
    }
}