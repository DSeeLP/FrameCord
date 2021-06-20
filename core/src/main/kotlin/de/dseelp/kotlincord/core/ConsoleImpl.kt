/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core

import de.dseelp.kotlincord.api.console.Console
import de.dseelp.kotlincord.api.console.ConsoleColor
import de.dseelp.kotlincord.api.event.EventBus
import de.dseelp.kotlincord.api.events.ConsoleMessageEvent
import de.dseelp.kotlincord.api.logging.LogManager
import de.dseelp.kotlincord.api.logging.logger
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
    val defaultLogger by logger(LogManager.ROOT)
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
        "${ConsoleColor.RED}${System.getProperty("user.name")}${ConsoleColor.DEFAULT}@${ConsoleColor.GRAY}KotlinCord-$version ${ConsoleColor.WHITE}=> "

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
                eventBus.call(ConsoleMessageEvent(line))
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
        reader = LineReaderBuilder.builder().appName("KotlinCord").terminal(terminal).build() as LineReaderImpl
        AnsiConsole.systemInstall()
    }

    override val lastWrittenMessages: Array<String>
        get() = _lastWrittenMessages.toTypedArray()


    fun replaceSysOut() {
        System.setOut(PrintStream(ActionOutputStream { defaultLogger.info(it) }, true))
        System.setErr(PrintStream(ActionOutputStream { defaultLogger.error(it) }, true))
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