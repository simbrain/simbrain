/** Checks combined content/font edits and preservation of each bulk target's untouched values. */
package org.simbrain.network.gui

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.gui.dialogs.text.TextEditorPanel
import org.simbrain.network.gui.dialogs.text.TextDialog
import org.simbrain.util.propertyeditor.FontWidget
import java.awt.Font
import javax.swing.SwingUtilities

class TextEditorTest {
    @Test
    fun `creation dialog commits text and font before callback and cancel creates nothing`() = SwingUtilities.invokeAndWait {
        val draft = NetworkTextObject()
        val created = mutableListOf<NetworkTextObject>()
        val dialog = TextDialog(listOf(draft), titleName = "Add Text") { created.addAll(it) }
        try {
            dialog.editor.textArea.text = "created\ntext"
            val font = dialog.editor.propertyEditor.propertyNameWidgetMap["font"] as FontWidget
            val chooser = font.createEditor()
            chooser.sizeField.text = "24"
            font.acceptEdits(chooser)
            assertTrue(created.isEmpty())
            dialog.okButton.doClick()
            assertEquals("created\ntext", created.single().text)
            assertEquals(24, created.single().fontSize)
        } finally { dialog.dispose() }
        val cancelledDraft = NetworkTextObject()
        val cancelled = TextDialog(listOf(cancelledDraft), titleName = "Add Text") { created.addAll(it) }
        try {
            cancelled.editor.textArea.text = "cancelled"
            cancelled.cancelButton.doClick()
            assertEquals(1, created.size)
            assertEquals("", cancelledDraft.text)
        } finally { cancelled.dispose() }
    }

    @Test
    fun `typing the first object's size still replaces a mixed size`() = SwingUtilities.invokeAndWait {
        val first = NetworkTextObject("first").apply { fontSize = 10 }
        val second = NetworkTextObject("second").apply { fontSize = 20 }
        val panel = TextEditorPanel(listOf(first, second))
        val font = panel.propertyEditor.propertyNameWidgetMap["font"] as FontWidget
        val chooser = font.createEditor()
        chooser.sizeField.text = "10"
        font.acceptEdits(chooser)
        panel.commitChanges()
        assertEquals(10, first.fontSize)
        assertEquals(10, second.fontSize)
    }

    @Test
    fun `single text editor commits multiline text and font together`() = SwingUtilities.invokeAndWait {
        val model = NetworkTextObject("old")
        val panel = TextEditorPanel(listOf(model))
        val font = panel.propertyEditor.propertyNameWidgetMap["font"] as FontWidget
        panel.textArea.text = "first line\nsecond line"
        val chooser = font.createEditor()
        chooser.familyList.setSelectedValue(Font.MONOSPACED, true)
        chooser.sizeList.setSelectedValue("24", true)
        chooser.styleList.selectedIndex = Font.BOLD
        font.acceptEdits(chooser)
        assertEquals("old", model.text)
        assertEquals(12, model.fontSize)
        panel.commitChanges()
        assertEquals("first line\nsecond line", model.text)
        assertEquals(Font.MONOSPACED, model.fontName)
        assertEquals(24, model.fontSize)
        assertTrue(model.isBold)
    }

    @Test
    fun `bulk size edit preserves distinct text family and styles`() = SwingUtilities.invokeAndWait {
        val first = NetworkTextObject("first").apply { fontName = Font.SERIF; isBold = true; fontSize = 10 }
        val second = NetworkTextObject("second").apply { fontName = Font.MONOSPACED; isItalic = true; fontSize = 20 }
        val panel = TextEditorPanel(listOf(first, second))
        assertFalse(panel.replaceText.isSelected)
        val font = panel.propertyEditor.propertyNameWidgetMap["font"] as FontWidget
        val chooser = font.createEditor()
        chooser.sizeField.text = "30"
        font.acceptEdits(chooser)
        panel.commitChanges()
        assertEquals(listOf("first", "second"), listOf(first.text, second.text))
        assertEquals(listOf(Font.SERIF, Font.MONOSPACED), listOf(first.fontName, second.fontName))
        assertEquals(listOf(30, 30), listOf(first.fontSize, second.fontSize))
        assertTrue(first.isBold)
        assertFalse(first.isItalic)
        assertFalse(second.isBold)
        assertTrue(second.isItalic)
    }

    @Test
    fun `bulk content replacement and clearing require explicit opt in`() = SwingUtilities.invokeAndWait {
        val first = NetworkTextObject("first")
        val second = NetworkTextObject("second")
        val panel = TextEditorPanel(listOf(first, second))
        panel.textArea.text = "replacement"
        panel.commitChanges()
        assertEquals("first", first.text)
        assertEquals("second", second.text)
        panel.replaceText.doClick()
        panel.commitChanges()
        assertEquals("replacement", first.text)
        assertEquals("replacement", second.text)
        panel.textArea.text = ""
        panel.commitChanges()
        assertEquals("", first.text)
        assertEquals("", second.text)
    }

    @Test
    fun `uncommitted edits and unchanged mixed fonts preserve model values`() = SwingUtilities.invokeAndWait {
        val first = NetworkTextObject("first").apply { fontSize = 10; isBold = true }
        val second = NetworkTextObject("second").apply { fontSize = 20; isItalic = true }
        val panel = TextEditorPanel(listOf(first, second))
        panel.commitChanges()
        assertEquals(10, first.fontSize)
        assertEquals(20, second.fontSize)
        val font = panel.propertyEditor.propertyNameWidgetMap["font"] as FontWidget
        val chooser = font.createEditor()
        chooser.sizeField.text = "40"
        panel.textArea.text = "uncommitted"
        assertEquals("first", first.text)
        assertEquals(10, first.fontSize)
        assertEquals(20, second.fontSize)
    }

    @Test
    fun `cancelled chooser preserves accepted draft and reopening retains it`() = SwingUtilities.invokeAndWait {
        val model = NetworkTextObject("label")
        val panel = TextEditorPanel(listOf(model))
        val font = panel.propertyEditor.propertyNameWidgetMap["font"] as FontWidget
        val accepted = font.createEditor()
        accepted.sizeField.text = "24"
        font.acceptEdits(accepted)
        val cancelled = font.createEditor()
        assertEquals("24", cancelled.sizeField.text)
        cancelled.sizeField.text = "48"
        assertEquals("24", font.createEditor().sizeField.text)
        assertEquals(12, model.fontSize)
        panel.commitChanges()
        assertEquals(24, model.fontSize)
    }

    @Test
    fun `font chooser validates sizes and previews combined bold italic style`() = SwingUtilities.invokeAndWait {
        val panel = TextEditorPanel(listOf(NetworkTextObject("label")))
        val font = panel.propertyEditor.propertyNameWidgetMap["font"] as FontWidget
        val chooser = font.createEditor()
        chooser.sizeField.text = "0"
        assertFalse(chooser.hasValidInput)
        chooser.sizeField.text = "bad"
        assertFalse(chooser.hasValidInput)
        chooser.sizeField.text = "18"
        chooser.styleList.selectedIndex = Font.BOLD or Font.ITALIC
        assertTrue(chooser.hasValidInput)
        assertEquals(18, chooser.preview.font.size)
        assertTrue(chooser.preview.font.isBold)
        assertTrue(chooser.preview.font.isItalic)
    }
}
