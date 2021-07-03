package de.dseelp.kotlincord.frontend.backend

import de.dseelp.kotlincord.api.plugins.PluginComponent
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.core.component.inject

object KtorServer: BackendComponent {
    private var _server: ApplicationEngine? = null
    val server: ApplicationEngine
        get() = _server!!

    val config: BackendConfig by inject()

    internal fun initialize() {
        if (_server != null) throw IllegalStateException("The KtorServer was already initialized")
        _server = embeddedServer(Netty, host = config.host, port = config.port) {

        }
    }


    fun start() {
        if (_server == null) throw IllegalStateException("The KtorServer must be initialized before it can be started!")
        server.start()
    }
}