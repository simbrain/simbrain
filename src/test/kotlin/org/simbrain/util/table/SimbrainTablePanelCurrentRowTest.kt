/**
 * A table panel highlights its data frame's current row through a per-frame refresh while it is on screen: the
 * highlight follows the row, a data world update never waits on the EDT, and a panel taken off screen stops
 * listening. Row names likewise follow the data frame while the panel is shown.
 */
package org.simbrain.util.table

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.UiWork
import org.simbrain.util.finishesWhileEdtIsBlocked
import org.simbrain.util.onEdt
import org.simbrain.util.subscriberTotal
import org.simbrain.world.dataworld.DataWorld
import javax.swing.JFrame
import javax.swing.SwingUtilities

class SimbrainTablePanelCurrentRowTest {

    private fun showPanel(world: DataWorld): Pair<JFrame, SimbrainTablePanel> {
        lateinit var frame: JFrame
        lateinit var panel: SimbrainTablePanel
        SwingUtilities.invokeAndWait {
            panel = SimbrainTablePanel(world.dataModel)
            frame = JFrame().apply {
                contentPane = panel
                pack()
            }
        }
        return frame to panel
    }

    @Test
    fun `a shown panel highlights the data world's current row`() = runBlocking {
        val world = DataWorld(rows = 10, cols = 2)
        val (frame, panel) = showPanel(world)

        repeat(3) { world.update() }
        UiWork.awaitIdle()

        try {
            assertEquals(world.dataModel.currentRowIndex, onEdt { panel.table.selectedRow })
        } finally {
            SwingUtilities.invokeAndWait { frame.dispose() }
        }
    }

    @Test
    fun `a data world update does not wait for the edt`() = runBlocking {
        val world = DataWorld(rows = 10, cols = 2)
        val (frame, _) = showPanel(world)

        try {
            assertTrue(finishesWhileEdtIsBlocked { repeat(20) { world.update() } })
        } finally {
            SwingUtilities.invokeAndWait { frame.dispose() }
        }
    }

    @Test
    fun `a panel taken off screen stops following the current row`() = runBlocking {
        val world = DataWorld(rows = 10, cols = 2)
        val (frame, panel) = showPanel(world)
        world.update()
        UiWork.awaitIdle()
        val shown = onEdt { panel.table.selectedRow }

        SwingUtilities.invokeAndWait { frame.dispose() }
        repeat(3) { world.update() }
        UiWork.awaitIdle()

        assertEquals(shown, onEdt { panel.table.selectedRow })
    }

    @Test
    fun `a panel taken off screen leaves no subscriptions on its data frame`() = runBlocking {
        val world = DataWorld(rows = 10, cols = 2)
        val withoutPanel = world.dataModel.events.subscriberTotal()
        val (frame, _) = showPanel(world)
        assertTrue(world.dataModel.events.subscriberTotal() > withoutPanel)

        SwingUtilities.invokeAndWait { frame.dispose() }

        assertEquals(withoutPanel, world.dataModel.events.subscriberTotal())
    }

    @Test
    fun `row names renamed while the panel is off screen show once it is back`() = runBlocking {
        val world = DataWorld(rows = 3, cols = 2)
        val (frame, panel) = showPanel(world)
        SwingUtilities.invokeAndWait { frame.dispose() }

        world.dataModel.rowNames = listOf("a", "b", "c")
        SwingUtilities.invokeAndWait { frame.isVisible = true }

        try {
            assertEquals(listOf("a", "b", "c"), onEdt { panel.scrollPane.rowNames })
        } finally {
            SwingUtilities.invokeAndWait { frame.dispose() }
        }
    }
}
