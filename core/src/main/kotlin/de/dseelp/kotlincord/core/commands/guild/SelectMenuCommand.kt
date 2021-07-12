package de.dseelp.kotlincord.core.commands.guild

import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.literal
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.command.GuildSender
import de.dseelp.kotlincord.api.selectionMenu
import de.dseelp.kotlincord.api.utils.CommandScope
import de.dseelp.kotlincord.core.FakePlugin

class SelectMenuCommand : Command<GuildSender> {
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.GUILD)

    @OptIn(InternalKotlinCordApi::class, dev.kord.common.annotation.KordPreview::class)
    override val node: CommandNode<GuildSender> = literal("selecttest") {
        execute {
            sender.sendMessage {
                content = "Hi"
                embed {
                    title = "Test Embed"
                    description = "This tests the new selection menus"
                }
                actionRow {
                    selectionMenu(FakePlugin) {
                        placeholder = "Test Placeholder"

                        repeat(5) { i ->
                            option("Option #$i", "option$i") {
                                onClick {
                                    val user = interaction.user.asUser()
                                    interaction.acknowledgePublic()/*.followUp {
                                        embed {
                                            title = "Option clicked"
                                            description = "Option #$i was clicked"
                                            footer {
                                                text = user.tag
                                                icon = user.avatar.url
                                            }
                                        }
                                    }*/
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}