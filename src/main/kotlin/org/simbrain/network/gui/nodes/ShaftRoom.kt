/**
 * Measures the gap between two layers from the arrow joining them, so that what a connector draws on the arrow,
 * such as a weight matrix image, a kernel or a kernel grid, can be sized to that gap.
 */
package org.simbrain.network.gui.nodes

import org.simbrain.util.p
import java.awt.geom.CubicCurve2D
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Room for a connector's drawing on its arrow, not counting what accompanies the drawing or the margin around it.
 *
 * @param width room from left to right, infinite when neither layer limits the drawing in that direction
 * @param height room from top to bottom, infinite when neither layer limits the drawing in that direction
 * @param centerX where the center of the drawing goes
 * @param centerY where the center of the drawing goes
 */
internal class ShaftRoom(val width: Double, val height: Double, val centerX: Double, val centerY: Double) {

    /** Side of the largest square drawing that fits, kept between [min], below which it overruns the room, and [max]. */
    fun squareSide(min: Double, max: Double) = min(width, height).coerceIn(min, max)

    companion object {

        /** Space kept clear between a drawing, with what accompanies it, and whatever limits it. */
        const val MARGIN = 6.0

        /** Change in a fitted size that is too small to be worth laying the drawing out again. */
        const val RESIZE_THRESHOLD = 0.25
    }
}

/**
 * Find the room a connector's drawing has on the [shaft] of its arrow, which runs from an edge of the source layer
 * to the base of the arrowhead and leaves both at a right angle.
 *
 * The drawing is kept on the arrow's side of the source layer's edge and of the base of the arrowhead, which keeps
 * it clear of both layers however they are arranged. Between layers that face each other, stacked or side by side,
 * that limits the drawing in one direction only, and it is centered in the gap along with what accompanies it.
 * Where the arrow turns a corner each end limits a different direction, and the drawing stays centered on the
 * shaft.
 *
 * @param position where on the shaft the drawing goes, from 0 at the source layer to 1 at the base of the arrowhead
 * @param shared whether a connector running the other way draws in the same gap, in which case this one keeps to
 * the half nearer its source
 * @param above height of what accompanies the drawing above it, such as a label box
 * @param below height of what accompanies the drawing below it, such as a caption
 * @param beside width of what accompanies the drawing on each side
 * @param fill fraction of the gap that the drawing may take up together with what accompanies it, which leaves the
 * rest of the arrow showing on either side of it
 * @param comfortable size the drawing may reach by filling the gap outright, so that [fill] only holds back a
 * drawing larger than this
 */
internal fun shaftRoom(
    shaft: CubicCurve2D,
    position: Double = 0.5,
    shared: Boolean = false,
    above: Double = 0.0,
    below: Double = 0.0,
    beside: Double = 0.0,
    fill: Double = 1.0,
    comfortable: Double = 0.0
): ShaftRoom {
    val center = shaft.p(position)
    // Direction the shaft leaves the source layer in, and the direction it would leave the target layer in
    val sourceOutX = shaft.ctrlX1 - shaft.x1
    val sourceOutY = shaft.ctrlY1 - shaft.y1
    val targetOutX = shaft.ctrlX2 - shaft.x2
    val targetOutY = shaft.ctrlY2 - shaft.y2
    if (sourceOutX == 0.0 && sourceOutY == 0.0) {
        // The layers are close enough that the shaft has no length
        return ShaftRoom(0.0, 0.0, center.x, center.y)
    }

    var left = Double.NEGATIVE_INFINITY
    var right = Double.POSITIVE_INFINITY
    var top = Double.NEGATIVE_INFINITY
    var bottom = Double.POSITIVE_INFINITY

    /** Keep the drawing on the side of the horizontal or vertical line through ([x], [y]) that [outX], [outY] points to. */
    fun limit(x: Double, y: Double, outX: Double, outY: Double) {
        if (abs(outX) > abs(outY)) {
            if (outX > 0) left = max(left, x) else right = min(right, x)
        } else {
            if (outY > 0) top = max(top, y) else bottom = min(bottom, y)
        }
    }
    limit(shaft.x1, shaft.y1, sourceOutX, sourceOutY)
    limit(shaft.x2, shaft.y2, targetOutX, targetOutY)
    if (shared) limit((shaft.x1 + shaft.x2) / 2, (shaft.y1 + shaft.y2) / 2, -sourceOutX, -sourceOutY)

    /**
     * Room for the drawing in a [span] of which what accompanies it has [taken] some and [margin] is kept clear.
     * The drawing has the [fill] fraction of the span to share with what accompanies it, or all of the span while
     * that leaves it no larger than [upTo].
     */
    fun roomIn(span: Double, taken: Double, margin: Double, upTo: Double): Double {
        val whole = span - taken - margin
        return max(min(whole, upTo), min(span * fill - taken, whole))
    }

    /**
     * Room and center for the drawing between [low] and [high], with [before] and [after] taken up on either side
     * of it. A drawing limited on one side only stays centered on [shaftCenter].
     */
    fun fit(low: Double, high: Double, shaftCenter: Double, before: Double, after: Double): Pair<Double, Double> =
        when {
            low.isFinite() && high.isFinite() ->
                roomIn(high - low, before + after, 2 * ShaftRoom.MARGIN, comfortable) to
                        (low + high + before - after) / 2
            low.isFinite() ->
                2 * roomIn(shaftCenter - low, before, ShaftRoom.MARGIN, comfortable / 2) to shaftCenter
            high.isFinite() ->
                2 * roomIn(high - shaftCenter, after, ShaftRoom.MARGIN, comfortable / 2) to shaftCenter
            else -> Double.POSITIVE_INFINITY to shaftCenter
        }
    val (width, centerX) = fit(left, right, center.x, beside, beside)
    val (height, centerY) = fit(top, bottom, center.y, above, below)
    return ShaftRoom(width, height, centerX, centerY)
}
