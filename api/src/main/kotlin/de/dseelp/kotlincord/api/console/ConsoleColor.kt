/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.console

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.ansi

enum class ConsoleColor(val colorName: String, val index: Char, val ansiCode: String) {
    DEFAULT("default", 'r', ansi().reset().fg(Ansi.Color.DEFAULT).boldOff().toString()),
    BLACK("black", '0', ansi().reset().fg(Ansi.Color.BLACK).boldOff().toString()),
    DARK_BLUE("dark_blue", '1', ansi().reset().fg(Ansi.Color.BLUE).boldOff().toString()),
    GREEN("green", '2', ansi().reset().fg(Ansi.Color.GREEN).boldOff().toString()),
    CYAN("cyan", '3', ansi().reset().fg(Ansi.Color.CYAN).boldOff().toString()),
    DARK_RED("dark_red", '4', ansi().reset().fg(Ansi.Color.RED).boldOff().toString()),
    PURPLE("purple", '5', ansi().reset().fg(Ansi.Color.MAGENTA).boldOff().toString()),
    ORANGE("orange", '6', ansi().reset().fg(Ansi.Color.RED).fg(Ansi.Color.YELLOW).boldOff().toString()),
    GRAY("gray", '7', ansi().reset().fg(Ansi.Color.WHITE).boldOff().toString()),
    DARK_GRAY("dark_gray", '8', ansi().reset().fg(Ansi.Color.BLACK).bold().toString()),
    BLUE("blue", '9', ansi().reset().fg(Ansi.Color.BLUE).bold().toString()),
    LIGHT_GREEN("light_green", 'a', ansi().reset().fg(Ansi.Color.GREEN).bold().toString()),
    AQUA("aqua", 'b', ansi().reset().fg(Ansi.Color.CYAN).bold().toString()),
    RED("red", 'c', ansi().reset().fg(Ansi.Color.RED).bold().toString()),
    PINK("pink", 'd', ansi().reset().fg(Ansi.Color.MAGENTA).bold().toString()),
    YELLOW("yellow", 'e', ansi().reset().fg(Ansi.Color.YELLOW).bold().toString()),
    WHITE("white", 'f', ansi().reset().fg(Ansi.Color.WHITE).bold().toString());

    override fun toString() = ansiCode

    companion object {
        fun toColouredString(triggerChar: Char, text: String): String {
            var changeableText = text
            for (consoleColour in values()) {
                changeableText =
                    changeableText.replace(triggerChar.toString() + "" + consoleColour.index, consoleColour.ansiCode)
            }
            return changeableText
        }

        fun getByChar(index: Char): ConsoleColor? = values().firstOrNull { it.index == index }

        fun getLastColour(triggerChar: Char, text: String): ConsoleColor? {
            @Suppress("NAME_SHADOWING")
            var text = text
            text = text.trim { it <= ' ' }
            return if (text.length > 2 && text[text.length - 2] == triggerChar) {
                getByChar(text[text.length - 1])
            } else null
        }
    }
}