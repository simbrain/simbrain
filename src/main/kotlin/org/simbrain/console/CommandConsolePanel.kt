/**
 * A terminal for a [CommandShell]: one scrolling text pane holding the transcript, then the prompt, then what is being
 * typed. Everything before the input is read-only, and output from a command that finishes later is inserted above the
 * prompt, so it never lands in the middle of what is being typed. Commands run off the event thread.
 */
package org.simbrain.console

import com.formdev.flatlaf.FlatLaf
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
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

private const val MAX_TRANSCRIPT_LINES = 5000

class CommandConsolePanel(workspace: Workspace, desktop: SimbrainDesktop? = null) : JPanel(BorderLayout()) {

    private val shell = CommandShell(workspace, desktop).apply {
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

    private var promptStart = 0

    private var inputStart = 0

    /** True while the panel itself is changing the document, which the read-only protection does not apply to. */
    private var editing = false

    private val history = mutableListOf<String>()

    private var historyIndex = 0

    init {
        add(JScrollPane(textPane).apply { border = BorderFactory.createEmptyBorder() }, BorderLayout.CENTER)
        document.documentFilter = InputFilter()
        writePrompt()
        val intro = "Simbrain commands. Enter help for a list."
        insertOutput(intro)
        textPane.styledDocument.setCharacterAttributes(intro.indexOf("help"), "help".length, bold, false)
        commandScope.launch { runQueue() }

        bindKey("ENTER", "console-enter") { enterLine() }
        bindKey("UP", "console-previous") { moveThroughHistory(-1) }
        bindKey("DOWN", "console-next") { moveThroughHistory(1) }
        bindKey("TAB", "console-complete") { completeInput() }
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

    private suspend fun runQueue() {
        for (queued in queue) runAndShow(queued)
    }

    /** Shows the line after the prompt, runs it, and shows its output. */
    private suspend fun runAndShow(queued: QueuedLine) {
        try {
            withContext(swingDispatcher) { insertOutput(shell.prompt, queued.line) }
            val result = shell.execute(queued.line)
            withContext(swingDispatcher) {
                if (result.isNotEmpty()) insertOutput(result)
                refreshPrompt()
            }
        } finally {
            queued.done.complete()
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
        promptStart = document.length
        edit { document.insertString(promptStart, shell.prompt, plain) }
        inputStart = document.length
        textPane.caretPosition = inputStart
    }

    /** Add a line to the transcript, above the prompt. Text after [plainText] is bold, as for a command. */
    internal fun insertOutput(plainText: String, boldText: String = "") {
        keepingCaretInInput {
            edit {
                document.insertString(promptStart, plainText, plain)
                document.insertString(promptStart + plainText.length, boldText + "\n", if (boldText.isEmpty()) plain else bold)
            }
            val added = plainText.length + boldText.length + 1
            promptStart += added
            inputStart += added
            trimTranscript()
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
        if (end > promptStart) return
        edit { document.remove(0, end) }
        promptStart -= end
        inputStart -= end
    }

    private fun clearTranscript() {
        keepingCaretInInput {
            edit { document.remove(0, promptStart) }
            inputStart -= promptStart
            promptStart = 0
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
