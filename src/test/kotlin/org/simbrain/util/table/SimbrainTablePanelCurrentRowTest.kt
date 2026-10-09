/**
 * A table panel highlights its data frame's current row through a per-frame refresh while it is on screen: the
 * highlight follows the row, a data world update never waits on the EDT, and a panel taken off screen stops
 * listening.
 */
package org.simbrain.util.table

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.UiWork
import org.simbrain.util.finishesWhileEdtIsBlocked
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

        assertEquals(world.dataModel.currentRowIndex, panel.table.selectedRow)
        SwingUtilities.invokeAndWait { frame.dispose() }
    }

    @Test
    fun `a data world update does not wait for the edt`() = runBlocking {
        val world = DataWorld(rows = 10, cols = 2)
        val (frame, _) = showPanel(world)

        assertTrue(finishesWhileEdtIsBlocked { repeat(20) { world.update() } })
        SwingUtilities.invokeAndWait { frame.dispose() }
    }

    @Test
    fun `a panel taken off screen stops following the current row`() = runBlocking {
        val world = DataWorld(rows = 10, cols = 2)
        val (frame, panel) = showPanel(world)
        world.update()
        UiWork.awaitIdle()
        val shown = panel.table.selectedRow

        SwingUtilities.invokeAndWait { frame.dispose() }
        repeat(3) { world.update() }
        UiWork.awaitIdle()

        assertEquals(shown, panel.table.selectedRow)
    }
}
