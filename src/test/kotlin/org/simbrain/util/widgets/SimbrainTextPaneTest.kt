package org.simbrain.util.widgets

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SimbrainTextPaneTest {

    @Test
    fun `text round-trips exactly including newlines and non-BMP characters`() {
        val pane = SimbrainTextPane()
        val text = "line one\nline two 😀 🇺🇸\n\nمرحبا"
        pane.text = text
        assertEquals(text, pane.text)
        assertEquals(text.length, pane.document.length)
    }

    @Test
    fun `undo reverts a typed insertion and redo restores it`() {
        val pane = SimbrainTextPane()
        pane.replaceTextWithoutUndo("hello")
        pane.document.insertString(5, " world", null)
        assertEquals("hello world", pane.text)
        pane.actionMap.get("simbrain-undo").actionPerformed(null)
        assertEquals("hello", pane.text)
        pane.actionMap.get("simbrain-redo").actionPerformed(null)
        assertEquals("hello world", pane.text)
    }

    @Test
    fun `programmatic replacement is not undoable`() {
        val pane = SimbrainTextPane()
        pane.document.insertString(0, "user typing", null)
        pane.replaceTextWithoutUndo("model output")
        pane.actionMap.get("simbrain-undo").actionPerformed(null)
        assertEquals("model output", pane.text)
    }

    @Test
    fun `edits made with undo suspended stay out of the history`() {
        val pane = SimbrainTextPane()
        pane.withUndoSuspended { pane.document.insertString(0, "styled sweep", null) }
        pane.actionMap.get("simbrain-undo").actionPerformed(null)
        assertEquals("styled sweep", pane.text)
    }
}
