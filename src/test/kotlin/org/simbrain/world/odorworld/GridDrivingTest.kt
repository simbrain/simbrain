/**
 * Manual key driving of a grid-mode entity through the odor world panel while the world is stopped.
 */
package org.simbrain.world.odorworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.plot.awaitUntil
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

    private class Fixture(val world: OdorWorld, val panel: OdorWorldPanel, val mouse: OdorWorldEntity)

    private fun drivingFixture(): Fixture {
        val component = OdorWorldComponent("Odor world")
        val world = component.world.apply {
            tileMap = TileMap(12, 12)
            gridCellSizeInTiles = 2
            wrapAround = false
            isObjectsBlockMovement = false
            isUseCameraCentering = false
        }
        lateinit var panel: OdorWorldPanel
        SwingUtilities.invokeAndWait { panel = OdorWorldPanel(component, world) }
        val mouse = runBlocking {
            world.addEntity(96.0, 96.0, EntityType.Mouse).apply {
                heading = 0.0
                movementMode = MovementMode.GRID
                manualMovement.manualStraightMovementIncrement = 16.0
            }
        }
        awaitUntil { panel.selectedEntityModels.contains(mouse) }
        return Fixture(world, panel, mouse)
    }

    @Test
    fun `a held direction keeps stepping in that absolute direction and faces it`() {
        val (world, panel, mouse) = drivingFixture().let { Triple(it.world, it.panel, it.mouse) }
        assertEquals(1 to 1, mouse.cell)
        panel.pressGridDirection(GridDirection.SOUTH)
        awaitUntil { mouse.cell.second >= 3 }
        panel.releaseGridDirection(GridDirection.SOUTH)
        assertEquals(1, mouse.cell.first)
        assertEquals(GridDirection.SOUTH, mouse.facingDirection)
        awaitUntil { !mouse.isInTransit }
        val restingCell = mouse.cell
        assertEquals(world.cellCenter(restingCell.first, restingCell.second), mouse.location)
        Thread.sleep(200)
        assertEquals(restingCell, mouse.cell)
    }

    @Test
    fun `the most recently pressed direction wins while several keys are held`() {
        val fixture = drivingFixture()
        val panel = fixture.panel
        val mouse = fixture.mouse
        panel.pressGridDirection(GridDirection.EAST)
        panel.pressGridDirection(GridDirection.NORTH)
        awaitUntil { mouse.cell.second == 0 }
        val column = mouse.cell.first
        assertTrue(column <= 2, "should have moved at most one cell east before north took over, got $column")
        panel.releaseGridDirection(GridDirection.NORTH)
        awaitUntil { mouse.cell.first >= column + 1 }
        panel.releaseGridDirection(GridDirection.EAST)
    }

    @Test
    fun `holding a direction into a wall stops at the wall without leaving the cell`() {
        val fixture = drivingFixture()
        fixture.world.maze = Maze(6, 6)
        fixture.panel.pressGridDirection(GridDirection.EAST)
        Thread.sleep(300)
        fixture.panel.releaseGridDirection(GridDirection.EAST)
        assertEquals(1 to 1, fixture.mouse.cell)
        assertEquals(fixture.world.cellCenter(1, 1), fixture.mouse.location)
        assertEquals(GridDirection.EAST, fixture.mouse.facingDirection)
    }
}
