package org.simbrain.world.odorworld.behaviors

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.point
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity

class SteeringTest {

    @Test
    fun `applyHeading clamps positive turn to maxTurn`() {
        val world = OdorWorld()
        val entity = OdorWorldEntity(world)
        entity.heading = 0.0
        Steering.applyHeading(entity, targetHeading = 90.0, speed = 1.0, maxTurn = 10.0)
        assertEquals(10.0, entity.movement.dtheta, 0.001)
        assertEquals(1.0, entity.movement.speed, 0.001)
    }

    @Test
    fun `applyHeading clamps negative turn to minus maxTurn`() {
        val world = OdorWorld()
        val entity = OdorWorldEntity(world)
        entity.heading = 90.0
        Steering.applyHeading(entity, targetHeading = 0.0, speed = 1.0, maxTurn = 10.0)
        assertEquals(-10.0, entity.movement.dtheta, 0.001)
    }

    @Test
    fun `applyHeading does not clamp when delta is within maxTurn`() {
        val world = OdorWorld()
        val entity = OdorWorldEntity(world)
        entity.heading = 0.0
        Steering.applyHeading(entity, targetHeading = 5.0, speed = 2.5, maxTurn = 10.0)
        assertEquals(5.0, entity.movement.dtheta, 0.001)
        assertEquals(2.5, entity.movement.speed, 0.001)
    }

    @Test
    fun `applyHeading takes the short way across zero`() {
        val world = OdorWorld()
        val entity = OdorWorldEntity(world)
        entity.heading = 350.0
        Steering.applyHeading(entity, targetHeading = 10.0, speed = 1.0, maxTurn = 30.0)
        assertEquals(20.0, entity.movement.dtheta, 0.001)
    }

    @Test
    fun `applyHeading writes intended values to debug info when capture is on`() {
        val world = OdorWorld()
        val entity = OdorWorldEntity(world)
        entity.steeringDebug = SteeringDebugInfo(
            DoubleArray(0), DoubleArray(0), DoubleArray(0), 0.0, 0.0
        )
        Steering.applyHeading(entity, targetHeading = 45.0, speed = 1.5, maxTurn = 20.0)
        val debug = entity.steeringDebug
        assertNotNull(debug)
        assertEquals(1.5, debug!!.intendedSpeed, 0.001)
        assertEquals(20.0, debug.intendedDtheta, 0.001)
    }

    @Test
    fun `stop zeros out speed and dtheta`() {
        val world = OdorWorld()
        val entity = OdorWorldEntity(world)
        entity.movement.speed = 5.0
        entity.movement.dtheta = 3.0
        Steering.stop(entity, "no targets")
        assertEquals(0.0, entity.movement.speed, 0.001)
        assertEquals(0.0, entity.movement.dtheta, 0.001)
    }

    @Test
    fun `stop does not write debug snapshot when capture is off`() {
        val world = OdorWorld()
        val entity = OdorWorldEntity(world)
        entity.showSteeringDebug = false
        Steering.stop(entity, "no targets")
        assertNull(entity.steeringDebug)
    }

    @Test
    fun `stop writes empty snapshot with reason when capture is on`() {
        val world = OdorWorld()
        val entity = OdorWorldEntity(world)
        entity.showSteeringDebug = true
        Steering.stop(entity, "no targets in range")
        val debug = entity.steeringDebug
        assertNotNull(debug)
        assertEquals(0, debug!!.headings.size)
        assertEquals(0, debug.scores.size)
        assertEquals(0, debug.obstacleDistances.size)
        assertEquals(0.0, debug.feelerLength, 0.001)
        assertEquals("no targets in range", debug.behaviorNotes)
    }

    @Test
    fun `pickBestHeading sweeps the full circle and picks the heading with the highest score`() {
        val world = OdorWorld()
        world.isObjectsBlockMovement = false
        val entity = OdorWorldEntity(world)
        entity.location = point(224.0, 224.0)
        entity.heading = 0.0
        val best = Steering.pickBestHeading(
            entity,
            feelerLength = 32.0,
            numCandidates = 8,
            isObstacle = { false }
        ) { heading, _, _, _ ->
            // Maximum at heading 90.
            -kotlin.math.abs(heading - 90.0)
        }
        assertEquals(90.0, best, 0.001)
    }

    @Test
    fun `pickBestHeading uses screen-space heading convention 0 deg is plus x`() {
        val world = OdorWorld()
        world.isObjectsBlockMovement = false
        val entity = OdorWorldEntity(world)
        entity.location = point(224.0, 224.0)
        entity.heading = 0.0
        val capturedDirs = HashMap<Int, Pair<Double, Double>>()
        Steering.pickBestHeading(
            entity,
            feelerLength = 32.0,
            numCandidates = 4,
            isObstacle = { false }
        ) { heading, dirX, dirY, _ ->
            capturedDirs[heading.toInt()] = dirX to dirY
            0.0
        }
        // Heading 0 -> +x (right)
        assertEquals(1.0, capturedDirs[0]!!.first, 0.001)
        assertEquals(0.0, capturedDirs[0]!!.second, 0.001)
        // Heading 90 -> -y (up in screen space)
        assertEquals(0.0, capturedDirs[90]!!.first, 0.001)
        assertEquals(-1.0, capturedDirs[90]!!.second, 0.001)
        // Heading 180 -> -x (left)
        assertEquals(-1.0, capturedDirs[180]!!.first, 0.001)
        assertEquals(0.0, capturedDirs[180]!!.second, 0.001)
        // Heading 270 -> +y (down in screen space)
        assertEquals(0.0, capturedDirs[270]!!.first, 0.001)
        assertEquals(1.0, capturedDirs[270]!!.second, 0.001)
    }

    @Test
    fun `pickBestHeading detects another entity AABB as an obstacle`() = runBlocking {
        val world = OdorWorld()
        world.wrapAround = false
        world.isObjectsBlockMovement = true
        val agent = OdorWorldEntity(world, EntityType.Swiss)
        agent.location = point(224.0, 224.0)
        agent.heading = 0.0
        world.addEntity(agent)
        val obstacle = OdorWorldEntity(world, EntityType.Swiss)
        obstacle.location = point(392.0, 224.0)
        world.addEntity(obstacle)

        // Agent at x=224, obstacle at x=392; obstacle and agent are both Swiss (32x32).
        // Minkowski-expanded obstacle box left edge is at 392 - 16 - 16 = 360.
        // Ray from agent (heading 0, +x) should hit at distance 360 - 224 = 136.
        // Use feelerLength = 180 so the ray stays well clear of the right wall (at x=448).
        val obstacleDistances = HashMap<Int, Double>()
        Steering.pickBestHeading(
            agent,
            feelerLength = 180.0,
            numCandidates = 4,
            isObstacle = { it !== agent }
        ) { heading, _, _, dist ->
            obstacleDistances[heading.toInt()] = dist
            0.0
        }
        // Heading 0 (+x) toward obstacle.
        assertEquals(136.0, obstacleDistances[0]!!, 1.0)
        // Heading 90 (-y, up) should see no AABB hit and stay clear of the top wall.
        assertEquals(180.0, obstacleDistances[90]!!, 0.001)
    }

    @Test
    fun `pickBestHeading captures debug snapshot when showSteeringDebug is on`() {
        val world = OdorWorld()
        world.isObjectsBlockMovement = false
        val entity = OdorWorldEntity(world)
        entity.location = point(224.0, 224.0)
        entity.heading = 0.0
        entity.showSteeringDebug = true
        Steering.pickBestHeading(
            entity,
            feelerLength = 32.0,
            numCandidates = 6,
            isObstacle = { false }
        ) { _, _, _, _ -> 0.0 }
        val debug = entity.steeringDebug
        assertNotNull(debug)
        assertEquals(6, debug!!.headings.size)
        assertEquals(6, debug.scores.size)
        assertEquals(6, debug.obstacleDistances.size)
        assertEquals(32.0, debug.feelerLength, 0.001)
    }

    @Test
    fun `pickBestHeading does not allocate debug arrays when capture is off`() {
        val world = OdorWorld()
        val entity = OdorWorldEntity(world)
        entity.location = point(224.0, 224.0)
        entity.showSteeringDebug = false
        Steering.pickBestHeading(
            entity,
            feelerLength = 32.0,
            numCandidates = 6,
            isObstacle = { false }
        ) { _, _, _, _ -> 0.0 }
        assertNull(entity.steeringDebug)
    }

    @Test
    fun `pickBestHeading angularOffset rotates the candidate set`() {
        val world = OdorWorld()
        world.isObjectsBlockMovement = false
        val entity = OdorWorldEntity(world)
        entity.location = point(224.0, 224.0)
        entity.heading = 0.0
        val headings = mutableListOf<Double>()
        Steering.pickBestHeading(
            entity,
            feelerLength = 32.0,
            numCandidates = 4,
            angularOffset = 10.0,
            isObstacle = { false }
        ) { heading, _, _, _ ->
            headings.add(heading)
            0.0
        }
        assertTrue(headings.any { kotlin.math.abs(it - 10.0) < 0.001 })
        assertTrue(headings.any { kotlin.math.abs(it - 100.0) < 0.001 })
        assertTrue(headings.any { kotlin.math.abs(it - 190.0) < 0.001 })
        assertTrue(headings.any { kotlin.math.abs(it - 280.0) < 0.001 })
    }
}
