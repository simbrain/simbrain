package org.simbrain.world.odorworld

import kotlinx.coroutines.runBlocking
import org.simbrain.util.SmellSource
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.point
import org.simbrain.world.odorworld.entities.BoundIntersection
import org.simbrain.world.odorworld.entities.Bounded
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.Color
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap
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
        world.addAgent().location = point(162, 200)
        objects.forEach { world.addEntity(it) }
    }
}

// ============================================================================
// Entity Image Utilities
// ============================================================================

/**
 * Thread-safe cache for entity images to avoid repeated resource loading.
 * Uses ConcurrentHashMap for safe access from multiple threads (e.g., GUI and simulation threads).
 */
private val entityImageCache = ConcurrentHashMap<String, BufferedImage>()

/**
 * Fallback image used when entity image cannot be loaded.
 */
private val fallbackImage: BufferedImage by lazy {
    BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB).apply {
        val g = createGraphics()
        g.color = Color.MAGENTA
        g.fillRect(0, 0, 32, 32)
        g.dispose()
    }
}

/**
 * Get the direction index for this entity based on a viewing angle.
 *
 * For rotating entities with multiple directional sprites (e.g., 8 directions),
 * this returns which direction's sprite should be used when viewing from the given angle.
 *
 * @param viewingAngle The angle from which the entity is being viewed (degrees, 0=East, CCW positive)
 * @return The direction index (0 to numDirections-1), or 0 for non-rotating entities
 */
fun OdorWorldEntity.getDirectionIndex(viewingAngle: Double): Int {
    val paths = entityType.imageBasePaths
    val numDirections = if (entityType.rotating && paths.size > 1) paths.size else 1
    if (numDirections == 1) return 0

    // Calculate relative viewing angle (combines entity heading and camera position)
    val relativeAngle = normalizeAngle(heading - viewingAngle + 90.0)
    val degreesPerDirection = 360.0 / numDirections

    return ((relativeAngle + degreesPerDirection / 2) / degreesPerDirection).toInt() % numDirections
}

/**
 * Get the angle difference from the optimal viewing angle for the selected sprite direction.
 *
 * This is used for foreshortening calculations when billboard mode is disabled.
 * For multi-direction sprites (8, 24), this will be small (max ±22.5° for 8-dir).
 * For single-direction sprites, this can be up to ±180°.
 *
 * @param viewingAngle The angle from which the entity is being viewed (degrees)
 * @param directionIndex The selected sprite direction index
 * @return Angle difference in degrees (-180 to 180)
 */
fun OdorWorldEntity.getAngleDiffFromOptimal(viewingAngle: Double, directionIndex: Int): Double {
    val paths = entityType.imageBasePaths
    val numDirections = if (entityType.rotating && paths.size > 1) paths.size else 1
    val degreesPerDirection = 360.0 / numDirections

    val relativeAngle = normalizeAngle(heading - viewingAngle + 90.0)
    val spriteOptimalAngle = directionIndex * degreesPerDirection

    return normalizeAngleSigned(relativeAngle - spriteOptimalAngle)
}

/**
 * Get the image for this entity at the specified direction and frame.
 *
 * @param directionIndex Direction index (0 for static entities, 0-7 for 8-direction, etc.)
 * @param frameIndex Animation frame index (0 for single-frame entities)
 * @return The BufferedImage for rendering
 */
fun OdorWorldEntity.getImage(directionIndex: Int = 0, frameIndex: Int = animationFrame): BufferedImage {
    val paths = entityType.imageBasePaths

    if (paths.isEmpty() || paths[0].isEmpty()) {
        return fallbackImage
    }

    val safeDirectionIndex = directionIndex.coerceIn(0, paths.size - 1)
    val frames = paths[safeDirectionIndex]
    val safeFrameIndex = frameIndex.coerceIn(0, frames.size - 1)
    val imagePath = frames[safeFrameIndex]

    val prefix = if (entityType.rotating) "rotating" else "static"
    val cacheKey = "$prefix/$imagePath"

    return entityImageCache.computeIfAbsent(cacheKey) {
        try {
            OdorWorldResourceManager.getBufferedImage("$prefix/$imagePath")
        } catch (e: Exception) {
            fallbackImage
        }
    }
}

/**
 * Get the current image for this entity based on its heading and animation frame.
 * This is the simplest way to get the entity's current visual representation.
 *
 * @return The current BufferedImage for this entity
 */
fun OdorWorldEntity.getCurrentImage(): BufferedImage {
    val paths = entityType.imageBasePaths

    // For rotating entities, use heading to determine direction
    val directionIndex = if (entityType.rotating && paths.size > 1) {
        // Map heading (0=East, CCW) to direction index
        // Directions are: E(0), NE(1), N(2), NW(3), W(4), SW(5), S(6), SE(7)
        val degreesPerDirection = 360.0 / paths.size
        ((heading + degreesPerDirection / 2) / degreesPerDirection).toInt() % paths.size
    } else {
        0
    }

    return getImage(directionIndex, animationFrame)
}

/**
 * Normalize angle to 0-360 range.
 */
private fun normalizeAngle(angle: Double): Double {
    var normalized = angle % 360.0
    if (normalized < 0) normalized += 360.0
    return normalized
}

/**
 * Normalize angle to -180 to 180 range (signed).
 */
private fun normalizeAngleSigned(angle: Double): Double {
    var normalized = angle % 360.0
    if (normalized > 180.0) normalized -= 360.0
    if (normalized < -180.0) normalized += 360.0
    return normalized
}