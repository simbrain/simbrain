package org.simbrain.world.odorworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.piccolo.TileMap
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.MovementMode
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import javax.swing.SwingUtilities

/**
 * Held-key driving of a grid-mode entity through the panel while the world is stopped, where the panel's
 * movement timer carries each step at the manual movement increment.
 */
class GridDrivingTest {

    private val component = OdorWorldComponent("Odor world")
    private val world = component.world.apply {
        tileMap = TileMap(12, 12)
        gridCellSizeInTiles = 2
        wrapAround = false
        isObjectsBlockMovement = false
        isUseCameraCentering = false
    }
    private val panel: OdorWorldPanel
    private val mouse: OdorWorldEntity

    init {
        lateinit var created: OdorWorldPanel
        SwingUtilities.invokeAndWait { created = OdorWorldPanel(component, world) }
        panel = created
        mouse = runBlocking {
            world.addEntity(96.0, 96.0, EntityType.Mouse).apply {
                heading = 0.0
                movementMode = MovementMode.GRID
                manualMovement.manualStraightMovementIncrement = 16.0
            }
        }
        await { panel.selectedEntityModels.contains(mouse) }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 3000
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(10)
        }
        assertTrue(condition())
    }

    @Test
    fun `a held direction keeps stepping in that absolute direction and faces it`() {
        assertEquals(1 to 1, mouse.cell)
        panel.pressGridDirection(GridDirection.SOUTH)
        await { mouse.cell.second >= 3 }
        panel.releaseGridDirection(GridDirection.SOUTH)
        assertEquals(1, mouse.cell.first)
        assertEquals(GridDirection.SOUTH, mouse.facingDirection)
        await { !mouse.isInTransit }
        val restingCell = mouse.cell
        assertEquals(world.cellCenter(restingCell.first, restingCell.second), mouse.location)
        Thread.sleep(200)
        assertEquals(restingCell, mouse.cell)
    }

    @Test
    fun `the most recently pressed direction wins while several keys are held`() {
        panel.pressGridDirection(GridDirection.EAST)
        panel.pressGridDirection(GridDirection.NORTH)
        await { mouse.cell.second == 0 }
        val column = mouse.cell.first
        assertTrue(column <= 2, "should have moved at most one cell east before north took over, got $column")
        panel.releaseGridDirection(GridDirection.NORTH)
        await { mouse.cell.first >= column + 1 }
        panel.releaseGridDirection(GridDirection.EAST)
    }

    @Test
    fun `holding a direction into a wall stops at the wall without leaving the cell`() {
        world.maze = Maze(6, 6)
        panel.pressGridDirection(GridDirection.EAST)
        Thread.sleep(300)
        panel.releaseGridDirection(GridDirection.EAST)
        assertEquals(1 to 1, mouse.cell)
        assertEquals(world.cellCenter(1, 1), mouse.location)
        assertEquals(GridDirection.EAST, mouse.facingDirection)
    }
}
