/**
 * A terminal for a [CommandShell]: one scrolling text pane holding the transcript, then the prompt, then what is being
 * typed. Everything before the input is read-only, and output from a command that finishes later is inserted above the
 * prompt, so it never lands in the middle of what is being typed. Commands run off the event thread.
 */
package org.simbrain.console

import com.formdev.flatlaf.FlatLaf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.simbrain.util.Theme
import org.simbrain.util.swingDispatcher
import org.simbrain.util.swingInvokeLater
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.Insets
import java.awt.datatransfer.DataFlavor
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import kotlin.math.roundToInt

private const val MAX_TRANSCRIPT_LINES = 5000
private const val STATUS_DELAY_MS = 500L
private const val STATUS_INTERVAL_MS = 100L
private const val BAR_WIDTH = 20

/** The status line for a running command: a bar if its size is known, otherwise a count. */
internal fun statusText(progress: Progress?): String {
    val hint = "Ctrl+C or Esc to cancel."
    progress ?: return "Working. $hint"
    val total = progress.total
    if (total == null || total <= 0) return "${progress.what}: ${"%,d".format(progress.done)} objects. $hint"
    val fraction = (progress.done.toDouble() / total).coerceIn(0.0, 1.0)
    val filled = (fraction * BAR_WIDTH).roundToInt()
    val bar = "[" + "#".repeat(filled) + ".".repeat(BAR_WIDTH - filled) + "]"
    return "$bar ${(fraction * 100).roundToInt()}% ${progress.what} ${"%,d".format(progress.done)} of ${"%,d".format(total)}. $hint"
}

/**
 * @param relayToTerminal for a panel in a Console window, which closes when the workspace is replaced: where to send
 * commands that replace the workspace
 */
class CommandConsolePanel(
    workspace: Workspace,
    desktop: SimbrainDesktop? = null,
    relayToTerminal: ((String) -> Unit)? = null
) : JPanel(BorderLayout()) {

    private val shell = CommandShell(workspace, desktop).apply {
        this.relayToTerminal = relayToTerminal
        onClearScreen = { swingInvokeLater { clearTranscript() } }
    }

    private val commandScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Commands waiting to run. Each runs to the end before the next starts, so their output never interleaves. */
    private val queue = Channel<QueuedLine>(Channel.UNLIMITED)

    private val plain = SimpleAttributeSet().apply {
        StyleConstants.setFontFamily(this, Font.MONOSPACED)
        StyleConstants.setFontSize(this, Theme.body.size)
    }

    private val bold = SimpleAttributeSet(plain).apply { StyleConstants.setBold(this, true) }

    internal val textPane = ConsoleTextPane().apply {
        margin = Insets(4, 6, 4, 6)
        // Lets the tab key reach its binding
        focusTraversalKeysEnabled = false
    }

    private val document = textPane.styledDocument as AbstractDocument

    /** The document is the transcript, then a status line while a long command runs, then the prompt, then the input. */
    private var statusStart = 0

    private var promptStart = 0

    private var inputStart = 0

    @Volatile
    private var runningJob: Job? = null

    /** True while the panel itself is changing the document, which the read-only protection does not apply to. */
    private var editing = false

    private val history = mutableListOf<String>()

    private var historyIndex = 0

    init {
        add(JScrollPane(textPane).apply { border = BorderFactory.createEmptyBorder() }, BorderLayout.CENTER)
        document.documentFilter = InputFilter()
        writePrompt()
        insertOutput(
            "Simbrain commands. Enter ${bold("help")} for a list, ${bold("examples")} for some to copy and paste, " +
                    "or ${bold("tour")} for a step-by-step walkthrough."
        )
        commandScope.launch { runQueue() }

        bindKey("ENTER", "console-enter") { enterLine() }
        bindKey("UP", "console-previous") { moveThroughHistory(-1) }
        bindKey("DOWN", "console-next") { moveThroughHistory(1) }
        bindKey("TAB", "console-complete") { completeInput() }
        bindKey("ESCAPE", "console-cancel") { cancelRunning() }
        bindKey("ctrl C", "console-interrupt") { interrupt() }
        bindKey("HOME", "console-home") { textPane.caretPosition = inputStart }
        bindKey("meta LEFT", "console-home") { textPane.caretPosition = inputStart }

        textPane.addAncestorListener(object : AncestorListener {
            override fun ancestorAdded(event: AncestorEvent) {
                textPane.requestFocusInWindow()
            }

            override fun ancestorRemoved(event: AncestorEvent) {}

            override fun ancestorMoved(event: AncestorEvent) {}
        })
    }

    /** Run [line] as though it had been entered. The job finishes once its output is in the transcript. */
    fun submit(line: String): Job {
        val queued = QueuedLine(line)
        if (shell.isImmediate(line)) {
            commandScope.launch { runAndShow(queued) }
        } else {
            queue.trySend(queued)
        }
        return queued.done
    }

    /** Stop running commands. Called when the console is closed. */
    fun close() {
        queue.close()
        commandScope.cancel()
    }

    private suspend fun runQueue() = coroutineScope {
        for (queued in queue) {
            shell.progress = null
            val job = launch { runAndShow(queued, showsProgress = true) }
            runningJob = job
            job.join()
            runningJob = null
        }
    }

    /** Copy if text is selected, as Ctrl+C does on Windows, otherwise cancel what is running or discard the input. */
    private fun interrupt() {
        when {
            !textPane.selectedText.isNullOrEmpty() -> textPane.copy()
            runningJob != null -> cancelRunning()
            else -> replaceInput("")
        }
    }

    /** Cancel the running command and drop any waiting to run. */
    private fun cancelRunning() {
        val job = runningJob ?: return
        var dropped = 0
        while (true) {
            val waiting = queue.tryReceive().getOrNull() ?: break
            waiting.done.complete()
            dropped++
        }
        job.cancel()
        if (dropped > 0) insertOutput("Dropped $dropped waiting command(s).")
    }

    /**
     * Shows the line after the prompt, runs it, and shows its output.
     *
     * @param showsProgress whether to show a status line if the command takes a while
     */
    private suspend fun runAndShow(queued: QueuedLine, showsProgress: Boolean = false) = coroutineScope {
        val status = if (showsProgress) launch { showStatusWhileRunning() } else null
        try {
            withContext(swingDispatcher) { insertOutput(shell.prompt, queued.line) }
            val result = shell.executeStyled(queued.line)
            status?.cancelAndJoin()
            withContext(swingDispatcher) {
                setStatus(null)
                if (result.isNotEmpty()) insertOutput(result)
                refreshPrompt()
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable + swingDispatcher) {
                setStatus(null)
                insertOutput(cancelledText())
                refreshPrompt()
            }
            throw e
        } finally {
            queued.done.complete()
        }
    }

    private fun cancelledText() =
        if (shell.progress?.what == "Searching") {
            "Cancelled. A longer path, such as net1/Neuron_1, searches less."
        } else {
            "Cancelled."
        }

    private suspend fun showStatusWhileRunning() {
        delay(STATUS_DELAY_MS)
        while (true) {
            val text = statusText(shell.progress)
            withContext(swingDispatcher) { setStatus(text) }
            delay(STATUS_INTERVAL_MS)
        }
    }

    private fun bindKey(key: String, name: String, action: () -> Unit) {
        textPane.inputMap.put(KeyStroke.getKeyStroke(key), name)
        textPane.actionMap.put(name, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) = action()
        })
    }

    private fun enterLine() {
        val line = currentInput()
        replaceInput("")
        if (line.isNotBlank() && history.lastOrNull() != line) history += line
        historyIndex = history.size
        submit(line)
    }

    private fun moveThroughHistory(step: Int) {
        historyIndex = (historyIndex + step).coerceIn(0, history.size)
        replaceInput(history.getOrElse(historyIndex) { "" })
    }

    private fun completeInput() {
        val completion = shell.complete(currentInput())
        replaceInput(completion.line)
        if (completion.options.isNotEmpty()) insertOutput(completion.options.joinToString("  ") { quoted(it) })
    }

    /** Multiple lines run one after another, as in a terminal, and text after the last line break stays as input. */
    internal fun pasteText(text: String) {
        val lines = text.lines()
        if (lines.size == 1) {
            if (textPane.caretPosition < inputStart) textPane.caretPosition = document.length
            textPane.replaceSelection(text)
            return
        }
        val complete = listOf(currentInput() + lines.first()) + lines.subList(1, lines.size - 1)
        replaceInput(lines.last())
        complete.filter { it.isNotBlank() }.forEach(::submit)
    }

    private fun currentInput() = document.getText(inputStart, document.length - inputStart)

    private inline fun edit(block: () -> Unit) {
        editing = true
        try {
            block()
        } finally {
            editing = false
        }
    }

    private fun replaceInput(text: String) {
        edit { document.replace(inputStart, document.length - inputStart, text, bold) }
        textPane.caretPosition = document.length
    }

    private fun writePrompt() {
        statusStart = document.length
        promptStart = document.length
        edit { document.insertString(promptStart, shell.prompt, plain) }
        inputStart = document.length
        textPane.caretPosition = inputStart
    }

    /**
     * Add a line to the transcript, above the prompt. Parts of [text] marked with [org.simbrain.console.bold] are bold,
     * and so is [boldText], which follows it, as for a command after its prompt.
     */
    internal fun insertOutput(text: String, boldText: String = "") {
        val segments = styledSegments(text) + (boldText to true) + ("\n" to false)
        keepingCaretInInput {
            var offset = statusStart
            edit {
                for ((segment, isBold) in segments.filter { it.first.isNotEmpty() }) {
                    document.insertString(offset, segment, if (isBold) bold else plain)
                    offset += segment.length
                }
            }
            shiftFromStatus(offset - statusStart)
            trimTranscript()
        }
        scrollToInput()
    }

    private fun shiftFromStatus(added: Int) {
        statusStart += added
        promptStart += added
        inputStart += added
    }

    /** Show [text] on the line above the prompt, or remove that line if null. */
    private fun setStatus(text: String?) {
        val line = text?.let { it + "\n" } ?: ""
        val oldLength = promptStart - statusStart
        if (document.getText(statusStart, oldLength) == line) return
        keepingCaretInInput {
            edit { document.replace(statusStart, oldLength, line, plain) }
            promptStart += line.length - oldLength
            inputStart += line.length - oldLength
        }
        scrollToInput()
    }

    private fun refreshPrompt() {
        val prompt = shell.prompt
        if (document.getText(promptStart, inputStart - promptStart) == prompt) return
        keepingCaretInInput {
            edit { document.replace(promptStart, inputStart - promptStart, prompt, plain) }
            inputStart = promptStart + prompt.length
        }
    }

    /** Runs a change to the text above the input, then puts the caret back if it was in the input. */
    private inline fun keepingCaretInInput(change: () -> Unit) {
        val caretInInput = textPane.caretPosition - inputStart
        change()
        if (caretInInput >= 0) textPane.caretPosition = inputStart + caretInInput
    }

    private fun trimTranscript() {
        val root = document.defaultRootElement
        val excess = root.elementCount - MAX_TRANSCRIPT_LINES
        if (excess <= 0) return
        val end = root.getElement(excess).startOffset
        if (end > statusStart) return
        edit { document.remove(0, end) }
        shiftFromStatus(-end)
    }

    private fun clearTranscript() {
        keepingCaretInInput {
            val removed = statusStart
            edit { document.remove(0, removed) }
            shiftFromStatus(-removed)
        }
    }

    private fun scrollToInput() {
        SwingUtilities.invokeLater {
            textPane.modelToView2D(document.length)?.bounds?.let(textPane::scrollRectToVisible)
        }
    }

    private class QueuedLine(val line: String) {
        val done: CompletableJob = Job()
    }

    /** Black on white or white on black, following the theme. */
    internal inner class ConsoleTextPane : JTextPane() {

        override fun updateUI() {
            super.updateUI()
            val dark = FlatLaf.isLafDark()
            background = if (dark) Color.BLACK else Color.WHITE
            foreground = if (dark) Color.WHITE else Color.BLACK
            caretColor = foreground
        }

        override fun paste() {
            val text = runCatching { toolkit.systemClipboard.getData(DataFlavor.stringFlavor) as String }.getOrNull()
            if (text != null) pasteText(text)
        }
    }

    /** Protects what is above the input. Typing with the caret up there goes to the end of the input instead. */
    private inner class InputFilter : DocumentFilter() {

        override fun insertString(fb: FilterBypass, offset: Int, text: String?, attrs: AttributeSet?) =
            replace(fb, offset, 0, text, attrs)

        override fun remove(fb: FilterBypass, offset: Int, length: Int) {
            if (editing || offset >= inputStart) super.remove(fb, offset, length)
        }

        override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String?, attrs: AttributeSet?) {
            when {
                editing -> super.replace(fb, offset, length, text, attrs)
                offset >= inputStart -> super.replace(fb, offset, length, text, bold)
                else -> {
                    super.insertString(fb, document.length, text.orEmpty(), bold)
                    textPane.caretPosition = document.length
                }
            }
        }
    }
}
