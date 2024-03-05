package org.simbrain.world.odorworld

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.simbrain.util.point
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class OdorWorldTest {

    var world = OdorWorld()

    @Test
    fun testSetCollisionProperty() {
        world.tileMap.getLayer("Tile Layer 1").setProperty("blocking", "true")
        Assertions.assertTrue(world.tileMap.getLayer("Tile Layer 1").blocking)
    }

    @Test
    fun testXStream() {

        // Create a world
        val oc = OdorWorldComponent("Test")
        val swiss = OdorWorldEntity(oc.world, EntityType.SWISS)
        swiss.location = point(5,6)
        oc.world.addEntity(swiss)
        val mouse = OdorWorldEntity(oc.world, EntityType.MOUSE)
        mouse.addSensor(ObjectSensor(EntityType.SWISS, 50.0, 45.0))
        mouse.addEffector(StraightMovement())

        mouse.location = point(10,11)
        oc.world.addEntity(mouse)

        // Create xstream for world
        val xstream = oc.xml
        // println(xstream)
        val stream: InputStream = ByteArrayInputStream(xstream.toByteArray(StandardCharsets.UTF_8))

        // Unmarshall from xstream
        val oc2 = OdorWorldComponent.open(stream, "test2", "xml")

        val newSwiss = oc2.world.entityList.firstOrNull { it.entityType == EntityType.SWISS }
        val newMouse = oc2.world.entityList.firstOrNull { it.entityType == EntityType.MOUSE }
        assertNotNull(newMouse)
        assertNotNull(newSwiss)
        assertEquals(mouse.location, newMouse?.location)
        assertEquals(swiss.location, newSwiss?.location)
        assertEquals(1, newMouse?.sensors?.size)
        assertEquals(1, newMouse?.effectors?.size)

    }

}