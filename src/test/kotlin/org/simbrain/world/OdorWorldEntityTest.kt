package org.simbrain.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.simbrain.util.point
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.SmellSensor

class OdorWorldEntityTest {

    // TODO: sensors, effectors, speech

    var world = OdorWorld()

    @Test
    fun `created entity can be retrieved by its id`() {
        val entity = OdorWorldEntity(world)
        world.addEntity(entity)
        assertNotNull(world.getEntity(entity.id))
    }

    @Test
    fun `entity location and heading are properly set`() {
        val entity = OdorWorldEntity(world)
        entity.location = point(40, 60)
        entity.heading = 90.0
        assertEquals(40.0, entity.x)
        assertEquals(60.0, entity.y)
        assertEquals(90.0, entity.heading)
    }

    @Test
    fun `(0,0) with heading 0 and speed 10 moves entity to (10,0)`() {
        val entity = OdorWorldEntity(world)
        entity.location = point(0, 0)
        entity.heading  = 0.0
        entity.movement.speed = 10.0
        entity.applyMovement()
        assertEquals(10.0, entity.x)
    }

    @Test
    fun `manual movement to right by 10 pixels works`() {
        val entity = OdorWorldEntity(world)
        entity.location = point(0, 0)
        entity.heading  = 0.0
        entity.manualMovement.speed = 10.0
        entity.applyMovement()
        assertEquals(10.0, entity.x)
    }

    @Test
    fun `after manual movement pre-existing movement resumes`() {
        val entity = OdorWorldEntity(world)
        entity.location = point(100, 0)
        entity.heading  = 0.0
        entity.movement.speed = -20.0
        entity.manualMovement.speed = 10.0
        entity.applyMovement()
        assertEquals(110.0, entity.x)
        entity.manualMovement.speed = 0.0
        entity.applyMovement()
        assertEquals(90.0, entity.x)
    }

    @Test
    fun `with heading 0 and dtheta 10, entity heading is 10 after one update`() {
        val entity = OdorWorldEntity(world)
        entity.heading  = 0.0
        entity.movement.dtheta = 10.0
        entity.applyMovement()
        assertEquals(10.0, entity.heading)
    }

    @Test
    fun `world wraps around properly`() {
        world.wrapAround = true
        val entity = OdorWorldEntity(world)
        entity.location = point(0,0)
        entity.heading  = 180.0 // pointing towards the wall
        entity.movement.speed = 10.0
        entity.applyMovement()
        assertEquals(world.width - 10, entity.x)
    }

    @Test
    fun `world blocks properly`() {
        world.wrapAround = false
        val entity = OdorWorldEntity(world)
        entity.location = point(0,0)
        entity.heading  = 180.0 // pointing towards the wall
        entity.movement.speed = 10.0
        entity.applyMovement()
        assertEquals(0.0, entity.topLeftLocation.x)
    }

    @Test
    fun `object blocks properly`() {
        val agent = OdorWorldEntity(world)
        agent.location = point(0,0)
        agent.heading  = 0.0
        agent.movement.speed = 10.0

        val entity = OdorWorldEntity(world)
        world.addEntity(entity)
        entity.location = point(agent.width + 5,0.0)

        agent.applyMovement()

        assertEquals(entity.x - agent.width, agent.x)

    }

    @Test
    fun `smell sensor location is correct `() {
        val agent = OdorWorldEntity(world)
        agent.location = point(10.0, 0.0)
        val sensor = SmellSensor()
        sensor.radius = 10.0
        sensor.theta = 0.0
        agent.addSensor(sensor)
        assertEquals(sensor.radius, sensor.computeRelativeLocation(agent).x)
        assertEquals(agent.x + sensor.radius, sensor.computeAbsoluteLocation(agent).x)
    }
    
}