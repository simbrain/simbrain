/** Tests runtime-generated OdorWorld entity sprite images. */
package org.simbrain.world.odorworld

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity

class ProceduralEntityImagesTest {

    @Test
    fun `procedural images are available to non GUI renderers`() {
        for (type in listOf(EntityType.Nematode, EntityType.Pillbug)) {
            val entity = OdorWorldEntity(OdorWorld(), type)

            val image = entity.getImage(directionIndex = 2)

            assertEquals(0, image.width % type.width)
            assertEquals(image.width / type.width, image.height / type.height)
            assertTrue((0 until image.width).any { x ->
                (0 until image.height).any { y -> image.getRGB(x, y) ushr 24 != 0 }
            })
            assertTrue((0 until image.width).none { x ->
                (0 until image.height).any { y -> image.getRGB(x, y) == 0xFFFF00FF.toInt() }
            })
        }
    }
}
