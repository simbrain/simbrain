/**
 * Rasterizes OdorWorld entity types whose visuals are generated at runtime so the 2D world and 3D sensor share them.
 */
package org.simbrain.world.odorworld

import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

fun interface ProceduralEntityImageRenderer {
    fun paint(entity: OdorWorldEntity, frameIndex: Int, graphics: Graphics2D)
}

object ProceduralEntityImages {
    private data class ImageKey(val type: EntityType, val frame: Int, val direction: Int)

    private val renderers = mutableMapOf<EntityType, ProceduralEntityImageRenderer>()
    private val imageCache = ConcurrentHashMap<ImageKey, BufferedImage>()

    /**
     * Images are rasterized at this multiple of the entity type's native size, so they stay sharp when an entity is
     * scaled up or the view is zoomed in. Renderers still paint in native units.
     */
    private const val RESOLUTION = 4

    init {
        register(EntityType.Nematode, CElegansRenderer)
        register(EntityType.Pillbug, PillbugRenderer)
        register(EntityType.Isopod, GnathiaRenderer)
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
            BufferedImage(
                entity.entityType.width * RESOLUTION,
                entity.entityType.height * RESOLUTION,
                BufferedImage.TYPE_INT_ARGB
            ).apply {
                val g = createGraphics()
                try {
                    g.translate(width / 2.0, height / 2.0)
                    g.scale(RESOLUTION.toDouble(), RESOLUTION.toDouble())
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

/**
 * A pillbug seen from above, facing east: a domed, segmented body with eyes and antennae, and seven leg pairs that
 * step in a wave as the frames advance.
 */
private object PillbugRenderer : ProceduralEntityImageRenderer {
    private const val HALF_LENGTH = 7.0
    private const val HALF_WIDTH = 4.2
    private const val HEAD_X = 4.6
    private val shell = Color(128, 124, 120)
    private val shellLight = Color(162, 158, 152)
    private val shellDark = Color(104, 100, 98)
    private val plateLine = Color(84, 80, 78)
    private val outline = Color(58, 54, 52)

    /** Half width of the body at [x], a flattened oval that narrows slightly toward the tail. */
    private fun halfWidthAt(x: Double): Double {
        val t = (abs(x) / HALF_LENGTH).coerceAtMost(1.0)
        val taper = 0.88 + 0.12 * (x + HALF_LENGTH) / (2 * HALF_LENGTH)
        return HALF_WIDTH * taper * (1 - t.pow(2.6)).pow(1 / 2.6)
    }

    override fun paint(entity: OdorWorldEntity, frameIndex: Int, graphics: Graphics2D) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val phase = 2.0 * PI * frameIndex / entity.entityType.numFrames
        val thin = BasicStroke(0.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        graphics.color = outline
        graphics.stroke = BasicStroke(0.45f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val legCount = 7
        repeat(legCount) { i ->
            val x = HEAD_X - 1.4 - i * 1.45
            val swing = 0.45 * sin(phase + i * 0.9)
            for (side in listOf(-1.0, 1.0)) {
                val baseY = side * halfWidthAt(x) * 0.7
                val angle = side * (PI / 2 + 0.35 + swing)
                graphics.draw(Line2D.Double(x, baseY, x + 2.6 * cos(angle), baseY + 2.6 * sin(angle)))
            }
        }

        graphics.stroke = thin
        for (side in listOf(-1.0, 1.0)) {
            val antenna = Path2D.Double().apply {
                moveTo(HALF_LENGTH - 0.6, side * 1.1)
                quadTo(HALF_LENGTH + 0.9, side * 2.0, HALF_LENGTH + 1.7, side * 3.9)
            }
            graphics.draw(antenna)
        }

        val body = Path2D.Double()
        val steps = 40
        for (k in 0..steps) {
            val x = -HALF_LENGTH + 2 * HALF_LENGTH * k / steps
            if (k == 0) body.moveTo(x, -halfWidthAt(x)) else body.lineTo(x, -halfWidthAt(x))
        }
        for (k in steps downTo 0) {
            val x = -HALF_LENGTH + 2 * HALF_LENGTH * k / steps
            body.lineTo(x, halfWidthAt(x))
        }
        body.closePath()
        graphics.color = shell
        graphics.fill(body)

        // A lighter ridge down the middle suggests the domed back
        val clip = graphics.clip
        graphics.clip(body)
        graphics.color = shellLight
        graphics.fill(Ellipse2D.Double(-HALF_LENGTH, -HALF_WIDTH * 0.42, 2 * HALF_LENGTH, HALF_WIDTH * 0.84))

        graphics.color = shellDark
        graphics.fill(Rectangle2D.Double(HEAD_X, -HALF_WIDTH, HALF_LENGTH, 2 * HALF_WIDTH))

        graphics.color = plateLine
        graphics.stroke = thin
        val segmentXs = listOf(HEAD_X) + (1 until legCount).map { HEAD_X - it * 1.45 } + listOf(-4.9, -5.7)
        for (x in segmentXs) {
            val w = halfWidthAt(x) + 0.5
            graphics.draw(Path2D.Double().apply {
                moveTo(x, -w)
                quadTo(x - 0.9, 0.0, x, w)
            })
        }
        graphics.clip = clip

        graphics.stroke = BasicStroke(0.5f)
        graphics.draw(body)

        graphics.color = Color(30, 28, 28)
        for (side in listOf(-1.0, 1.0)) {
            graphics.fill(Ellipse2D.Double(HEAD_X + 0.9, side * 2.0 - 0.45, 0.9, 0.9))
        }
    }
}

/**
 * A juvenile (praniza) of the parasitic isopod Gnathia marleyi seen from above, facing east: a head with large
 * side eyes, a narrow neck, an elongated translucent bulb swollen with a blood meal, a tapering tail ending in a
 * small fan, and five pairs of jointed legs that alternate as the frames advance.
 */
private object GnathiaRenderer : ProceduralEntityImageRenderer {
    private val body = Color(230, 219, 203)
    private val gut = Color(160, 78, 62, 95)
    private val leg = Color(120, 100, 88)
    private val outline = Color(78, 64, 58)
    private val eye = Color(40, 32, 32)

    override fun paint(entity: OdorWorldEntity, frameIndex: Int, graphics: Graphics2D) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val phase = 2.0 * PI * frameIndex / entity.entityType.numFrames
        val bodyStroke = BasicStroke(0.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val fine = BasicStroke(0.28f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        // Leg base x, base half width, and how far the leg angles forward (-) or back (+)
        val legs = listOf(Triple(3.0, 1.3, -0.75), Triple(2.0, 1.5, -0.4), Triple(0.6, 2.7, 0.0),
            Triple(-1.4, 2.9, 0.35), Triple(-3.4, 2.4, 0.7))
        graphics.color = leg
        graphics.stroke = BasicStroke(0.38f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        legs.forEachIndexed { i, (x, halfWidth, tilt) ->
            for (side in listOf(-1.0, 1.0)) {
                val swing = 0.28 * sin(phase + i * PI + if (side > 0) PI else 0.0)
                val upper = side * (PI / 2 + tilt + swing)
                val kneeX = x + 1.6 * cos(upper)
                val kneeY = side * halfWidth + 1.6 * sin(upper)
                val lower = upper + side * (0.55 + 0.5 * tilt)
                graphics.draw(Path2D.Double().apply {
                    moveTo(x, side * (halfWidth - 0.4))
                    lineTo(kneeX, kneeY)
                    lineTo(kneeX + 1.5 * cos(lower), kneeY + 1.5 * sin(lower))
                })
            }
        }

        graphics.color = outline
        graphics.stroke = fine
        for (side in listOf(-1.0, 1.0)) {
            graphics.draw(Path2D.Double().apply {
                moveTo(7.4, side * 0.8)
                quadTo(8.4, side * 1.0, 8.9, side * 2.4)
            })
            graphics.draw(Path2D.Double().apply {
                moveTo(7.6, side * 0.3)
                quadTo(8.5, side * 0.3, 8.8, side * 0.9)
            })
        }

        val tail = Path2D.Double().apply {
            moveTo(-4.4, -1.4)
            lineTo(-6.8, -0.8)
            lineTo(-8.0, -1.9)
            lineTo(-8.8, -0.6)
            lineTo(-8.4, 0.0)
            lineTo(-8.8, 0.6)
            lineTo(-8.0, 1.9)
            lineTo(-6.8, 0.8)
            lineTo(-4.4, 1.4)
            closePath()
        }
        val neck = Path2D.Double().apply {
            moveTo(1.2, -2.0)
            lineTo(4.6, -1.4)
            lineTo(4.6, 1.4)
            lineTo(1.2, 2.0)
            closePath()
        }
        val head = Path2D.Double().apply {
            moveTo(4.4, -2.1)
            lineTo(6.6, -1.7)
            quadTo(7.9, -1.4, 7.9, 0.0)
            quadTo(7.9, 1.4, 6.6, 1.7)
            lineTo(4.4, 2.1)
            closePath()
        }
        val bulb = Ellipse2D.Double(-5.4, -3.1, 8.0, 6.2)

        graphics.stroke = bodyStroke
        for (part in listOf(tail, neck, bulb, head)) {
            graphics.color = body
            graphics.fill(part)
            graphics.color = outline
            graphics.draw(part)
        }

        graphics.color = gut
        graphics.fill(Ellipse2D.Double(-4.4, -1.9, 6.0, 3.8))

        graphics.color = outline
        graphics.stroke = fine
        for (x in listOf(-5.3, -6.0)) {
            graphics.draw(Line2D.Double(x, -1.15, x, 1.15))
        }
        graphics.draw(Line2D.Double(-6.8, -0.8, -6.8, 0.8))
        graphics.draw(Line2D.Double(2.9, -1.7, 2.9, 1.7))

        graphics.color = eye
        for (side in listOf(-1.0, 1.0)) {
            graphics.fill(Ellipse2D.Double(4.9, side * 1.85 - 0.65, 1.7, 1.3))
        }
    }
}
