/**
 * Checks that every entity type's images load from resources or a procedural renderer.
 */
package org.simbrain.world.odorworld

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity

class EntityTypeImagesTest {
    @Test
    fun `every entity type image loads for every direction and frame`() {
        val world = OdorWorld()
        val missing = EntityType::class.sealedSubclasses.mapNotNull { it.objectInstance }.flatMap { type ->
            val entity = OdorWorldEntity(world, type)
            type.imageBasePaths.indices.flatMap { direction ->
                (0 until type.imageBasePaths[direction].size).mapNotNull { frame ->
                    val image = entity.getImage(direction, frame)
                    val allMagenta = (0 until image.width).all { x ->
                        (0 until image.height).all { y -> image.getRGB(x, y) == 0xFFFF00FF.toInt() }
                    }
                    if (allMagenta) "$type direction $direction frame $frame" else null
                }
            }
        }
        assertTrue(missing.isEmpty(), "Missing images: $missing")
    }
}
