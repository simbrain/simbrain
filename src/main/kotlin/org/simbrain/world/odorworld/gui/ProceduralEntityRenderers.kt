/**
 * GUI renderers for OdorWorld entity types whose visual appearance is generated at paint time rather than loaded
 * from image resources. Register additional type-specific renderers here as procedural entity visuals are added.
 */
package org.simbrain.world.odorworld.gui

import org.piccolo2d.util.PBounds
import org.simbrain.util.toRadian
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

fun interface ProceduralEntityRenderer {
    fun paint(entity: OdorWorldEntity, graphics: Graphics2D, bounds: PBounds)
}

object ProceduralEntityRenderers {
    private val renderers = mutableMapOf<EntityType, ProceduralEntityRenderer>()

    init {
        register(EntityType.Nematode, CElegansRenderer)
    }

    fun register(entityType: EntityType, renderer: ProceduralEntityRenderer) {
        renderers[entityType] = renderer
    }

    fun paintIfRegistered(entity: OdorWorldEntity, graphics: Graphics2D, bounds: PBounds): Boolean {
        val renderer = renderers[entity.entityType] ?: return false
        renderer.paint(entity, graphics, bounds)
        return true
    }
}

private object CElegansRenderer : ProceduralEntityRenderer {
    override fun paint(entity: OdorWorldEntity, graphics: Graphics2D, bounds: PBounds) {
        val g = graphics.create() as Graphics2D
        try {
            g.translate(bounds.centerX, bounds.centerY)
            g.rotate(-entity.heading.toRadian())
            g.scale(bounds.width / 48.0, bounds.height / 48.0)

            val phase = 2.0 * PI * entity.animationFrame / 4.0
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

            g.color = Color(244, 224, 184)
            g.fill(body)
            g.color = Color(94, 65, 43)
            g.stroke = BasicStroke(0.65f)
            g.draw(body)
        } finally {
            g.dispose()
        }
    }
}
