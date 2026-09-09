package org.simbrain.world.odorworld

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
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
    fun `a requested step advances by grid speed each update and sensors see the way there`() = runBlocking {
        val world = gridWorld()
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        world.addEntity(mouse)
        world.addEntity(OdorWorldEntity(world, EntityType.Swiss).apply { location = world.cellCenter(1, 3) })
        mouse.location = world.cellCenter(1, 1)
        mouse.movementMode = MovementMode.GRID
        mouse.gridSpeed = 16.0
        val cheeseSensor = mouse.addObjectSensor(EntityType.Swiss, 0.0, 0.0, 400.0)
        world.update()
        val atRest = cheeseSensor.currentValue
        assertTrue(mouse.requestGridStep(GridDirection.SOUTH))
        assertTrue(mouse.isInTransit)
        world.update()
        assertEquals(point(96.0, 112.0), mouse.location)
        assertTrue(cheeseSensor.currentValue > atRest, "sensor should read the intermediate position")
        assertEquals(1 to 1, mouse.cell)
        world.update()
        world.update()
        assertTrue(mouse.isInTransit)
        world.update()
        assertEquals(world.cellCenter(1, 2), mouse.location)
        assertFalse(mouse.isInTransit)
    }

    @Test
    fun `world updates carry a requested step to the next cell over several iterations`() = runBlocking {
        val world = gridWorld()
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        world.addEntity(mouse)
        mouse.location = world.cellCenter(1, 1)
        mouse.movementMode = MovementMode.GRID
        mouse.gridSpeed = 32.0
        assertTrue(mouse.requestGridStep(GridDirection.EAST))
        assertTrue(mouse.isInTransit)
        world.update()
        assertTrue(mouse.isInTransit)
        assertEquals(point(128.0, 96.0), mouse.location)
        world.update()
        assertFalse(mouse.isInTransit)
        assertEquals(2 to 1, mouse.cell)
        assertEquals(world.cellCenter(2, 1), mouse.location)
    }

    @Test
    fun `grid speed at the cell size moves a whole cell in one update`() = runBlocking {
        val world = gridWorld()
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        world.addEntity(mouse)
        mouse.location = world.cellCenter(1, 1)
        mouse.movementMode = MovementMode.GRID
        mouse.gridSpeed = world.gridCellPixelSize
        assertTrue(mouse.requestGridStep(GridDirection.SOUTH))
        assertEquals(1 to 2, mouse.cell)
        assertFalse(mouse.isInTransit)
    }

    @Test
    fun `no new step is accepted while one is in transit`() {
        val world = gridWorld()
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.location = world.cellCenter(1, 1)
        mouse.movementMode = MovementMode.GRID
        mouse.gridSpeed = 8.0
        assertTrue(mouse.requestGridStep(GridDirection.SOUTH))
        assertFalse(mouse.requestGridStep(GridDirection.EAST))
        assertFalse(mouse.stepOneCell(GridDirection.EAST))
        mouse.advanceTransit(1000.0)
        assertEquals(1 to 2, mouse.cell)
        assertTrue(mouse.stepOneCell(GridDirection.EAST))
    }

    @Test
    fun `switching to continuous mode cancels a transit`() {
        val world = gridWorld()
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.location = world.cellCenter(1, 1)
        mouse.movementMode = MovementMode.GRID
        mouse.gridSpeed = 8.0
        assertTrue(mouse.requestGridStep(GridDirection.SOUTH))
        mouse.movementMode = MovementMode.CONTINUOUS
        assertFalse(mouse.isInTransit)
    }

    @Test
    fun `manual key direction is consumed at every cell center`() = runBlocking {
        val world = gridWorld()
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        world.addEntity(mouse)
        mouse.location = world.cellCenter(0, 0)
        mouse.movementMode = MovementMode.GRID
        mouse.gridSpeed = world.gridCellPixelSize
        mouse.manualGridDirection = GridDirection.EAST
        world.update()
        world.update()
        assertEquals(2 to 0, mouse.cell)
        assertEquals(0.0, mouse.heading)
        mouse.manualGridDirection = null
        world.update()
        assertEquals(2 to 0, mouse.cell)
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
        mouse.gridSpeed = world.gridCellPixelSize
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

    @Test
    fun `grid entities survive a save and reload`() = runBlocking {
        val component = OdorWorldComponent("Test")
        val world = component.world.apply {
            tileMap = TileMap(8, 8)
            gridCellSizeInTiles = 2
        }
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        world.addEntity(mouse)
        mouse.location = world.cellCenter(1, 2)
        mouse.movementMode = MovementMode.GRID
        mouse.gridSpeed = 16.0
        val reopened = OdorWorldComponent.open(component.xml.byteInputStream(), "reopened", "xml").world
        val loadedMouse = reopened.entityList.single { it.entityType == EntityType.Mouse }
        assertEquals(MovementMode.GRID, loadedMouse.movementMode)
        assertEquals(world.cellCenter(1, 2), loadedMouse.location)
        assertEquals(16.0, loadedMouse.gridSpeed)
        assertFalse(loadedMouse.isInTransit)
    }

    @Test
    fun `worlds saved before grid movement existed load with the default cell size`() = runBlocking {
        val component = OdorWorldComponent("Test")
        val xml = component.xml
        assertTrue(xml.contains("<gridCellSizeInTiles>"))
        val legacy = xml.replace(Regex("<gridCellSizeInTiles>\\d+</gridCellSizeInTiles>"), "")
        val reopened = OdorWorldComponent.open(legacy.byteInputStream(), "legacy", "xml").world
        assertEquals(2, reopened.gridCellSizeInTiles)
        assertTrue(reopened.gridColumns > 0)
        assertEquals(0 to 0, reopened.cellAt(point(10.0, 10.0)))
    }

    @Test
    fun `a cell larger than the map counts as one cell`() {
        val world = OdorWorld().apply {
            tileMap = TileMap(8, 8)
            wrapAround = true
            gridCellSizeInTiles = 10
        }
        assertEquals(1, world.gridColumns)
        assertEquals(1, world.gridRows)
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.movementMode = MovementMode.GRID
        assertEquals(0 to 0, mouse.cell)
        mouse.stepOneCell(GridDirection.EAST)
        assertEquals(0 to 0, mouse.cell)
    }

    @Test
    fun `a step from outside a smaller maze is blocked by its border from both sides`() {
        val world = OdorWorld().apply {
            tileMap = TileMap(10, 10)
            gridCellSizeInTiles = 2
            wrapAround = false
            isObjectsBlockMovement = false
        }
        world.maze = openMaze(3, 3)
        val outside = OdorWorldEntity(world, EntityType.Mouse)
        outside.location = world.cellCenter(3, 1)
        outside.movementMode = MovementMode.GRID
        assertFalse(outside.stepOneCell(GridDirection.WEST))
        assertEquals(3 to 1, outside.cell)
        assertTrue(outside.stepOneCell(GridDirection.NORTH))
        val inside = OdorWorldEntity(world, EntityType.Mouse)
        inside.location = world.cellCenter(2, 1)
        inside.movementMode = MovementMode.GRID
        assertFalse(inside.stepOneCell(GridDirection.EAST))
    }

    @Test
    fun `continuous movement is stopped by a maze wall at any speed`() {
        for (speed in listOf(10.0, 25.0, 40.0, 60.0)) {
            val world = gridWorld()
            world.maze = Maze(4, 4)
            val mouse = OdorWorldEntity(world, EntityType.Mouse)
            mouse.location = world.cellCenter(0, 0)
            mouse.heading = 0.0
            mouse.movement.speed = speed
            repeat(10) { mouse.applyMovement() }
            assertTrue(mouse.x + mouse.width / 2 <= 64.0 + 1e-9, "speed $speed pushed through the wall to x=${mouse.x}")
            assertEquals(0 to 0, mouse.cell, "speed $speed")
        }
    }

    @Test
    fun `a continuous mover slides along a wall it is pressed against`() {
        val world = gridWorld()
        world.maze = openMaze(4, 4).apply {
            for (row in 0 until 4) setWall(0, row, GridDirection.EAST, true)
        }
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        mouse.location = world.cellCenter(0, 2)
        mouse.heading = 45.0
        mouse.movement.speed = 20.0
        val startY = mouse.y
        repeat(3) { mouse.applyMovement() }
        assertTrue(mouse.x + mouse.width / 2 <= 64.0 + 1e-9)
        assertTrue(mouse.y < startY - 30.0, "should keep moving north along the wall, y=${mouse.y}")
        assertFalse(mouse.wasStuckLastTick)
    }

    private suspend fun gridMouse(world: OdorWorld, column: Int, row: Int, speed: Double = world.gridCellPixelSize): OdorWorldEntity {
        val mouse = OdorWorldEntity(world, EntityType.Mouse)
        world.addEntity(mouse)
        mouse.location = world.cellCenter(column, row)
        mouse.movementMode = MovementMode.GRID
        mouse.gridSpeed = speed
        return mouse
    }

    @Test
    fun `queued steps walk one cell per update and complete when the last one arrives`() = runBlocking {
        val world = gridWorld()
        val mouse = gridMouse(world, 1, 2)
        val plan = mouse.queueGridSteps { north(2); east() }
        world.update()
        world.update()
        assertEquals(1 to 0, mouse.cell)
        assertFalse(plan.isCompleted)
        world.update()
        assertEquals(2 to 0, mouse.cell)
        assertTrue(plan.isCompleted)
        assertTrue(plan.await())
        assertFalse(mouse.hasQueuedGridSteps)
        world.update()
        assertEquals(2 to 0, mouse.cell)
    }

    @Test
    fun `a blocked queued step fails the plan and drops the rest of it`() = runBlocking {
        val world = gridWorld()
        world.maze = Maze(4, 4)
        val mouse = gridMouse(world, 1, 1)
        val plan = mouse.queueGridSteps { east(); north() }
        world.update()
        assertEquals(1 to 1, mouse.cell)
        assertFalse(plan.await())
        assertFalse(mouse.hasQueuedGridSteps)
        assertTrue(mouse.wasStuckLastTick)
        world.update()
        assertEquals(1 to 1, mouse.cell)
    }

    @Test
    fun `until blocked walks to the wall and then the plan continues`() = runBlocking {
        val world = gridWorld()
        world.maze = openMaze(4, 4)
        val mouse = gridMouse(world, 0, 1)
        val plan = mouse.queueGridSteps { untilBlocked(GridDirection.EAST); north() }
        repeat(3) { world.update() }
        assertEquals(3 to 1, mouse.cell)
        assertFalse(plan.isCompleted)
        world.update()
        assertEquals(3 to 0, mouse.cell)
        assertTrue(plan.await())
    }

    @Test
    fun `walking suspends until a gliding plan finishes`() = runBlocking {
        val world = gridWorld()
        val mouse = gridMouse(world, 1, 1, speed = 32.0)
        val walk = async(start = CoroutineStart.UNDISPATCHED) { mouse.walkGridSteps { east() } }
        assertFalse(walk.isCompleted)
        world.update()
        assertFalse(walk.isCompleted)
        world.update()
        assertTrue(walk.await())
        assertEquals(2 to 1, mouse.cell)
    }

    @Test
    fun `manual keys take priority over a queued plan and a new plan replaces the old one`() = runBlocking {
        val world = gridWorld()
        val mouse = gridMouse(world, 1, 1)
        val first = mouse.queueGridSteps { north() }
        mouse.manualGridDirection = GridDirection.SOUTH
        world.update()
        assertEquals(1 to 2, mouse.cell)
        assertTrue(mouse.hasQueuedGridSteps)
        mouse.manualGridDirection = null
        val second = mouse.queueGridSteps { west() }
        assertFalse(first.await())
        world.update()
        assertEquals(0 to 2, mouse.cell)
        assertTrue(second.await())
    }
}
