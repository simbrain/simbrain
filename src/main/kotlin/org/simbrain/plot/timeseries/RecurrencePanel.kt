/**
 * Recurrence plot for one [TimeSeriesModel.TimeSeries]: the pairwise distances between the series'
 * states over its recent window, drawn as a square time-by-time matrix. In threshold mode a cell is
 * filled, in the series' line color, when two states are within [TimeSeriesModel.recurrenceThreshold]
 * of each other; in spectrum mode every cell is colored by its distance through
 * [TimeSeriesModel.recurrenceColorMap], with a colorbar.
 *
 * The matrix is recomputed from the series' data on demand rather than accumulated: dataset changes
 * only mark the panel dirty, and the O(n^2) rebuild runs only while the panel has visible area — a
 * showing flag alone is not enough, because a split pane collapsed to zero height still counts as
 * showing — and at most once per [REFRESH_MIN_INTERVAL_MS]. The throttle matters beyond this panel:
 * coupled values reach the time series through invokeAndWait, so recurrence work on the event thread
 * directly slows the whole workspace's iteration rate.
 */
package org.simbrain.plot.timeseries

import kotlinx.coroutines.Job
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.AxisLocation
import org.jfree.chart.axis.AxisSpace
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.PaintScale
import org.jfree.chart.renderer.xy.XYBlockRenderer
import org.jfree.chart.title.PaintScaleLegend
import org.jfree.chart.ui.RectangleEdge
import org.jfree.chart.ui.RectangleInsets
import org.jfree.data.Range
import org.jfree.data.general.DatasetChangeListener
import org.jfree.data.xy.AbstractXYZDataset
import org.simbrain.plot.*
import org.simbrain.util.MinIntervalGate
import org.simbrain.util.swingDispatcher
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Paint
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.HierarchyEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.sqrt

class RecurrencePanel(
    private val model: TimeSeriesModel,
    val timeSeries: TimeSeriesModel.TimeSeries
) : JPanel() {

    private val renderer = XYBlockRenderer()

    private val colorBarAxis = NumberAxis("Distance")

    private val colorBarLegend = PaintScaleLegend(ChartColorMapPaintScale(0.0, 1.0) { model.recurrenceColorMap }, colorBarAxis).apply {
        axisLocation = AxisLocation.BOTTOM_OR_RIGHT
        position = RectangleEdge.RIGHT
        margin = RectangleInsets(8.0, 4.0, 24.0, 4.0)
    }

    private val plot: XYPlot

    private val chart: JFreeChart

    val chartPanel: ChartPanel = ChartPanel(null)

    /** The rebuild throttle bounds matrix recomputes; this additionally self-clocks the paints. */
    private val repainter = AdaptiveChartRepainter(chartPanel)

    /** Whether an event-thread rebuild is already queued, so bursts of data coalesce into one. */
    private val refreshQueued = AtomicBoolean(false)

    /** Whether data or settings changed since the last rebuild; consumed when the panel next renders. */
    private var dirty = false

    /** Rate limit on matrix rebuilds; the action re-checks the guards because the tail is deferred. */
    private val refreshGate = MinIntervalGate(REFRESH_MIN_INTERVAL_MS) { if (dirty && hasVisibleArea()) refresh() }

    /** Palette slot of this panel's series, resolved per refresh rather than per painted cell. */
    private var paletteIndex = 0

    /**
     * When non-null, the time axis follows the range this yields — the line chart's current domain —
     * rather than the matrix's own extent, so time t sits at the same x pixel in both plots. A
     * provider rather than a pushed value because the line chart's auto-range moves without firing
     * axis-change events (JFreeChart adjusts it with notify off during rendering), so the current
     * range must be pulled on every [refresh]. The provider also carries the range-axis space both
     * charts reserve, sized to their actual tick labels by the desktop component; the colorbar moves
     * to the bottom edge while aligned so it does not narrow the plot area horizontally.
     */
    var domainAlignmentProvider: (() -> DomainAlignment)? = null
        set(value) {
            field = value
            if (value != null) {
                colorBarLegend.position = RectangleEdge.BOTTOM
                applyAlignedDomain()
            } else {
                plot.fixedRangeAxisSpace = null
                colorBarLegend.position = RectangleEdge.RIGHT
                scheduleRefresh()
            }
        }

    private val datasetListener = DatasetChangeListener { scheduleRefresh() }

    private val propertyChangedSubscription: Job

    init {
        preferredSize = PREFERRED_SIZE
        layout = BorderLayout()
        add(chartPanel, BorderLayout.CENTER)

        val domainAxis = NumberAxis("Time").apply { standardTickUnits = NumberAxis.createIntegerTickUnits() }
        val rangeAxis = NumberAxis("Time").apply { standardTickUnits = NumberAxis.createIntegerTickUnits() }
        plot = XYPlot(RecurrenceDataset(DoubleArray(0), emptyArray()), domainAxis, rangeAxis, renderer)
        chart = JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false)
        chartPanel.chart = chart
        chart.applySimbrainChartTheme()
        repainter.install()

        model.dataset.addChangeListener(datasetListener)
        propertyChangedSubscription = model.events.propertyChanged.on(swingDispatcher) { scheduleRefresh() }
        addHierarchyListener { e ->
            if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) refreshIfDue()
        }
        // Regaining area after a split-pane collapse is a resize, not a showing change
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                refreshIfDue()
            }
        })

        refresh()
    }

    /** Detach from the model; call when the panel is permanently removed. */
    fun dispose() {
        model.dataset.removeChangeListener(datasetListener)
        propertyChangedSubscription.cancel()
        refreshGate.stop()
        repainter.uninstall()
    }

    /** Snap the time axis and axis space to the aligned state right now; no-op when alignment is off. */
    fun applyAlignedDomain() {
        val alignment = domainAlignmentProvider?.invoke() ?: return
        if (plot.fixedRangeAxisSpace != alignment.rangeAxisSpace) plot.fixedRangeAxisSpace = alignment.rangeAxisSpace
        if (plot.domainAxis.range != alignment.range) plot.domainAxis.setRange(alignment.range, true, true)
    }

    private fun scheduleRefresh() {
        if (refreshQueued.compareAndSet(false, true)) {
            SwingUtilities.invokeLater {
                refreshQueued.set(false)
                dirty = true
                refreshIfDue()
            }
        }
    }

    /** Whether the panel actually has pixels to render into right now. */
    private fun hasVisibleArea() = isShowing && width > 0 && height > 0

    private fun refreshIfDue() {
        if (dirty && hasVisibleArea()) refreshGate.request()
    }

    fun refresh() {
        dirty = false
        refreshGate.stamp()
        val series = timeSeries.series
        val times = DoubleArray(series.itemCount) { series.getX(it).toDouble() }
        val values = DoubleArray(series.itemCount) { series.getY(it).toDouble() }
        val distances = computeRecurrenceMatrix(values, model.recurrenceEmbeddingDimension, model.recurrenceEmbeddingDelay)

        // Each embedded state is anchored at the time of its first sample
        val stateTimes = times.copyOf(distances.size)
        val maxDistance = distances.maxOfOrNull { row -> row.max() }?.takeIf { it > 0.0 } ?: 1.0
        applyPaintScale(maxDistance)
        plot.dataset = RecurrenceDataset(stateTimes, distances)

        // Blocks are center-anchored, so pad both axes by half a cell to avoid clipping the edges
        val cell = cellSpacing(stateTimes)
        renderer.blockWidth = cell
        renderer.blockHeight = cell
        val earliest = stateTimes.minOrNull() ?: 0.0
        val latest = stateTimes.maxOrNull() ?: 1.0
        if (domainAlignmentProvider != null) {
            applyAlignedDomain()
        } else {
            plot.domainAxis.setRange(earliest - cell / 2, latest + cell / 2)
        }
        plot.rangeAxis.setRange(earliest - cell / 2, latest + cell / 2)
    }

    /**
     * The colorbar is a chart subtitle and only belongs to spectrum mode, so it is added and removed
     * as the mode changes; the theme pass reruns after an add because it only reaches existing subtitles.
     */
    private fun applyPaintScale(maxDistance: Double) {
        val legendShown = chart.subtitles.contains(colorBarLegend)
        when (model.recurrenceMode) {
            RecurrenceMode.SPECTRUM -> {
                val scale = ChartColorMapPaintScale(0.0, maxDistance) { model.recurrenceColorMap }
                renderer.paintScale = scale
                colorBarLegend.scale = scale
                colorBarAxis.setRange(0.0, maxDistance)
                if (!legendShown) {
                    chart.addSubtitle(colorBarLegend)
                    chart.applySimbrainChartTheme()
                }
            }
            RecurrenceMode.THRESHOLD -> {
                // Resolved here, once per refresh, not per painted cell; kept on removal (index -1)
                // so a tab awaiting disposal does not flash a wrong palette color
                model.timeSeriesList.indexOf(timeSeries).takeIf { it >= 0 }?.let { paletteIndex = it }
                renderer.paintScale = ThresholdPaintScale(maxDistance * model.recurrenceThreshold, maxDistance) {
                    chartSeriesColor(paletteIndex)
                }
                if (legendShown) chart.removeSubtitle(colorBarLegend)
            }
        }
    }

    private fun cellSpacing(times: DoubleArray): Double {
        val gaps = times.sorted().zipWithNext { earlier, later -> later - earlier }.filter { it > 0 }
        return gaps.minOrNull() ?: 1.0
    }

    companion object {
        private val PREFERRED_SIZE = Dimension(500, 400)

        /** Shortest gap between two matrix rebuilds; the display evolves slowly enough for 10 Hz. */
        private const val REFRESH_MIN_INTERVAL_MS = 100
    }
}

/**
 * The shared alignment state an aligned recurrence plot pulls per refresh: the line chart's current
 * time range, and the range-axis area both charts reserve so their plot areas share left and right
 * pixel edges. The space is measured from actual tick labels by the desktop component.
 */
data class DomainAlignment(val range: Range, val rangeAxisSpace: AxisSpace)

/**
 * Matrix of Euclidean distances between every pair of the series' states, where a state is
 * [embeddingDimension] consecutive samples [embeddingDelay] apart, so with dimension 1 it is the
 * plain |x(i) - x(j)| recurrence matrix. Empty when the series is shorter than one embedded state.
 */
fun computeRecurrenceMatrix(values: DoubleArray, embeddingDimension: Int, embeddingDelay: Int): Array<DoubleArray> {
    val dimension = embeddingDimension.coerceAtLeast(1)
    val delay = embeddingDelay.coerceAtLeast(1)
    val stateCount = values.size - (dimension - 1) * delay
    if (stateCount <= 0) return emptyArray()
    val matrix = Array(stateCount) { DoubleArray(stateCount) }
    for (i in 0 until stateCount) {
        for (j in i + 1 until stateCount) {
            var sum = 0.0
            for (k in 0 until dimension) {
                val diff = values[i + k * delay] - values[j + k * delay]
                sum += diff * diff
            }
            val distance = sqrt(sum)
            matrix[i][j] = distance
            matrix[j][i] = distance
        }
    }
    return matrix
}

/**
 * Two-valued scale for threshold mode: cells within the threshold get the series' color, the rest
 * are transparent so the plot background shows through. The color resolves per paint call so it
 * tracks the series' current palette slot and a live theme switch.
 */
private class ThresholdPaintScale(
    private val threshold: Double,
    private val upper: Double,
    private val color: () -> Paint
) : PaintScale {

    override fun getLowerBound() = 0.0

    override fun getUpperBound() = upper

    override fun getPaint(value: Double): Paint =
        if (value.isFinite() && value <= threshold) color() else NO_DATA
}

/**
 * Presents a distance matrix to JFreeChart's block renderer without copying it. Both axes carry the
 * series' time values; item `i * n + j` reports the cell comparing states i and j.
 */
private class RecurrenceDataset(
    private val times: DoubleArray,
    private val distances: Array<DoubleArray>
) : AbstractXYZDataset() {

    override fun getSeriesCount() = 1

    override fun getSeriesKey(series: Int): Comparable<*> = "Distances"

    override fun getItemCount(series: Int) = times.size * times.size

    override fun getX(series: Int, item: Int): Number = times[item / times.size]

    override fun getY(series: Int, item: Int): Number = times[item % times.size]

    override fun getZ(series: Int, item: Int): Number = distances[item / times.size][item % times.size]
}
