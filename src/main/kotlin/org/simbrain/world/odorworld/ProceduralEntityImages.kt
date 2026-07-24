/**
 * Rasterizes OdorWorld entity types whose visuals are generated at runtime so the 2D world and 3D sensor share them.
 */
package org.simbrain.world.odorworld

import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

fun interface ProceduralEntityImageRenderer {
    fun paint(entity: OdorWorldEntity, frameIndex: Int, graphics: Graphics2D)
}

object ProceduralEntityImages {
    private data class ImageKey(val type: EntityType, val frame: Int, val direction: Int)

    private val renderers = mutableMapOf<EntityType, ProceduralEntityImageRenderer>()
    private val imageCache = ConcurrentHashMap<ImageKey, BufferedImage>()

    init {
        register(EntityType.Nematode, CElegansRenderer)
    }

    fun register(entityType: EntityType, renderer: ProceduralEntityImageRenderer) {
        renderers[entityType] = renderer
        imageCache.keys.removeIf { it.type == entityType }
    }

    fun getBaseImage(entity: OdorWorldEntity): BufferedImage? = getImage(entity, 0)

    fun getImage(
        entity: OdorWorldEntity,
        directionIndex: Int,
        frameIndex: Int = entity.animationFrame
    ): BufferedImage? {
        val renderer = renderers[entity.entityType] ?: return null
        val directionCount = entity.entityType.imageBasePaths.size.coerceAtLeast(1)
        val direction = directionIndex.mod(directionCount)
        val frame = frameIndex.mod(entity.entityType.numFrames)
        val key = ImageKey(entity.entityType, frame, direction)

        return imageCache.computeIfAbsent(key) {
            BufferedImage(entity.entityType.width, entity.entityType.height, BufferedImage.TYPE_INT_ARGB).apply {
                val g = createGraphics()
                try {
                    g.translate(width / 2.0, height / 2.0)
                    g.rotate(-2.0 * PI * direction / directionCount)
                    renderer.paint(entity, frame, g)
                } finally {
                    g.dispose()
                }
            }
        }
    }
}

private object CElegansRenderer : ProceduralEntityImageRenderer {
    override fun paint(entity: OdorWorldEntity, frameIndex: Int, graphics: Graphics2D) {
        val phase = 2.0 * PI * frameIndex / entity.entityType.numFrames
        val body = Path2D.Double()
        val segments = 20

        fun centerY(index: Int): Double {
            val progress = index.toDouble() / segments
            return 2.5 * sin(2.0 * PI * progress + phase)
        }

        fun halfWidth(index: Int): Double {
            val progress = index.toDouble() / segments
            return 0.8 + 1.6 * sin(PI * progress).pow(0.65)
        }

        repeat(segments + 1) { index ->
            val x = -16.0 + 32.0 * index / segments
            val y = centerY(index) - halfWidth(index)
            if (index == 0) body.moveTo(x, y) else body.lineTo(x, y)
        }
        for (index in segments downTo 0) {
            val x = -16.0 + 32.0 * index / segments
            body.lineTo(x, centerY(index) + halfWidth(index))
        }
        body.closePath()

        graphics.color = Color(244, 224, 184)
        graphics.fill(body)
        graphics.color = Color(94, 65, 43)
        graphics.stroke = BasicStroke(0.65f)
        graphics.draw(body)
    }
}
