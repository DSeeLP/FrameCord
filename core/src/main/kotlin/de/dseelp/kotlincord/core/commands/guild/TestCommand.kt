/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.commands.guild

import de.dseelp.kommon.command.literal
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.command.Sender
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.api.utils.messageBuilder
import net.dv8tion.jda.api.interactions.components.Button
import org.koin.core.component.inject
import java.awt.Color

@OptIn(InternalKotlinCordApi::class)
object TestCommand : CordKoinComponent {
    val eventBus by inject<EventBus>()
    val cmd = literal<Sender>("test") {
        execute {
            println("Test command executed")
            val msg = messageBuilder {
                embed {
                    title = "Url Test"
                    url = "https://discord.com"
                    field("Test 1", "Test Content 1", true)
                    field("Test 2", "Test Content 2", true)
                    field("Test 3", "Test Content 3", true)
                    field("Test Big", "Test Content Long euiivuhkweigrg")
                    color = Color.RED
                }
                actionRow {
                    +Button.primary("testIdButtonPrimary", "Primary")
                    +Button.secondary("testIdButtonSecondary", "Secondary")
                    +Button.link("https://discord.com", "Discord")
                }
                actionRow {
                    +Button.success("testIdButtonSuccess", "Success")
                    +Button.danger("testIdButtonDanger", "Danger")
                    +Button.primary("ephemeralMessage", "Lonely")
                }
            }.build()
            sender.sendMessage(msg)
        }
    }
}