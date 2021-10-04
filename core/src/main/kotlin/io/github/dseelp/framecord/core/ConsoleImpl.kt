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

package io.github.dseelp.framecord.core

import com.log4k.Config
import com.log4k.Level
import com.log4k.Log4k
import com.log4k.SimpleEvent
import io.github.dseelp.framecord.api.console.Console
import io.github.dseelp.framecord.api.console.ConsoleColor
import io.github.dseelp.framecord.api.event.EventBus
import io.github.dseelp.framecord.api.events.ConsoleMessageEvent
import kotlinx.coroutines.*
import org.fusesource.jansi.AnsiConsole
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import kotlin.system.exitProcess


object ConsoleImpl : Console {
    val terminal: Terminal
    val reader: LineReaderImpl
    private val _lastWrittenMessages = object : ArrayList<String>() {
        override fun add(element: String): Boolean {
            if (size > 200) {
                removeAt(0)
            }
            return super.add(element)
        }
    }

    override val prompt: String
        get() = _prompt

    val version = CordBootstrap.version

    private val defaultPrompt =
        "${ConsoleColor.RED}${System.getProperty("user.name")}${ConsoleColor.DEFAULT}@${ConsoleColor.GRAY}FrameCord-$version ${ConsoleColor.WHITE}=> "

    private var _prompt = defaultPrompt

    private var readLoopJob: Job? = null

    private var readJob: Deferred<String?>? = null

    val eventBus: EventBus by inject()

    @OptIn(DelicateCoroutinesApi::class)
    override fun startReading() {
        if (readLoopJob != null) return
        readLoopJob = GlobalScope.launch {
            while (isActive) {
                withContext(Dispatchers.IO) {
                    readJob = async { kotlin.runCatching { reader.readLine(prompt)!! }.getOrNull() }
                }
                val catching = kotlin.runCatching { readJob?.await() }
                if (catching.isFailure && (catching.exceptionOrNull() is CancellationException || catching.exceptionOrNull() is UserInterruptException)) continue
                catching.exceptionOrNull()?.printStackTrace()
                val line = catching.getOrNull() ?: exitProcess(0)
                readJob = null
                eventBus.callAsync(ConsoleMessageEvent(line))
            }
        }
    }

    override fun stopReading() = runBlocking {
        readLoopJob?.cancel()
        readLoopJob = null
    }

    fun stopCurrentRead() = runBlocking {
        readJob?.cancelAndJoin()
        readJob = null
    }

    override fun resetPrompt() {
        changePrompt(defaultPrompt)
    }

    override fun changePrompt(prompt: String) {
        this._prompt = prompt
        val isReading = readLoopJob != null
        stopReading()
        reader.clearScreen()
        reader.redisplay()
        if (isReading) startReading()
    }

    init {
        terminal = TerminalBuilder.builder().streams(System.`in`, System.out).system(true).build()
        reader = LineReaderBuilder.builder().appName("FrameCord").terminal(terminal).build() as LineReaderImpl
        AnsiConsole.systemInstall()
    }

    override val lastWrittenMessages: Array<String>
        get() = _lastWrittenMessages.toTypedArray()


    var realSysOut = System.out
    private set
    var realSysErr = System.err
    private set


    fun replaceSysOut() {
        realSysOut = System.out
        realSysErr = System.err
        System.setOut(PrintStream(ActionOutputStream { Log4k.log(Level.Info, Config(tag = ""), SimpleEvent(it)) }, true))
        System.setErr(PrintStream(ActionOutputStream { Log4k.log(Level.Error, Config(tag = ""), SimpleEvent(it)) }, true))
    }

    override fun forceWriteLine(vararg messages: String) {
        messages.onEach {
            _lastWrittenMessages.add(it)
            reader.printAbove(it + System.lineSeparator())
        }
    }

    override fun forceWrite(message: String) {
        _lastWrittenMessages.add(message)
        reader.printAbove(message)
    }

    override fun newLine() {
        reader.printAbove(System.lineSeparator())
    }

    override val printStream: PrintStream by lazy { PrintStream(ActionOutputStream { forceWriteLine(it) }, true) }


    internal class ActionOutputStream(private val consumer: (message: String) -> Unit) :
        ByteArrayOutputStream() {
        private val separator = System.lineSeparator()

        @Throws(IOException::class)
        override fun flush() {
            synchronized(this) {
                super.flush()
                val record = this.toString()
                super.reset()
                if (record.isNotEmpty() && record != separator) {
                    consumer.invoke(record)
                }
            }
        }
    }

}