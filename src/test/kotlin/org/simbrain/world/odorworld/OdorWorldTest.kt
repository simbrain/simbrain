package org.simbrain.world.odorworld

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.simbrain.util.point
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class OdorWorldTest {

    var world = OdorWorld()

    @Test
    fun testEditingExistingTile() {
        world.tileMap.setTile("Tile Layer 1", 4, 4, 25)
        assertEquals(25, world.tileMap.layers[0][4, 4])
    }

    @Test
    fun testUpdateMapSize() {
        world.tileMap.updateMapSize(20, 10)
        assert(world.tileMap.width == 20)
        assert(world.tileMap.height == 10)
    }

    @Test
    fun testSetCollisionProperty() {
        world.tileMap.getLayer("Tile Layer 1").setProperty("collision", "true")
        Assertions.assertTrue(world.tileMap.getLayer("Tile Layer 1").collision)
    }

    @Test
    fun testXStream() {

        // Create a world
        val oc = OdorWorldComponent("Test")
        val swiss = OdorWorldEntity(oc.world, EntityType.SWISS)
        swiss.location = point(5,6)
        oc.world.addEntity(swiss)
        val mouse = OdorWorldEntity(oc.world, EntityType.MOUSE)
        mouse.location = point(10,11)
        oc.world.addEntity(mouse)

        // Create xstream for world
        val xstream = oc.xml
        // println(xstream)
        val stream: InputStream = ByteArrayInputStream(xstream.toByteArray(StandardCharsets.UTF_8))

        // Unmarshall from xstream
        val oc2 = OdorWorldComponent.open(stream, "test2", "xml")

        val swiss2 = oc2.world.entityList.firstOrNull { it.entityType == EntityType.SWISS }
        val mouse2 = oc2.world.entityList.firstOrNull { it.entityType == EntityType.MOUSE }
        assertNotNull(mouse2)
        assertNotNull(swiss2)
        assertEquals(mouse.location, mouse2?.location)
        assertEquals(swiss.location, swiss2?.location)
    }

}