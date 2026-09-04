package org.simbrain.world.odorworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.point
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.MovementMode
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.util.concurrent.atomic.AtomicInteger

class GridMovementTest {

    private fun gridWorld(cells: Int = 4) = OdorWorld().apply {
        tileMap = TileMap(cells * 2, cells * 2)
        gridCellSizeInTiles = 2
        gridStepDurationMs = 0
        wrapAround = false
        isObjectsBlockMovement = false
    }

    @Test
    fun `switching to grid mode snaps to the cell center`() {
        val world = gridWorld()
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.location = point(70, 5)
        mouse.movementMode = MovementMode.GRID
        assertEquals(point(96.0, 32.0), mouse.location)
        assertEquals(1 to 0, mouse.cell)
    }

    @Test
    fun `stepping moves one cell and faces the direction`() {
        val world = gridWorld()
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.location = world.cellCenter(1, 1)
        mouse.movementMode = MovementMode.GRID
        assertTrue(mouse.stepOneCell(GridDirection.NORTH))
        assertEquals(1 to 0, mouse.cell)
        assertEquals(90.0, mouse.heading)
        assertTrue(mouse.stepOneCell(GridDirection.EAST))
        assertEquals(2 to 0, mouse.cell)
        assertEquals(world.cellCenter(2, 0), mouse.location)
    }

    @Test
    fun `stepping off a non wrapping map is blocked`() {
        val world = gridWorld()
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.location = world.cellCenter(0, 0)
        mouse.movementMode = MovementMode.GRID
        assertFalse(mouse.stepOneCell(GridDirection.WEST))
        assertEquals(0 to 0, mouse.cell)
    }

    @Test
    fun `stepping off a wrapping map without a maze wraps`() {
        val world = gridWorld().apply { wrapAround = true }
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.location = world.cellCenter(0, 0)
        mouse.movementMode = MovementMode.GRID
        assertTrue(mouse.stepOneCell(GridDirection.WEST))
        assertEquals(3 to 0, mouse.cell)
    }

    @Test
    fun `maze walls block steps and fire a collision`() {
        val world = gridWorld()
        world.maze = Maze(4, 4).apply { setWall(0, 0, GridDirection.EAST, false) }
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.location = world.cellCenter(0, 0)
        mouse.movementMode = MovementMode.GRID
        val collisions = AtomicInteger()
        mouse.events.collided.on { collisions.incrementAndGet() }
        assertFalse(mouse.stepOneCell(GridDirection.SOUTH))
        assertEquals(0 to 0, mouse.cell)
        assertTrue(mouse.stepOneCell(GridDirection.EAST))
        assertEquals(1 to 0, mouse.cell)
        assertFalse(mouse.stepOneCell(GridDirection.EAST))
        // collided handlers run asynchronously on the event dispatcher
        val deadline = System.currentTimeMillis() + 2000
        while (collisions.get() < 2 && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertEquals(2, collisions.get())
    }

    @Test
    fun `blocking tiles in the target cell block a step`() = runBlocking {
        val world = gridWorld()
        world.tileMap.layers[0].blocking = true
        world.tileMap.setTile(2, 0, 25, world.tileMap.layers[0])
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.location = world.cellCenter(0, 0)
        mouse.movementMode = MovementMode.GRID
        assertFalse(mouse.stepOneCell(GridDirection.EAST))
        assertTrue(mouse.stepOneCell(GridDirection.SOUTH))
    }

    @Test
    fun `animated step arrives at the target cell`() = runBlocking {
        val world = gridWorld().apply { gridStepDurationMs = 48 }
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.location = world.cellCenter(1, 1)
        mouse.movementMode = MovementMode.GRID
        assertTrue(mouse.moveOneCell(GridDirection.SOUTH))
        assertEquals(world.cellCenter(1, 2), mouse.location)
        assertFalse(mouse.isInTransit)
    }

    @Test
    fun `grid turn is always a quarter turn`() {
        val world = gridWorld()
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.movementMode = MovementMode.GRID
        mouse.heading = 0.0
        mouse.turn(1.0)
        assertEquals(90.0, mouse.heading)
        mouse.turn(-30.0)
        assertEquals(0.0, mouse.heading)
        mouse.turn(0.0)
        assertEquals(0.0, mouse.heading)
    }

    @Test
    fun `speed drives one cell per update in grid mode`() = runBlocking {
        val world = gridWorld()
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        world.addEntity(mouse)
        mouse.location = world.cellCenter(1, 1)
        mouse.heading = 0.0
        mouse.movementMode = MovementMode.GRID
        mouse.movement.speed = 5.0
        world.update()
        assertEquals(2 to 1, mouse.cell)
        mouse.movement.speed = -1.0
        world.update()
        assertEquals(1 to 1, mouse.cell)
        assertEquals(0.0, mouse.heading)
    }

    @Test
    fun `continuous movement is stopped by a maze wall`() {
        val world = gridWorld()
        world.maze = Maze(4, 4)
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.location = world.cellCenter(0, 0)
        mouse.heading = 0.0
        mouse.movement.speed = 10.0
        repeat(10) { mouse.applyMovement() }
        assertTrue(mouse.x + mouse.width / 2 <= 64.0)
        assertEquals(0 to 0, mouse.cell)
    }

    @Test
    fun `generate maze resizes the map to fit the cells`() {
        val world = OdorWorld()
        world.generateMaze(5, 3, cellSizeInTiles = 2, seed = 1L)
        assertEquals(10, world.tileMap.width)
        assertEquals(6, world.tileMap.height)
        assertEquals(5, world.gridColumns)
        assertEquals(3, world.gridRows)
        assertEquals(15, world.maze!!.reachableCellCount())
        assertTrue(world.collidableObjects.size >= world.maze!!.wallCount)
    }
}
