/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.command

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kotlincord.api.utils.CommandScope

interface Command<S : Sender> {
    val scopes: Array<CommandScope>
    val node: CommandNode<S>
}