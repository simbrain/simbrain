package org.simbrain.util.widgets

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PArea
import org.piccolo2d.nodes.PPath
import org.simbrain.util.*
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.Arc2D
import java.awt.geom.Point2D
import kotlin.math.cos
import kotlin.math.sin

/**
 * Represents a self-directed connection from a group to itself.
 *
 * Centered by default on 0,0.  Can be offset by whoever calls this.
 */
class RecurrentArrow(color: Color) : PNode() {

    private val radius = 100.0
    private val startDeg = 20.0
    private val endDeg = 320.0

    /**
     * The triangle at the tip of the arrow. This triangle is constructed only once, and during [layout] this
     * triangle will be placed onto the correct location
     */
    private val arrowTip = listOf(point(-1, 0), point(1, 0), point(0, -1))
            .map { it * 30.0 }
            .toPolygon()
            .let { polygon -> PArea(polygon, null) }
            .apply {
                paint = color
                val arrowTipRadian = endDeg.toRadian()
                rotate(-arrowTipRadian)
                offset(cos(arrowTipRadian) * radius, -sin(arrowTipRadian) * radius)
                transparency = 0.5f
                this@RecurrentArrow.addChild(this)
            }

    private val arc = PPath.createArc(-radius, -radius, 2 * radius, 2 * radius, startDeg,
        endDeg - startDeg, Arc2D.OPEN)
        .apply {
            paint = null
            stroke = BasicStroke(20.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
            strokePaint = color
            transparency = 0.5f
        }.also { addChild(it) }

    /**
     * Pass in where you want to center the recurrent arrow, and then any additional action to perform
     */
    fun layout(location: Point2D, callback: (Point2D) -> Unit) {
        globalTranslation = location - point(radius, 0.0)
        callback(location - point(2*radius, 0.0))
    }
}