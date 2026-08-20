/**
 * Tests for [TextWorldPanel]'s document-listener behavior. User edits must not force a text
 * layout while the document mutation is still being fired: the pane's views have not absorbed
 * the edit yet, so an in-listener modelToView can throw from deep inside Swing layout, which
 * aborts the notification chain and leaves the view tree permanently out of sync.
 */
package org.simbrain.world.textworld.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.world.textworld.TextWorld
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

class TextWorldPanelTest {

    @Test
    fun `highlight refresh is deferred until after a document mutation completes`() {
        val panelRef = AtomicReference<TextWorldPanel>()
        SwingUtilities.invokeAndWait {
            val world = TextWorld()
            world.text = "seed text"
            val panel = TextWorldPanel(world)
            panel.textArea.setSize(160, 600)
            panel.textArea.modelToView(0)
            panelRef.set(panel)
        }
        SwingUtilities.invokeAndWait {
            val textArea = panelRef.get().textArea
            val pasted = "日本語のテキスト😀🦕🚀 Ωß Привет мир\n".repeat(40)
            textArea.document.insertString(textArea.document.length, pasted, null)
            assertEquals(
                0, textArea.highlighter.highlights.size,
                "highlights must not be recomputed while the insert is still being fired"
            )
        }
        SwingUtilities.invokeAndWait { }
        SwingUtilities.invokeAndWait {
            val textArea = panelRef.get().textArea
            assertTrue(
                textArea.highlighter.highlights.isNotEmpty(),
                "the deferred pass should still highlight the current token"
            )
            textArea.modelToView(textArea.document.length)
        }
    }
}
