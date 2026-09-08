package org.simbrain.world.odorworld.behaviors

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.point
import org.simbrain.world.odorworld.GridDirection
import org.simbrain.world.odorworld.Maze
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.MovementMode
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import kotlin.math.abs

class BehaviorsTest {

    @Test
    fun `NoOpBehavior leaves movement untouched`() {
        val world = OdorWorld()
        val entity = OdorWorldEntity(world)
        entity.movement.speed = 3.0
        entity.movement.dtheta = 4.0
        NoOpBehavior().update(entity)
        assertEquals(3.0, entity.movement.speed, 0.001)
        assertEquals(4.0, entity.movement.dtheta, 0.001)
    }

    @Test
    fun `NoOpBehavior copy returns a new NoOpBehavior`() {
        val original = NoOpBehavior()
        val copy = original.copy()
        assertTrue(copy !== original)
    }

    @Test
    fun `Pursue with no targets in range stops the entity`() {
        val world = OdorWorld()
        world.isObjectsBlockMovement = false
        val agent = OdorWorldEntity(world)
        agent.location = point(100.0, 100.0)
        agent.movement.speed = 5.0
        agent.movement.dtheta = 2.0
        val pursue = Pursue().also {
            it.targetType = EntityType.Swiss
            it.visionRange = 50.0
        }
        pursue.update(agent)
        assertEquals(0.0, agent.movement.speed, 0.001)
        assertEquals(0.0, agent.movement.dtheta, 0.001)
    }

    @Test
    fun `Pursue with target directly ahead moves at max speed without turning`() = runBlocking {
        val world = OdorWorld()
        world.wrapAround = false
        world.isObjectsBlockMovement = false
        val agent = OdorWorldEntity(world, EntityType.Mouse)
        agent.location = point(100.0, 100.0)
        agent.heading = 0.0
        world.addEntity(agent)
        val target = OdorWorldEntity(world, EntityType.Swiss)
        target.location = point(300.0, 100.0)
        world.addEntity(target)
        val pursue = Pursue().also {
            it.targetType = EntityType.Swiss
            it.maxSpeed = 2.0
            it.maxTurn = 10.0
            it.feelerLength = 64.0
            it.numRays = 4
            it.visionRange = 400.0
        }
        pursue.update(agent)
        assertEquals(2.0, agent.movement.speed, 0.001)
        assertEquals(0.0, agent.movement.dtheta, 0.001)
    }

    @Test
    fun `Pursue records behavior notes when showSteeringDebug is on`() = runBlocking {
        val world = OdorWorld()
        world.wrapAround = false
        world.isObjectsBlockMovement = false
        val agent = OdorWorldEntity(world, EntityType.Mouse)
        agent.location = point(100.0, 100.0)
        agent.showSteeringDebug = true
        world.addEntity(agent)
        val target = OdorWorldEntity(world, EntityType.Swiss)
        target.location = point(300.0, 100.0)
        world.addEntity(target)
        val pursue = Pursue().also {
            it.targetType = EntityType.Swiss
            it.numRays = 4
            it.visionRange = 400.0
        }
        pursue.update(agent)
        val debug = agent.steeringDebug
        assertNotNull(debug)
        assertTrue(debug!!.behaviorNotes.contains("Pursue"))
    }

    @Test
    fun `Pursue copy preserves all parameters`() {
        val original = Pursue().apply {
            targetType = EntityType.Mouse
            maxSpeed = 7.0
            visionRange = 123.0
            leadTicks = 4.0
            maxTurn = 11.0
            feelerLength = 99.0
            wallWeight = 2.5
            numRays = 16
        }
        val copy = original.copy()
        assertEquals(EntityType.Mouse, copy.targetType)
        assertEquals(7.0, copy.maxSpeed)
        assertEquals(123.0, copy.visionRange)
        assertEquals(4.0, copy.leadTicks)
        assertEquals(11.0, copy.maxTurn)
        assertEquals(99.0, copy.feelerLength)
        assertEquals(2.5, copy.wallWeight)
        assertEquals(16, copy.numRays)
        assertTrue(copy !== original)
    }

    @Test
    fun `Evade with no threats in range stops the entity`() {
        val world = OdorWorld()
        world.isObjectsBlockMovement = false
        val agent = OdorWorldEntity(world)
        agent.location = point(100.0, 100.0)
        agent.movement.speed = 5.0
        agent.movement.dtheta = 2.0
        val evade = Evade().also {
            it.threatType = EntityType.Swiss
            it.visionRange = 50.0
        }
        evade.update(agent)
        assertEquals(0.0, agent.movement.speed, 0.001)
        assertEquals(0.0, agent.movement.dtheta, 0.001)
    }

    @Test
    fun `Evade with threat ahead picks a heading away from the threat`() = runBlocking {
        val world = OdorWorld()
        world.wrapAround = false
        world.isObjectsBlockMovement = false
        val agent = OdorWorldEntity(world, EntityType.Mouse)
        agent.location = point(100.0, 100.0)
        agent.heading = 0.0
        world.addEntity(agent)
        val threat = OdorWorldEntity(world, EntityType.Swiss)
        threat.location = point(300.0, 100.0)
        world.addEntity(threat)
        val evade = Evade().also {
            it.threatType = EntityType.Swiss
            it.maxSpeed = 2.0
            it.maxTurn = 10.0
            it.feelerLength = 64.0
            it.numRays = 4
            it.visionRange = 400.0
        }
        evade.update(agent)
        assertEquals(2.0, agent.movement.speed, 0.001)
        // The agent must turn (any direction) — heading 0 is the worst score with the threat
        // straight ahead, so applyHeading must clamp to a non-zero turn.
        assertTrue(abs(agent.movement.dtheta) > 0.0)
    }

    @Test
    fun `Evade copy preserves all parameters`() {
        val original = Evade().apply {
            threatType = EntityType.Mouse
            maxSpeed = 6.0
            visionRange = 150.0
            leadTicks = 3.0
            maxTurn = 12.0
            feelerLength = 88.0
            wallWeight = 1.25
            threatWeight = 3.0
            numRays = 32
        }
        val copy = original.copy()
        assertEquals(EntityType.Mouse, copy.threatType)
        assertEquals(6.0, copy.maxSpeed)
        assertEquals(150.0, copy.visionRange)
        assertEquals(3.0, copy.leadTicks)
        assertEquals(12.0, copy.maxTurn)
        assertEquals(88.0, copy.feelerLength)
        assertEquals(1.25, copy.wallWeight)
        assertEquals(3.0, copy.threatWeight)
        assertEquals(32, copy.numRays)
        assertTrue(copy !== original)
    }

    @Test
    fun `Wander writes max speed and clamps dtheta within maxTurn`() {
        val world = OdorWorld()
        world.isObjectsBlockMovement = false
        val agent = OdorWorldEntity(world)
        agent.location = point(224.0, 224.0)
        agent.heading = 0.0
        val wander = Wander().also {
            it.maxSpeed = 1.5
            it.maxTurn = 4.0
            it.feelerLength = 32.0
            it.numRays = 8
        }
        wander.update(agent)
        assertEquals(1.5, agent.movement.speed, 0.001)
        assertTrue(abs(agent.movement.dtheta) <= 4.0 + 1e-9)
    }

    @Test
    fun `Wander copy preserves all parameters`() {
        val original = Wander().apply {
            maxSpeed = 2.5
            maxTurn = 7.0
            feelerLength = 50.0
            wallWeight = 1.5
            driftDegreesPerTick = 9.0
            numRays = 32
            gridTurnChance = 0.6
        }
        val copy = original.copy()
        assertEquals(0.6, copy.gridTurnChance)
        assertEquals(2.5, copy.maxSpeed)
        assertEquals(7.0, copy.maxTurn)
        assertEquals(50.0, copy.feelerLength)
        assertEquals(1.5, copy.wallWeight)
        assertEquals(9.0, copy.driftDegreesPerTick)
        assertEquals(32, copy.numRays)
        assertTrue(copy !== original)
    }

    private fun gridWorld() = OdorWorld().apply {
        tileMap = TileMap(8, 8)
        gridCellSizeInTiles = 2
        gridStepDurationMs = 0
        wrapAround = false
        isObjectsBlockMovement = false
    }

    private suspend fun gridAgent(world: OdorWorld, column: Int, row: Int): OdorWorldEntity {
        val agent = OdorWorldEntity(world, EntityType.Mouse)
        world.addEntity(agent)
        agent.location = world.cellCenter(column, row)
        agent.heading = 0.0
        agent.movementMode = MovementMode.GRID
        return agent
    }

    @Test
    fun `Pursue in grid mode steps one cell toward the target along a cardinal direction`() = runBlocking {
        val world = gridWorld()
        val agent = gridAgent(world, 0, 0)
        val target = OdorWorldEntity(world, EntityType.Swiss)
        world.addEntity(target)
        target.location = world.cellCenter(0, 3)
        agent.behavior = Pursue().also { it.targetType = EntityType.Swiss; it.visionRange = 400.0 }
        world.update()
        assertEquals(0 to 1, agent.cell)
        assertEquals(GridDirection.SOUTH, agent.facingDirection)
        assertEquals(0.0, agent.movement.speed, 0.001)
    }

    @Test
    fun `Pursue in grid mode follows the shortest path through a maze to the target`() = runBlocking {
        val world = gridWorld()
        world.maze = Maze.recursiveBacktracker(4, 4, seed = 11L)
        val agent = gridAgent(world, 0, 0)
        val target = OdorWorldEntity(world, EntityType.Swiss)
        world.addEntity(target)
        target.location = world.cellCenter(3, 3)
        agent.behavior = Pursue().also { it.targetType = EntityType.Swiss; it.visionRange = 1000.0 }
        val pathLength = world.gridDistancesFrom(0 to 0).distanceAt(3 to 3)
        assertTrue(pathLength > 0)
        repeat(pathLength) {
            world.update()
            assertFalse(agent.wasStuckLastTick, "every step along the path should be open")
        }
        assertEquals(3 to 3, agent.cell)
        world.update()
        assertEquals(3 to 3, agent.cell)
        assertNull(agent.pendingGridStep)
    }

    @Test
    fun `Pursue in grid mode stops next to a target that blocks movement`() = runBlocking {
        val world = gridWorld().apply { isObjectsBlockMovement = true }
        val agent = gridAgent(world, 0, 0)
        val target = OdorWorldEntity(world, EntityType.Swiss)
        world.addEntity(target)
        target.location = world.cellCenter(2, 0)
        agent.behavior = Pursue().also { it.targetType = EntityType.Swiss; it.visionRange = 400.0 }
        world.update()
        assertEquals(1 to 0, agent.cell)
        world.update()
        assertEquals(1 to 0, agent.cell)
        assertEquals(GridDirection.EAST, agent.facingDirection)
        assertFalse(agent.wasStuckLastTick)
    }

    @Test
    fun `Evade in grid mode steps to the cell furthest from the threat by path`() = runBlocking {
        val world = gridWorld()
        world.maze = Maze.openGrid(4, 1)
        val agent = gridAgent(world, 1, 0)
        val threat = OdorWorldEntity(world, EntityType.Swiss)
        world.addEntity(threat)
        threat.location = world.cellCenter(0, 0)
        agent.behavior = Evade().also { it.threatType = EntityType.Swiss; it.visionRange = 1000.0 }
        world.update()
        assertEquals(2 to 0, agent.cell)
        world.update()
        assertEquals(3 to 0, agent.cell)
        world.update()
        assertEquals(3 to 0, agent.cell)
        assertNull(agent.pendingGridStep)
    }

    @Test
    fun `Evade in grid mode prefers a long corridor over a nearby dead end`() = runBlocking {
        val world = gridWorld()
        // Row 0 is a corridor; cell (1, 1) is a dead end hanging off (1, 0)
        world.maze = Maze(4, 4).apply {
            for (c in 0 until 3) setWall(c, 0, GridDirection.EAST, false)
            setWall(1, 0, GridDirection.SOUTH, false)
        }
        val agent = gridAgent(world, 1, 0)
        val threat = OdorWorldEntity(world, EntityType.Swiss)
        world.addEntity(threat)
        threat.location = world.cellCenter(0, 0)
        agent.behavior = Evade().also { it.threatType = EntityType.Swiss; it.visionRange = 1000.0 }
        world.update()
        world.update()
        assertEquals(3 to 0, agent.cell)
    }

    @Test
    fun `Wander in grid mode walks corridors and turns at walls without leaving cell centers`() = runBlocking {
        val world = gridWorld()
        world.maze = Maze.recursiveBacktracker(4, 4, seed = 3L)
        val agent = gridAgent(world, 1, 1)
        agent.behavior = Wander()
        val visited = mutableSetOf<Pair<Int, Int>>()
        repeat(60) {
            world.update()
            visited += agent.cell
            val (column, row) = agent.cell
            assertEquals(world.cellCenter(column, row), agent.location)
            assertEquals(0.0, agent.heading % 90.0)
        }
        assertTrue(visited.size >= 4, "should have explored several cells, visited $visited")
    }

    @Test
    fun `Wander in grid mode with no turn chance goes straight until a wall`() = runBlocking {
        val world = gridWorld()
        world.maze = Maze.openGrid(4, 4)
        val agent = gridAgent(world, 0, 0)
        agent.behavior = Wander().also { it.gridTurnChance = 0.0 }
        world.update()
        world.update()
        world.update()
        assertEquals(3 to 0, agent.cell)
        world.update()
        assertNotEquals(3 to 0, agent.cell)
    }

    @Test
    fun `stop in grid mode clears a pending step`() {
        val world = gridWorld()
        val agent = OdorWorldEntity(world, EntityType.Mouse)
        agent.movementMode = MovementMode.GRID
        Steering.applyHeading(agent, 270.0, 2.0, 10.0)
        assertEquals(GridDirection.SOUTH, agent.pendingGridStep)
        Steering.stop(agent, "done")
        assertNull(agent.pendingGridStep)
    }
}
