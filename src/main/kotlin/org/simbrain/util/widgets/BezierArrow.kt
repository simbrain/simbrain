package org.simbrain.util.widgets

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PArea
import org.piccolo2d.nodes.PPath
import org.simbrain.util.*
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.CubicCurve2D
import java.awt.geom.Line2D

/**
 * Represents a Bezier curved with a single arrow at its end. Where it is located on its
 * source can be set, but it's target location is automatically determined.
 *
 * Terminology: a source and target [LocatableModel] are connected by a directed bezier
 * curve with a tail and tip.
 *
 * @param thickness thickness of the arrow in pixels
 * @param color color the arrow
 * @param sourceEdgePercentage where on the source edge the tail of the arrow is located. Starting is 0 and end is 1.
 *
 * @author Zoë Tosi
 * @author Leo Yulin Li
 */
class BezierArrow(
        val thickness: Float = 20.0f,
        val color: Color = Color.GREEN,
        var sourceEdgePercentage: Double = 0.5
) : PNode() {

    /**
     * The triangle at the tip of the arrow. This triangle is constructed only once and during [update] the this
     * triangle will be placed onto the correct location
     */
    private val arrowTip = listOf(point(0, 0), point(0.5, -0.866025), point(-0.5, -0.866025))
            .map { it * (thickness * 2.0) }.toPolygon()
            .let { polygon -> PArea(polygon, null) }
            .apply { paint = color }

    /**
     * Update the shape of the arrow base on the outlines of source and target.
     *
     * @return the updated curve model
     */
    fun update(sourceOutlines: RectangleOutlines, targetOutlines: RectangleOutlines): CubicCurve2D? {

        // 0. clear old arrow
        removeAllChildren()

        // 1. for each source and target, find a side of the outline to let the arrow connect
        val (sourceSide, targetSide) = (sourceOutlines.toList() cartesianProduct targetOutlines.toList()).filter { (source, target) ->
            // make sure the curve does not bent backward
            val line = target.headOffset - source.tailOffset
            line dot source.normal > 0 && line dot target.normal < 0
        }.minBy { (source, target) -> source.midPoint distanceSqTo target.midPoint } ?: return null

        // 2. compute the vector from source to target
        val deltaVector = targetSide.headOffset - sourceSide.tailOffset

        // 3. put the arrow tip at the right location and orient it at the right angle
        arrowTip.getTransformReference(true).apply {
            setToTranslation(targetSide.tipOffset.x, targetSide.tipOffset.y)
            val theta = targetSide.normalTheta
            rotate(theta)
        }

        // 4. compute the curve
        val curveModel = cubicBezier(
                sourceSide.tailOffset,
                sourceSide.tailOffset + deltaVector.abs * sourceSide.unitNormal * 0.5,
                targetSide.headOffset + deltaVector.abs * targetSide.unitNormal * 0.5,
                targetSide.headOffset
        )

        val curveView = PPath.Double(curveModel, BasicStroke(thickness)).apply {
            paint = null
            strokePaint = color
            transparency = 0.5f
        }

        // 5. add shape to node
        addChild(arrowTip)
        addChild(curveView)

        // 6. return the new location of this arrow
        return curveModel

    }

    /**
     * Given a side of a rectangle bound, find the location of where an arrow tail would go.
     */
    private val Line2D.tailOffset
        get() = p(sourceEdgePercentage) + unitNormal * (thickness * 2.5)

    /**
     * Given a side of a rectangle bound, find the location of where an arrow head would go.
     */
    private val Line2D.headOffset
        get() = p(1 - sourceEdgePercentage) + unitNormal * (thickness * 2.5)

    /**
     * Given a side of a rectangle bound, find the location of where the tip of an arrow would go.
     */
    private val Line2D.tipOffset
        get() = p(1 - sourceEdgePercentage) + unitNormal * (thickness * 1.0)
}