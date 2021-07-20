/*
 * Copyright (c) 2021 KotlinCord team & contributors
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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