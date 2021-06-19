/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.api.console

import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import java.io.PrintStream

@OptIn(InternalKotlinCordApi::class)
interface Console : CordKoinComponent {
    val lastWrittenMessages: Array<String>
    fun forceWriteLine(vararg messages: String)
    fun forceWrite(message: String)
    fun newLine()

    val printStream: PrintStream

    val prompt: String

    fun changePrompt(prompt: String)

    fun startReading()

    fun stopReading()

    fun resetPrompt()

}