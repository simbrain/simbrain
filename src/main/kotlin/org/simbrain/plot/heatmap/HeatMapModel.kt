/**
 * Data model for the heat map plot: a matrix of values where one axis is time and the other indexes
 * the elements of whatever array is coupled in, with value shown as color.
 *
 * This is the continuous-valued counterpart to the raster plot. Where the raster draws a dot per spike,
 * this draws every value, which is the standard way to show a whole population's activity over time in
 * one image, or to stack repeated trials of the same measurement.
 */
package org.simbrain.plot.heatmap

import org.jfree.data.xy.AbstractXYZDataset
import org.jfree.data.xy.XYZDataset
import org.simbrain.plot.ChartColorMap
import org.simbrain.plot.HeatMapEvents
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Workspace
import javax.swing.SwingUtilities

class HeatMapModel : AttributeContainer, EditableObject {

    @Transient
    lateinit var timeSupplier: () -> Int

    /** One entry per recorded time step, each holding that step's values down the rows. */
    val columns: MutableList<DoubleArray> = ArrayList()

    /** Workspace time of each entry in [columns], used for the domain axis. */
    val times: MutableList<Int> = ArrayList()

    @Transient
    var events = HeatMapEvents()
        private set

    var colorMap by GuiEditable(
        initValue = ChartColorMap.JET,
        label = "Color map",
        description = "How values are mapped to colors",
        order = 10
    )

    var isAutoRange by GuiEditable(
        initValue = true,
        label = "Auto range",
        description = "If true, the color scale spans the values currently in the window, rather than a fixed range",
        order = 20
    )

    var rangeLowerBound by GuiEditable(
        initValue = 0.0,
        label = "Color range lower bound",
        description = "Value mapped to the low end of the color map when auto range is off",
        onUpdate = { enableWidget(!widgetValue(HeatMapModel::isAutoRange)) },
        order = 30
    )

    var rangeUpperBound by GuiEditable(
        initValue = 1.0,
        label = "Color range upper bound",
        description = "Value mapped to the high end of the color map when auto range is off",
        onUpdate = { enableWidget(!widgetValue(HeatMapModel::isAutoRange)) },
        order = 40
    )

    var fixedWidth by GuiEditable(
        initValue = true,
        label = "Fixed width",
        description = "If true, only the most recent time steps are kept",
        order = 50
    )

    var windowSize by GuiEditable(
        initValue = 200,
        label = "Window size",
        description = "Number of time steps retained when fixed width is used",
        conditionallyEnabledBy = HeatMapModel::fixedWidth,
        order = 60
    )

    /** Number of rows, taken from the width of the most recent column. Zero until data arrives. */
    val rowCount get() = columns.lastOrNull()?.size ?: 0

    val columnCount get() = columns.size

    @Consumable(description = "Append a column of values")
    fun setValues(values: DoubleArray) {
        onEventThread {
            columns.add(values.copyOf())
            times.add(if (::timeSupplier.isInitialized) timeSupplier() else times.size)
            trimToWindow()
            events.propertyChanged.fire()
        }
    }

    fun clearData() {
        onEventThread {
            columns.clear()
            times.clear()
            events.propertyChanged.fire()
        }
    }

    /**
     * The color scale's current bounds. In auto-range mode this is the span of the retained data,
     * widened to a non-degenerate interval so a constant-valued matrix still renders.
     */
    fun colorRange(): ClosedFloatingPointRange<Double> {
        if (!isAutoRange) return rangeLowerBound..maxOf(rangeUpperBound, rangeLowerBound + 1e-12)
        val values = columns.asSequence().flatMap { it.asSequence() }.filter { it.isFinite() }
        val low = values.minOrNull() ?: 0.0
        val high = values.maxOrNull() ?: 1.0
        return if (high - low < 1e-12) low..(low + 1e-12) else low..high
    }

    fun dataset(): XYZDataset = HeatMapDataset(this)

    private fun trimToWindow() {
        if (!fixedWidth) return
        val excess = columns.size - windowSize.coerceAtLeast(1)
        repeat(excess.coerceAtLeast(0)) {
            columns.removeAt(0)
            times.removeAt(0)
        }
    }

    private fun onEventThread(block: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeAndWait(block)
    }

    private fun readResolve(): Any {
        events = HeatMapEvents()
        return this
    }

    override val id: String get() = "Heat map"
}

/**
 * Presents the model's matrix to JFreeChart's block renderer without copying it. Items are indexed
 * column-major, so item `column * rowCount + row` reports that cell's time, row index, and value.
 */
private class HeatMapDataset(private val model: HeatMapModel) : AbstractXYZDataset() {

    private val rows = model.rowCount

    private val snapshot = model.columns.toList()

    private val snapshotTimes = model.times.toList()

    override fun getSeriesCount() = 1

    override fun getSeriesKey(series: Int): Comparable<*> = "Values"

    override fun getItemCount(series: Int) = rows * snapshot.size

    override fun getX(series: Int, item: Int): Number = snapshotTimes[item / rows]

    override fun getY(series: Int, item: Int): Number = item % rows

    override fun getZ(series: Int, item: Int): Number? =
        snapshot[item / rows].getOrNull(item % rows)
}

fun Workspace.createHeatMapModel() = HeatMapModel().also { it.timeSupplier = { time } }
