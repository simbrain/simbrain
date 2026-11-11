package org.simbrain.console

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.workspace.Workspace
import javax.swing.SwingUtilities
import javax.swing.text.StyleConstants

class CommandConsolePanelTest {

    private fun panel(): CommandConsolePanel {
        val workspace = Workspace().apply { addWorkspaceComponent(NetworkComponent("net1")) }
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
        withTimeout(30_000) {
            while (expected !in text) delay(5)
        }
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
        panel.awaitText("> partial")
        val text = panel.text
        assertTrue(text.indexOf("Added Neuron_1") < text.indexOf("> ls net1"), text)
        assertTrue(text.endsWith("> partial"), text)
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
