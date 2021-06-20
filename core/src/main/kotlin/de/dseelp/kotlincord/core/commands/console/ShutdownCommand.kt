/*
 * Created by Dirk on 20.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.commands.console

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.literal
import de.dseelp.kotlincord.api.Cord
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.command.Sender
import de.dseelp.kotlincord.api.utils.CommandScope
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import org.koin.core.component.inject

@OptIn(InternalKotlinCordApi::class)
class ShutdownCommand : Command<Sender>, CordKoinComponent {

    val cord: Cord by inject()

    override val scopes: Array<CommandScope> = CommandScope.ALL
    override val node: CommandNode<Sender> = literal("shutdown") {
        execute {
            cord.shutdown()
        }
    }
}