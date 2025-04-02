package org.simbrain.world.odorworld.entities

import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

/**
 * The type of an odor world entity (swiss, candle, etc).
 *
 * For the mapping from type to image, see [EntityNode].
 *
 * @param rotating: true if separate images are provided for different headings
 * @param headLocation location of the head in uv coordinates when the agent is facing East.
 *                     Both numbers are ratios. Example: (.5, .5) is the center of the image.
 *                     (.5, 1.1) is just outside the right side of the image.
 * @param pivotLocation location of the pivot point when rotating in uv coordinate. defaults to [headLocation]
 *                      Pivot is the uv coordinates around which the agent’s “head” is assumed to rotate.
 *                      Used to determine location of speech and hearing bubbles as agents rotate
 */
sealed class EntityType(
    val description: String,
    val rotating: Boolean,
    val width: Int,
    val height: Int,
    val imageName: String? = null,
    val imageExt: String = "png",
    val numFrames: Int = if (rotating) 8 else 1,
    val headLocation: Point2D = point(0.5, 0.5),
    val pivotLocation: Point2D = headLocation
): EditableObject {
    object Swiss: EntityType("Swiss", rotating = false, width = 32, height = 32, imageExt = "gif")
    object Gouda : EntityType("Gouda", rotating = false, width = 32, height = 32, imageExt = "gif")
    object BlueCheese : EntityType("Blue Cheese", imageName = "Bluecheese", rotating = false, width = 32, height = 32, imageExt = "gif")
    object Bell : EntityType("Bell", rotating = false, width = 32, height = 32, imageExt = "gif")
    object Poison : EntityType("Poison", rotating = false, width = 32, height = 32, imageExt = "gif")
    object Candle : EntityType("Candle", rotating = false, width = 32, height = 32)
    object Fish : EntityType("Fish", rotating = false, width = 32, height = 32, imageExt = "gif")
    object Flower : EntityType("Flower", rotating = false, width = 32, height = 32, imageExt = "gif")
    object Dandelions : EntityType("Dandelions", rotating = false, width = 32, height = 32)
    object Geraniums : EntityType("Geraniums", rotating = false, width = 32, height = 32)
    object Tulip : EntityType("Tulip", rotating = false, width = 32, height = 32, imageExt = "gif")
    object Flax : EntityType("Flax", rotating = false, width = 32, height = 32, imageExt = "gif")
    object Pansy : EntityType("Pansy", rotating = false, width = 32, height = 32, imageExt = "gif")
    object Amy : EntityType("Amy", rotating = true, width = 96, height = 96, headLocation = point(0.5, 0.25))
    object Arno : EntityType("Arno", rotating = true, width = 96, height = 96)
    object Boy : EntityType("Boy", rotating = true, width = 96, height = 96)
    object Cow : EntityType("Cow", rotating = true, width = 96, height = 96)
    object Girl : EntityType("Girl", rotating = true, width = 96, height = 96)
    object Jake : EntityType("Jake", rotating = true, width = 96, height = 96)
    object Lion : EntityType("Lion", rotating = true, width = 96, height = 96)
    object Steve : EntityType("Steve", rotating = true, width = 96, height = 96)
    object Susi : EntityType("Susi", rotating = true, width = 96, height = 96)
    object Isopod : EntityType("Isopod", rotating = true, width = 18, height = 18, numFrames = 2)
    object Candy : EntityType("Candy", rotating = false, width = 35, height = 35)
    object Fork : EntityType("Fork", rotating = false, width = 11, height = 35)
    object Handle : EntityType("Handle", rotating = false, width = 75, height = 75)

    object Circle : EntityType("Circle", rotating = true, width = 73, height = 73, imageName = "circle") {
        override val imageBasePaths by lazy {
            listOf(listOf("circle" / "circle.png"))
        }
    }

    object Mouse : EntityType("Mouse", rotating = true, width = 40, height = 40, headLocation = point(1.1, 0.5), pivotLocation = point(0.5, 0.5)) {
        override val imageBasePaths by lazy {
            (0 until 360 step 15).map { angle ->
                listOf("mouse" / "Mouse_$angle.gif")
            }
        }
    }

    val boundingBox by lazy {
        Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble())
    }

    fun computeHeadLocation(heading: Double): Point2D {
        val headUVVector = (headLocation - pivotLocation).rotate(-heading.toRadian())
        return boundingBox.uv(pivotLocation + headUVVector) - point(width / 2.0, height / 2.0)
    }

    open val imageBasePaths by lazy {
        if (rotating) {
            val baseName = imageName ?: description.lowercase()
            listOf("e", "ne", "n", "nw", "w", "sw", "s", "se").map { prefix ->
                (0 until numFrames).map { frame ->
                    baseName / "${prefix}${String.format("%04d", frame)}.$imageExt"
                }
            }
        } else {
            listOf(listOf("${imageName ?: description}.$imageExt"))
        }
    }


    override fun toString(): String {
        return description
    }
}