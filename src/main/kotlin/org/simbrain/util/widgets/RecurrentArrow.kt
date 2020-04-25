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
 * Represents a self directed connection from a neuron group to itself via a weight matrix.
 * Could potentially generalize this to be used by synapse groups too.
 *
 * Centered by default on 0,0.  Can be offset by whoever calls this.
 */
class RecurrentArrow : PNode() {

    private val radius = 100.0
    private val startDeg = 10.0
    private val endDeg = 300.0

    /**
     * The triangle at the tip of the arrow. This triangle is constructed only once, and during [update] this
     * triangle will be placed onto the correct location
     */
    private val arrowTip = listOf(point(0, 0), point(0.5, -0.866025), point(-0.5, -0.866025))
            .map { it * 40.0 }
            .toPolygon()
            .let { polygon -> PArea(polygon, null) }
            .apply { paint = Color.ORANGE }

    /**
     * Pass in where you want to center the recurrent arrow, and then any additional action to perform
     */
    fun update(location: Point2D, callback: (Point2D) -> Unit) {
        removeAllChildren()
        val (x, y) = location + point(-radius, 0.0)
        PPath.createArc(x - radius, y - radius, 2 * radius, 2 * radius, startDeg, endDeg, Arc2D.OPEN)
                .apply {
                    paint = null
                    stroke = BasicStroke(20.0f)
                    strokePaint = Color.ORANGE
                    transparency = 0.5f
                }.also { addChild(it) }
        val arrowTipRadian = (endDeg + 30).toRadian()
        arrowTip.getTransformReference(true).apply {
            setToTranslation(x + cos(arrowTipRadian) * (radius + 10), y - sin(arrowTipRadian) * (radius + 10))
            rotate(arrowTipRadian + 265.toRadian())
        }
        addChild(arrowTip)
        callback(location - point(2*radius, 0.0))
    }
}