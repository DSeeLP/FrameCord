/*
 * Copyright (c) 2021 DSeeLP & FrameCord contributors
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

package io.github.dseelp.framecord.api.utils

object CharUtils {
    val numberCharacters = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val lowercaseCharacters = arrayOf(
        'a',
        'b',
        'c',
        'd',
        'e',
        'f',
        'g',
        'h',
        'i',
        'j',
        'k',
        'l',
        'm',
        'n',
        'o',
        'p',
        'q',
        'r',
        's',
        't',
        'u',
        'v',
        'w',
        'x',
        'y',
        'z',
        'ü',
        'ö',
        'ä',
        'ß'
    )
    val uppercaseCharacters = arrayOf(
        'A',
        'B',
        'C',
        'D',
        'E',
        'F',
        'G',
        'H',
        'I',
        'J',
        'K',
        'L',
        'M',
        'N',
        'O',
        'P',
        'Q',
        'R',
        'S',
        'T',
        'U',
        'V',
        'W',
        'X',
        'Y',
        'Z',
        'Ü',
        'Ö',
        'Ä'
    )
    val specialCharacters = arrayOf(
        '`',
        '~',
        '!',
        '@',
        '#',
        '$',
        '%',
        '^',
        '&',
        '*',
        '(',
        ')',
        '-',
        '_',
        '=',
        '+',
        '[',
        ']',
        '\\',
        '{',
        '}',
        '|',
        ';',
        '\'',
        ':',
        '"',
        ',',
        '.',
        '/',
        '<',
        '>',
        '?'
    )

    val allCharacters = characters(numbers = true, lowercase = true, uppercase = true, special = true)
    val allCharactersAndSpace =
        characters(numbers = true, lowercase = true, uppercase = true, special = true, includeSpace = true)

    fun characters(
        numbers: Boolean = false,
        lowercase: Boolean = false,
        uppercase: Boolean = false,
        special: Boolean = false,
        includeSpace: Boolean = false
    ): CharArray {
        val final = mutableListOf<Char>()
        if (numbers) final.addAll(numberCharacters)
        if (lowercase) final.addAll(lowercaseCharacters)
        if (uppercase) final.addAll(uppercaseCharacters)
        if (special) final.addAll(specialCharacters)
        if (includeSpace) final.add(' ')
        return final.toCharArray()
    }

    fun String.validate(allowedCharacters: CharArray): Boolean {
        return toCharArray().any { !allowedCharacters.contains(it) }
    }
}