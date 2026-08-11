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
import org.simbrain.util.runOnEventThread
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Workspace
import kotlin.math.abs

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
        initValue = ChartColorMap.COOL_TO_HOT,
        label = "Color map",
        description = "How values are mapped to colors",
        order = 10
    )

    var isAutoRange by GuiEditable(
        initValue = true,
        label = "Auto range",
        description = "If true, the color scale expands symmetrically around zero to span the values currently in the window",
        order = 20
    )

    var rangeLowerBound by GuiEditable(
        initValue = -1.0,
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

    /**
     * Number of rows, taken from the widest retained column or the available row labels so a coupled
     * neuron collection is labelled before its first values arrive. Cells past a shorter column's end
     * have no value and render as background rather than as a real reading.
     */
    val rowCount get() = maxOf(columns.maxOfOrNull { it.size } ?: 0, componentNames.size)

    val columnCount get() = columns.size

    /** Names of the incoming array's components, one per row, ordinarily from a coupled neuron collection. */
    var componentNames: List<String> = emptyList()
        private set

    @Consumable(description = "Append a column of values")
    fun setValues(values: DoubleArray) {
        runOnEventThread {
            columns.add(values.copyOf())
            times.add(if (::timeSupplier.isInitialized) timeSupplier() else times.size)
            trimToWindow()
            events.propertyChanged.fire()
        }
    }

    fun clearData() {
        runOnEventThread {
            columns.clear()
            times.clear()
            events.propertyChanged.fire()
        }
    }

    fun setComponentNames(names: List<String>) {
        runOnEventThread {
            componentNames = names
            events.propertyChanged.fire()
        }
    }

    /**
     * The color scale's current bounds, always symmetric around zero so the midpoint color retains
     * its meaning. Auto-range chooses the magnitude from retained data; the default fixed range
     * keeps colors stable as incoming values change.
     */
    fun colorRange(): ClosedFloatingPointRange<Double> {
        if (!isAutoRange) return symmetricRange(maxOf(abs(rangeLowerBound), abs(rangeUpperBound)))
        var magnitude = 0.0
        columns.forEach { column ->
            column.forEach { value ->
                if (value.isFinite()) {
                    magnitude = maxOf(magnitude, abs(value))
                }
            }
        }
        return symmetricRange(magnitude)
    }

    private fun symmetricRange(magnitude: Double): ClosedFloatingPointRange<Double> {
        val safeMagnitude = if (magnitude > 0.0) magnitude else 1.0
        return -safeMagnitude..safeMagnitude
    }

    fun dataset(): XYZDataset = HeatMapDataset(this)

    private fun trimToWindow() {
        if (!fixedWidth) return
        val excess = columns.size - windowSize.coerceAtLeast(1)
        if (excess <= 0) return
        columns.subList(0, excess).clear()
        times.subList(0, excess).clear()
    }

    private fun readResolve(): Any {
        events = HeatMapEvents()
        return this
    }

    override val id: String get() = "Heat map"

    override val name: String get() = "Heat map"
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
