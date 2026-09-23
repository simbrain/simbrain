/** Verifies annotation/delegate metadata, multiline preservation, and opt-in bulk replacement. */
package org.simbrain.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.propertyeditor.*
import javax.swing.SwingUtilities

class MultilineStringWidgetTest {
    class Note : EditableObject {
        @UserParameter(label = "Annotated", multiline = true, textAreaRows = 7, textAreaColumns = 40)
        var annotated = "first\nsecond"
        var delegated by GuiEditable<Note, String>(
            initValue = "alpha\nbeta", multiline = true, textAreaRows = 3, textAreaColumns = 24
        )
        var nullable by GuiEditable<Note, String?>(initValue = null, multiline = true)
        @UserParameter(label = "Single line")
        var single = "ordinary"
    }

    @Test
    fun `annotations and delegates select multiline widgets with configured dimensions`() = SwingUtilities.invokeAndWait {
        val note = Note()
        val editor = AnnotatedPropertyEditor(note)
        val annotated = editor.propertyNameWidgetMap["annotated"] as MultilineStringWidget
        val delegated = editor.propertyNameWidgetMap["delegated"] as MultilineStringWidget
        assertEquals(7, annotated.textArea.rows)
        assertEquals(40, annotated.textArea.columns)
        assertEquals(3, delegated.textArea.rows)
        assertEquals(24, delegated.textArea.columns)
        assertTrue(editor.propertyNameWidgetMap["single"] is StringWidget)
        annotated.textArea.text = "new\ncontent\n"
        delegated.textArea.text = ""
        editor.commitChanges()
        assertEquals("new\ncontent\n", note.annotated)
        assertEquals("", note.delegated)
        assertNull(note.nullable)
    }

    @Test
    fun `bulk replacement can be opted out again and explicitly cleared`() = SwingUtilities.invokeAndWait {
        val first = Note()
        val second = Note().apply { annotated = "different" }
        val editor = AnnotatedPropertyEditor(first, second)
        val widget = editor.propertyNameWidgetMap["annotated"] as MultilineStringWidget
        widget.replaceText.doClick()
        widget.textArea.text = "replacement"
        widget.replaceText.doClick()
        editor.commitChanges()
        assertEquals("first\nsecond", first.annotated)
        assertEquals("different", second.annotated)
        widget.replaceText.doClick()
        widget.textArea.text = ""
        editor.commitChanges()
        assertEquals("", first.annotated)
        assertEquals("", second.annotated)
    }
}
