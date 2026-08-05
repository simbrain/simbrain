/**
 * Continuous value-to-color maps for charts that encode a third variable as color, plus the
 * [PaintScale] adapter that lets JFreeChart use them. Kept beside ChartTheme.kt because, like the
 * series palette there, these are chart-facing color decisions rather than general image utilities.
 */
package org.simbrain.plot

import org.jfree.chart.renderer.PaintScale
import org.simbrain.util.NetworkTheme
import java.awt.Color
import java.awt.Paint

/**
 * Named color maps, each mapping a fraction in 0..1 to a color.
 *
 * [JET] and [HOT] are the MATLAB ramps used throughout the C. elegans thermotaxis literature and are
 * fixed, so figures reproduced from a paper look like the paper in either light or dark mode.
 * [COOL_TO_HOT] instead follows the current Simbrain network palette, matching how neuron activations
 * are colored on the network canvas.
 */
enum class ChartColorMap(private val label: String, private val diverging: Boolean) {

    JET("Jet", false) {
        override fun anchors() = jetAnchors
    },

    HOT("Hot", false) {
        override fun anchors() = hotAnchors
    },

    GRAYSCALE("Grayscale", false) {
        override fun anchors() = listOf(0.0 to Color.BLACK, 1.0 to Color.WHITE)
    },

    COOL_TO_HOT("Cool to hot", true) {
        override fun anchors() = NetworkTheme.current.let {
            listOf(0.0 to it.coolNode, 0.5 to it.neutralMidpoint, 1.0 to it.hotNode)
        }
    };

    /** Stops the ramp interpolates between, resolved on each call so theme-derived maps stay live. */
    protected abstract fun anchors(): List<Pair<Double, Color>>

    /** True when the map has a distinguished midpoint, so a symmetric range around it reads correctly. */
    fun isDiverging() = diverging

    override fun toString() = label

    /** Color for [fraction] of the way along the ramp; values outside 0..1 clamp to the endpoints. */
    fun color(fraction: Double): Color {
        val stops = anchors()
        val clamped = fraction.coerceIn(0.0, 1.0)
        val upper = stops.indexOfFirst { it.first >= clamped }.coerceAtLeast(1)
        val (lowPoint, lowColor) = stops[upper - 1]
        val (highPoint, highColor) = stops[upper]
        val span = highPoint - lowPoint
        val t = if (span <= 0.0) 0.0 else (clamped - lowPoint) / span
        return Color(
            lerp(lowColor.red, highColor.red, t),
            lerp(lowColor.green, highColor.green, t),
            lerp(lowColor.blue, highColor.blue, t)
        )
    }

    private fun lerp(from: Int, to: Int, t: Double) = (from + (to - from) * t).toInt().coerceIn(0, 255)
}

private val jetAnchors = listOf(
    0.0 to Color(0, 0, 128), 0.125 to Color(0, 0, 255), 0.375 to Color(0, 255, 255),
    0.625 to Color(255, 255, 0), 0.875 to Color(255, 0, 0), 1.0 to Color(128, 0, 0)
)

private val hotAnchors = listOf(
    0.0 to Color(0, 0, 0), 0.375 to Color(255, 0, 0),
    0.75 to Color(255, 255, 0), 1.0 to Color(255, 255, 255)
)

/**
 * Bridges a [ChartColorMap] to JFreeChart's block renderer and colorbar legend.
 *
 * Colors are resolved inside [getPaint] rather than cached, so a theme-derived map follows a live
 * light/dark switch without the scale having to be rebuilt. That matters because
 * [applySimbrainChartTheme] is re-run wholesale on a theme change and does not know about paint scales.
 */
class ChartColorMapPaintScale(
    private val lower: Double,
    private val upper: Double,
    private val colorMap: () -> ChartColorMap
) : PaintScale {

    override fun getLowerBound() = lower

    override fun getUpperBound() = upper

    override fun getPaint(value: Double): Paint {
        val span = upper - lower
        val fraction = if (span <= 0.0) 0.0 else (value - lower) / span
        return colorMap().color(fraction)
    }
}
