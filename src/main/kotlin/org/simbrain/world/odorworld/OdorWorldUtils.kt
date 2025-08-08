package org.simbrain.world.odorworld

import kotlinx.coroutines.runBlocking
import org.simbrain.util.SmellSource
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.point
import org.simbrain.world.odorworld.entities.BoundIntersection
import org.simbrain.world.odorworld.entities.Bounded
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.geom.Point2D
import kotlin.math.min
import kotlin.random.Random

fun Bounded.intersect(other: Bounded): BoundIntersection {
    val a = this
    val topLeftA = a.topLeftLocation
    val b = other
    val topLeftB = b.topLeftLocation

    return if (b is OdorWorld) { // world bound is inverted
        val left = topLeftA.x - topLeftB.x
        val right = (topLeftB.x + b.width) - (topLeftA.x + a.width)
        val top = topLeftA.y - topLeftB.y
        val bottom = (topLeftB.y + b.height) - (topLeftA.y + a.height)
        val xCollision = -min(left, right)
        val yCollision = -min(top, bottom)
        BoundIntersection(xCollision > 0 || yCollision > 0, xCollision, yCollision)
    } else {
        val xCollision = min((topLeftA.x + a.width) - topLeftB.x, (topLeftB.x + b.width) - topLeftA.x)
        val yCollision = min((topLeftA.y + a.height) - topLeftB.y, (topLeftB.y + b.height) - topLeftA.y)
        BoundIntersection(xCollision > 0 && yCollision > 0, xCollision, yCollision)
    }

}

fun OdorWorld.getRandomLocation(rand: Random = Random): Point2D {
    return point(rand.nextInt(width.toInt()), rand.nextInt(height.toInt()))
}

/**
 * Resizes the tile map of the `worldPanel` to fit the current frame size based on the camera's dimensions.
 *
 * Calculates the number of tiles required to fill the frame by dividing the width and height of
 * the camera by the dimensions of a single tile.
 */
fun OdorWorldDesktopComponent.fitWorldToFrameSize() {
    val width = worldPanel.canvas.camera.width.toInt()
    val height = worldPanel.canvas.camera.height.toInt()

    val widthInTiles = width / worldPanel.world.tileMap.tileWidth
    val heightInTiles = height / worldPanel.world.tileMap.tileHeight

    worldPanel.world.tileMap.updateMapSize(widthInTiles, heightInTiles)

    fitFrameToWorldSize()
}

fun OdorWorld.addDefaultEntities() {
    val world = this
    // Add agent to environment
    val mouse = OdorWorldEntity(world, EntityType.Mouse).apply {
        setLocation(162, 200)
    }

    val objects = buildList {
        add(OdorWorldEntity(world, EntityType.Swiss).apply {
            setLocation(36, 107)
            smellSource = SmellSource(doubleArrayOf(0.7, 0.3, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)).apply {
                decayFunction = GaussianDecayFunction()
            }
        })
        add(OdorWorldEntity(world, EntityType.Gouda).apply {
            setLocation(169, 32)
            smellSource = SmellSource(doubleArrayOf(0.7, 0.0, 0.3, 0.0, 0.0, 0.0, 0.0, 0.0)).apply {
                decayFunction = GaussianDecayFunction()
            }
        })
        add(OdorWorldEntity(world, EntityType.BlueCheese).apply {
            setLocation(304, 87)
            smellSource = SmellSource(doubleArrayOf(0.7, 0.0, 0.0, 0.0, 0.3, 0.0, 0.0, 0.0)).apply {
                decayFunction = GaussianDecayFunction()
            }
        })
        add(OdorWorldEntity(world, EntityType.Tulip).apply {
            setLocation(80, 351)
            smellSource = SmellSource(doubleArrayOf(0.0, 0.3, 0.0, 0.7, 0.0, 0.0, 0.0, 0.0)).apply {
                decayFunction = GaussianDecayFunction()
            }
        })
        add(OdorWorldEntity(world, EntityType.Pansy).apply {
            setLocation(251, 370)
            smellSource = SmellSource(doubleArrayOf(0.0, 0.0, 0.3, 0.7, 0.0, 0.0, 0.0, 0.0)).apply {
                decayFunction = GaussianDecayFunction()
            }
        })
    }
    runBlocking {
        world.addEntity(mouse)
        objects.forEach { world.addEntity(it) }
    }
}