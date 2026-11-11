package org.simbrain.console

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.workspace.Workspace
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import javax.swing.SwingUtilities
import javax.swing.TransferHandler
import javax.swing.text.StyleConstants

class CommandConsolePanelTest {

    private fun panel(workspace: Workspace = Workspace().apply { addWorkspaceComponent(NetworkComponent("net1")) }): CommandConsolePanel {
        lateinit var panel: CommandConsolePanel
        SwingUtilities.invokeAndWait { panel = CommandConsolePanel(workspace) }
        return panel
    }

    private fun <T> onEdt(block: () -> T): T {
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(block) }
        return result!!.getOrThrow()
    }

    private val CommandConsolePanel.text: String
        get() = onEdt { textPane.document.getText(0, textPane.document.length) }

    private fun CommandConsolePanel.type(text: String) = onEdt { textPane.replaceSelection(text) }

    private fun CommandConsolePanel.press(action: String) =
        onEdt { textPane.actionMap.get(action).actionPerformed(null) }

    private fun CommandConsolePanel.awaitText(expected: String) = runBlocking {
        withTimeoutOrNull(30_000) {
            while (expected !in text) delay(5)
        } ?: fail("Timed out waiting for \"$expected\" in:\n$text")
    }

    private fun CommandConsolePanel.enter(line: String, expected: String) {
        type(line)
        press("console-enter")
        awaitText(expected)
    }

    @Test
    fun `a typed command shows its output above a new prompt`() {
        val panel = panel()
        panel.enter("ls", "net1\n")
        assertTrue(panel.text.endsWith("> ls\nnet1\n> "), panel.text)
    }

    @Test
    fun `the prompt follows cd`() {
        val panel = panel()
        panel.type("cd net1")
        panel.press("console-enter")
        panel.awaitText("net1> ")
        assertTrue(panel.text.endsWith("net1> "), panel.text)
    }

    @Test
    fun `text above the input can't be deleted`() {
        val panel = panel()
        val before = panel.text
        onEdt { panel.textPane.document.remove(0, 5) }
        assertEquals(before, panel.text)
    }

    @Test
    fun `backspace stops at the prompt`() {
        val panel = panel()
        val before = panel.text
        onEdt { panel.textPane.document.remove(before.length - 1, 1) }
        assertEquals(before, panel.text)
    }

    @Test
    fun `the input can be edited`() {
        val panel = panel()
        panel.type("lss")
        onEdt { panel.textPane.document.remove(panel.textPane.document.length - 1, 1) }
        assertTrue(panel.text.endsWith("> ls"), panel.text)
    }

    @Test
    fun `typing with the caret in the transcript goes to the input`() {
        val panel = panel()
        val before = panel.text
        onEdt { panel.textPane.caretPosition = 2 }
        panel.type("ls")
        assertEquals(before + "ls", panel.text)
    }

    @Test
    fun `tab completes the input and leaves the caret at its end`() {
        val panel = panel()
        panel.type("sh")
        panel.press("console-complete")
        assertTrue(panel.text.endsWith("> show "), panel.text)
        assertEquals(0, onEdt { panel.textPane.document.length - panel.textPane.caretPosition })
    }

    @Test
    fun `output arriving while typing goes above the input and keeps the caret in place`() {
        val panel = panel()
        panel.type("partial")
        onEdt { panel.textPane.caretPosition = panel.textPane.document.length - 3 }
        val fromEnd = onEdt { panel.textPane.document.length - panel.textPane.caretPosition }
        onEdt { panel.insertOutput("late output") }
        assertTrue(panel.text.endsWith("late output\n> partial"), panel.text)
        assertEquals(fromEnd, onEdt { panel.textPane.document.length - panel.textPane.caretPosition })
    }

    @Test
    fun `tab with several matches lists them above the input`() {
        val panel = panel()
        panel.type("s")
        panel.press("console-complete")
        val text = panel.text
        assertTrue("show" in text && "stop" in text && "save" in text, text)
        assertTrue(text.endsWith("> s"), text)
    }

    @Test
    fun `up and down move through history`() {
        val panel = panel()
        panel.enter("ls", "net1\n")
        panel.enter("help ls", "List contents")
        panel.press("console-previous")
        assertTrue(panel.text.endsWith("> help ls"), panel.text)
        panel.press("console-previous")
        assertTrue(panel.text.endsWith("> ls"), panel.text)
        panel.press("console-next")
        panel.press("console-next")
        assertTrue(panel.text.endsWith("> "), panel.text)
    }

    @Test
    fun `home goes to the start of the input`() {
        val panel = panel()
        val inputStart = panel.text.length
        panel.type("ls")
        panel.press("console-home")
        assertEquals(inputStart, onEdt { panel.textPane.caretPosition })
    }

    @Test
    fun `commands are bold and output is not`() {
        val panel = panel()
        panel.enter("ls", "net1\n")
        val text = panel.text
        fun isBold(offset: Int) = onEdt {
            StyleConstants.isBold(panel.textPane.styledDocument.getCharacterElement(offset).attributes)
        }
        assertTrue(isBold(text.indexOf("> ls") + 2))
        assertFalse(isBold(text.indexOf("> ls")))
        assertFalse(isBold(text.indexOf("net1\n")))
    }

    @Test
    fun `help is bold in the intro`() {
        val panel = panel()
        val text = panel.text
        fun isBold(offset: Int) = onEdt {
            StyleConstants.isBold(panel.textPane.styledDocument.getCharacterElement(offset).attributes)
        }
        assertTrue(isBold(text.indexOf("help")))
        assertFalse(isBold(text.indexOf("Enter")))
    }

    @Test
    fun `pasting one line adds it to the input`() {
        val panel = panel()
        onEdt { panel.pasteText("ls net1") }
        assertTrue(panel.text.endsWith("> ls net1"), panel.text)
    }

    @Test
    fun `pasting several lines runs them in order and keeps the last fragment`() {
        val panel = panel()
        onEdt { panel.pasteText("add neurons 1\nls net1\npartial") }
        panel.awaitText("> ls net1\nNeuron_1")
        val text = panel.text
        assertTrue(text.indexOf("Added Neuron_1") < text.indexOf("> ls net1"), text)
        assertTrue(text.endsWith("> partial"), text)
    }

    @Test
    fun `pasted commands leave what is being typed alone`() {
        val panel = panel()
        onEdt { panel.pasteText("run 20\nls net1\n") }
        panel.type("abc")
        panel.awaitText("> ls net1\n(empty)")
        val text = panel.text
        assertTrue(text.endsWith("> abc"), text)
        assertTrue(text.indexOf("Ran 20") < text.indexOf("> ls net1"), text)
    }

    @Test
    fun `stop runs while another command is running`() {
        val workspace = Workspace().apply { addWorkspaceComponent(NetworkComponent("net1")) }
        val panel = panel(workspace)
        panel.submit("run 100000000")
        runBlocking { withTimeout(30_000) { while (workspace.time < 2) delay(5) } }
        panel.submit("stop")
        panel.awaitText("Ran ")
        assertTrue(panel.text.indexOf("> stop") < panel.text.indexOf("Ran "), panel.text)
    }

    @Test
    fun `a long command shows progress and escape cancels it`() {
        val workspace = Workspace().apply { addWorkspaceComponent(NetworkComponent("net1")) }
        val panel = panel(workspace)
        panel.type("abc")
        panel.submit("run 100000000")
        panel.submit("ls")
        panel.awaitText("Esc to cancel.")
        assertTrue(panel.text.contains("Running"), panel.text)
        panel.press("console-cancel")
        panel.awaitText("Cancelled.")
        val text = panel.text
        assertTrue("Dropped 1 waiting command(s)." in text, text)
        assertFalse("Esc to cancel." in text, text)
        assertTrue(text.endsWith("Cancelled.\n> abc"), text)
        runBlocking { withTimeout(30_000) { while (workspace.updater.isRunning) delay(5) } }
    }

    @Test
    fun `the status line shows a bar when the total is known`() {
        assertEquals(
            "[#####...............] 25% Setting 250 of 1,000. Ctrl+C or Esc to cancel.",
            statusText(Progress("Setting", 250, 1000))
        )
        assertEquals("Searching: 52,000 objects. Ctrl+C or Esc to cancel.", statusText(Progress("Searching", 52_000)))
        assertEquals("Working. Ctrl+C or Esc to cancel.", statusText(null))
    }

    @Test
    fun `examples copied from help can be pasted back to run them`() {
        val panel = panel()
        panel.enter("help ls", "Examples")
        val clipboard = Clipboard("test")
        onEdt {
            val text = panel.textPane.document.getText(0, panel.textPane.document.length)
            val start = text.indexOf("# List the components")
            val end = text.indexOf("# List the neurons in net1\n")
            panel.textPane.select(start, end)
            panel.textPane.transferHandler.exportToClipboard(panel.textPane, clipboard, TransferHandler.COPY)
        }
        val copied = clipboard.getData(DataFlavor.stringFlavor) as String
        assertEquals("# List the components in the workspace\nls\n\n# List what is in net1\nls net1\n\n", copied)
        onEdt { panel.pasteText(copied) }
        panel.awaitText("> ls net1\n(empty)")
        assertTrue(panel.text.contains("> ls\nnet1\n"), panel.text)
    }

    @Test
    fun `ctrl c cancels a running command, and otherwise clears the input`() {
        val workspace = Workspace().apply { addWorkspaceComponent(NetworkComponent("net1")) }
        val panel = panel(workspace)
        panel.submit("run 100000000")
        panel.awaitText("to cancel.")
        panel.press("console-interrupt")
        panel.awaitText("Cancelled.")
        panel.type("partial")
        panel.press("console-interrupt")
        assertTrue(panel.text.endsWith("> "), panel.text)
        runBlocking { withTimeout(30_000) { while (workspace.updater.isRunning) delay(5) } }
    }

    @Test
    fun `command names in help are bold`() {
        val panel = panel()
        panel.enter("help", "Inspect")
        val offset = panel.text.indexOf("\n  ls") + 3
        assertTrue(onEdt { StyleConstants.isBold(panel.textPane.styledDocument.getCharacterElement(offset).attributes) })
        assertFalse(panel.text.contains('\u0001'))
    }

    @Test
    fun `cls clears the transcript`() {
        val panel = panel()
        panel.enter("ls", "net1\n")
        panel.type("cls")
        panel.press("console-enter")
        runBlocking {
            withTimeout(30_000) {
                while (panel.text != "> ") delay(5)
            }
        }
    }
}
