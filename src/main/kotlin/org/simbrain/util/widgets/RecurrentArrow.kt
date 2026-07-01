package org.simbrain.util.widgets

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.geom.Area
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
    private val startDeg = 10.0
    private val endDeg = 320.0

    /**
     * The stroked arc and the tip triangle unioned into a single translucent shape, so their
     * overlap does not double-blend into seams.
     */
    private val arrowView = run {
        val arc = Arc2D.Double(-radius, -radius, 2 * radius, 2 * radius, startDeg, endDeg - startDeg, Arc2D.OPEN)
        val arrowTipRadian = endDeg.toRadian()
        val tipTransform = AffineTransform().apply {
            translate(cos(arrowTipRadian) * radius, -sin(arrowTipRadian) * radius)
            rotate(-arrowTipRadian)
        }
        val arrowTipShape = listOf(point(-1, 0), point(1, 0), point(0, -1))
                .map { it * 30.0 }
                .toPolygon()
        val arrowShape = Area(BasicStroke(20.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER).createStrokedShape(arc)).apply {
            add(Area(tipTransform.createTransformedShape(arrowTipShape)))
        }
        PPath.Double(arrowShape, null).apply {
            paint = color
            transparency = 0.5f
            this@RecurrentArrow.addChild(this)
        }
    }

    /** Recolor the arrow to the given color. */
    fun updateColor(color: Color) {
        arrowView.paint = color
    }

    /**
     * Update the arrow color from the current [NetworkPreferences.connectorArrowColor][org.simbrain.network.gui.dialogs.NetworkPreferences.connectorArrowColor].
     */
    fun updateColorFromPreferences() {
        updateColor(NetworkPreferences.connectorArrowColor)
    }

    /**
     * Pass in where you want to center the recurrent arrow, and then any additional action to perform
     */
    fun layout(location: Point2D, callback: (Point2D) -> Unit) {
        globalTranslation = location - point(radius, 0.0)
        callback(location - point(2*radius, 0.0))
    }
}