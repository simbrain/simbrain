/**
 * Checks that an entity's scale sets its world size and survives saving and loading.
 */
package org.simbrain.world.odorworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity

class EntityScaleTest {
    @Test
    fun `scale multiplies the entity type size`() {
        val world = OdorWorld()
        val entity = OdorWorldEntity(world, EntityType.Cow)
        assertEquals(96.0, entity.width)
        entity.scale = 0.5
        assertEquals(48.0, entity.width)
        assertEquals(48.0, entity.height)
    }

    @Test
    fun `scale is saved and restored`() {
        val world = OdorWorld()
        runBlocking { world.addEntity(OdorWorldEntity(world, EntityType.Swiss).apply { scale = 2.5 }) }
        val xstream = OdorWorldComponent.odorWorldXStream
        val restored = xstream.fromXML(xstream.toXML(world)) as OdorWorld
        val entity = restored.entityList.single()
        assertEquals(2.5, entity.scale)
        assertEquals(80.0, entity.width)
    }

    @Test
    fun `world saved without scale loads at native size`() {
        val world = OdorWorld()
        runBlocking { world.addEntity(OdorWorldEntity(world, EntityType.Swiss).apply { scale = 2.5 }) }
        val xstream = OdorWorldComponent.odorWorldXStream
        val xml = xstream.toXML(world)
        val legacyXml = xml.replace(Regex("""\s*<scale>2\.5</scale>"""), "")
        assertNotEquals(xml, legacyXml)
        val restored = xstream.fromXML(legacyXml) as OdorWorld
        assertEquals(1.0, restored.entityList.single().scale)
    }
}
