/**
 * How long the wand palette's popup panel follows its palette: while it is on screen, and no longer.
 */
package org.simbrain.network.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.util.awaitUntil
import org.simbrain.util.onEdt
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JDialog

class WandPalettePanelTest {

    private fun palette() = WandPalette().apply {
        addAction(AdjustValueAction.activate())
        addAction(AdjustValueAction.inhibit())
        addAction(AdjustValueAction.setValue(0.0))
    }

    @Test
    fun `a panel on screen is told when the selected action changes`() {
        val palette = palette()
        val selectionChanges = AtomicInteger()
        onEdt { JDialog().apply { contentPane = WandPalettePanel(palette) { selectionChanges.incrementAndGet() }; pack() } }

        palette.selectAction(1)

        awaitUntil { selectionChanges.get() == 1 }
    }

    @Test
    fun `a panel taken off screen is no longer told when the selected action changes`() {
        val palette = palette()
        val closedPanelChanges = AtomicInteger()
        val openPanelChanges = AtomicInteger()
        onEdt {
            val closed = JDialog().apply {
                contentPane = WandPalettePanel(palette) { closedPanelChanges.incrementAndGet() }
                pack()
            }
            closed.dispose()
            JDialog().apply { contentPane = WandPalettePanel(palette) { openPanelChanges.incrementAndGet() }; pack() }
        }

        palette.selectAction(1)

        // The panel still on screen hearing of the change shows that the closed one had its chance to
        awaitUntil { openPanelChanges.get() == 1 }
        assertEquals(0, closedPanelChanges.get())
    }
}
