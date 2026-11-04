/**
 * Background layer for a projection plot that shades each part of the visible plane by the class a classifier
 * assigns to it, so its decision regions and boundaries can be seen behind the data points. Only meaningful when the
 * plot axes are the classifier's two raw inputs. Used by `createClassifierProjectionPlot` in `SimbrainDesktopUtils`.
 *
 * "Annotation" is JFreeChart's term for anything drawn on a chart besides its data series, such as text, arrows, or
 * images. JFreeChart calls an annotation's [DecisionRegionAnnotation.draw] each time it paints the chart, passing
 * the plot area and axes so it can convert between data coordinates and screen pixels. Added to a renderer with
 * `renderer.addAnnotation(annotation, Layer.BACKGROUND)`, it is painted before the data points, so the points stay
 * on top.
 *
 * The shading is an image of a grid sampled over the current axis ranges: each cell's center is classified and the
 * cell is filled with that class's color. Classifying thousands of cells can be slow, so the grid is computed off
 * the Swing thread whenever the model, the visible range, or the plot size changes. Until a new grid is ready, the
 * previous image is drawn at the data coordinates it was computed for, so zooming and panning stay smooth.
 */
package org.simbrain.plot.projection

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.jfree.chart.annotations.AbstractXYAnnotation
import org.jfree.chart.axis.ValueAxis
import org.jfree.chart.plot.PlotRenderingInfo
import org.jfree.chart.plot.XYPlot
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage

/**
 * @param scope where grid computations run; cancelled jobs are replaced when a newer grid is needed
 * @param modelKey identifies the current model; regions are recomputed when it changes and hidden when it is null
 * @param isActive whether the regions should be drawn, e.g. only when the plot shows raw input coordinates
 * @param classAt class index for a point in the plane, or null to leave it unshaded
 * @param colors color for each class index
 */
class DecisionRegionAnnotation(
    private val scope: CoroutineScope,
    private val modelKey: () -> Any?,
    private val isActive: () -> Boolean = { true },
    private val colors: List<Color>,
    private val classAt: (DoubleArray) -> Int?,
) : AbstractXYAnnotation() {

    /**
     * Opacity of the shading, so the data points drawn above remain the most prominent marks.
     */
    var alpha = 0.3

    /**
     * Upper bound on grid cells per side. The grid otherwise uses one cell per [pixelsPerCell] screen pixels.
     */
    var maxCells = 200

    var pixelsPerCell = 2

    /**
     * Everything a grid image depends on. A new grid is computed when the key for the current paint differs from the
     * key of the image on hand.
     */
    private data class GridKey(
        val model: Any,
        val xLow: Double,
        val xHigh: Double,
        val yLow: Double,
        val yHigh: Double,
        val cols: Int,
        val rows: Int
    )

    private class Regions(val key: GridKey, val image: BufferedImage)

    /**
     * The most recently completed grid image, drawn on every paint until a newer one replaces it.
     */
    @Volatile
    private var regions: Regions? = null

    /**
     * The grid currently being computed, so repeated paints do not start the same computation again.
     */
    private var pendingKey: GridKey? = null

    /**
     * A grid whose computation threw, so repaints do not retry it until the model or range changes.
     */
    private var failedKey: GridKey? = null

    private var job: Job? = null

    /**
     * Start a new grid computation if the model, axis ranges, or plot size changed, then draw the latest completed
     * grid image stretched over its data coordinates. Called by JFreeChart on the Swing thread.
     */
    override fun draw(
        g2: Graphics2D,
        plot: XYPlot,
        dataArea: Rectangle2D,
        domainAxis: ValueAxis,
        rangeAxis: ValueAxis,
        rendererIndex: Int,
        info: PlotRenderingInfo?
    ) {
        if (!isActive()) return
        val model = modelKey() ?: return
        val cols = (dataArea.width / pixelsPerCell).toInt().coerceIn(1, maxCells)
        val rows = (dataArea.height / pixelsPerCell).toInt().coerceIn(1, maxCells)
        val key = GridKey(
            model,
            domainAxis.lowerBound, domainAxis.upperBound,
            rangeAxis.lowerBound, rangeAxis.upperBound,
            cols, rows
        )
        if (regions?.key != key && pendingKey != key && failedKey != key) {
            compute(key)
        }
        val current = regions ?: return
        if (current.key.model !== model && pendingKey != null) return
        val edge = plot.domainAxisEdge
        val rangeEdge = plot.rangeAxisEdge
        val x0 = domainAxis.valueToJava2D(current.key.xLow, dataArea, edge)
        val x1 = domainAxis.valueToJava2D(current.key.xHigh, dataArea, edge)
        val y0 = rangeAxis.valueToJava2D(current.key.yHigh, dataArea, rangeEdge)
        val y1 = rangeAxis.valueToJava2D(current.key.yLow, dataArea, rangeEdge)
        val oldClip = g2.clip
        val oldInterpolation = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION)
        g2.clip(dataArea)
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        g2.drawImage(current.image, x0.toInt(), y0.toInt(), (x1 - x0).toInt(), (y1 - y0).toInt(), null)
        oldInterpolation?.let { g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, it) }
        g2.clip = oldClip
    }

    /**
     * Classify the center of every cell in [key]'s grid off the Swing thread and, if no newer grid was requested
     * meanwhile, store the resulting image and ask the chart to repaint.
     */
    private fun compute(key: GridKey) {
        pendingKey = key
        job?.cancel()
        val alphaBits = (alpha.coerceIn(0.0, 1.0) * 255).toInt() shl 24
        val argb = colors.map { (it.rgb and 0xFFFFFF) or alphaBits }
        job = scope.launch(Dispatchers.Default) {
            val image = BufferedImage(key.cols, key.rows, BufferedImage.TYPE_INT_ARGB)
            val dx = (key.xHigh - key.xLow) / key.cols
            val dy = (key.yHigh - key.yLow) / key.rows
            val point = DoubleArray(2)
            try {
                for (row in 0 until key.rows) {
                    ensureActive()
                    point[1] = key.yHigh - (row + 0.5) * dy
                    for (col in 0 until key.cols) {
                        point[0] = key.xLow + (col + 0.5) * dx
                        val index = classAt(point)
                        if (index != null && index in argb.indices) {
                            image.setRGB(col, row, argb[index])
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Swing) {
                    if (pendingKey == key) pendingKey = null
                    failedKey = key
                }
                return@launch
            }
            withContext(Dispatchers.Swing) {
                if (pendingKey == key) {
                    regions = Regions(key, image)
                    pendingKey = null
                    fireAnnotationChanged()
                }
            }
        }
    }

    /**
     * Ask the plot to redraw, for example after the model was retrained, so the regions are recomputed.
     */
    fun refresh() {
        scope.launch(Dispatchers.Swing) { fireAnnotationChanged() }
    }

    override fun equals(other: Any?) = this === other

    override fun hashCode() = System.identityHashCode(this)

}
