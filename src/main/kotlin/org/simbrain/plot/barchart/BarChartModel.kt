/**
 * The bar chart's data: one bar per component of the coupled array. Couplings post values and names from the
 * updating thread; the JFreeChart dataset is EDT-confined and changes only when those land on the EDT.
 */
package org.simbrain.plot.barchart

import com.thoughtworks.xstream.XStream
import org.jfree.data.category.DefaultCategoryDataset
import org.simbrain.plot.ChartUpdate
import org.simbrain.plot.applyChartUpdates
import org.simbrain.plot.chartSeriesColor
import org.simbrain.plot.componentLabel
import org.simbrain.util.EdtUpdates
import org.simbrain.util.UserParameter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import java.awt.Color

/**
 * Data for a JFreeChart bar chart.
 */
class BarChartModel : AttributeContainer, EditableObject, AutoCloseable {

    override val id: String
        get() = "Bar Chart"

    /**
     * JFreeChart dataset for bar charts.
     */
    private val dataset = DefaultCategoryDataset()

    /**
     * Color of bars in barchart.
     */
    @UserParameter(label = "Bar Color", order = 4)
    var barColor: Color = chartSeriesColor(0)

    /**
     * Auto range bar chart.
     */
    @UserParameter(label = "Auto Range", order = 3)
    var autoRange = true
        @JvmName("isAutoRange")
        get

    /**
     * Maximum range.
     */
    @UserParameter(label = "Upper Bound", order = 2)
    var upperBound = 10.0

    /**
     * Minimum range.
     */
    @UserParameter(label = "Lower Bound", order = 1)
    var lowerBound = 0.0

    /**
     * Names of the incoming array's components, one per bar. Set via coupling events in [BarChartComponent].
     * Setting new names renames any bars already in the dataset, so a label change shows without waiting for the
     * next value update. Reads return the names applied so far.
     */
    var componentNames: List<String>
        get() = names
        set(value) = updates.post(ChartUpdate.Names(value))

    /** The names currently applied to the dataset. Workspaces saved before this field load through [componentNames]. */
    private var names = listOf<String>()

    /**
     * Track how many bars there are. If an array with a different number of
     * components is sent to this component, numBars is updated.
     */
    private var numBars = 0

    /**
     * Return JFreeChart category dataset.
     */
    fun getDataset(): DefaultCategoryDataset {
        return dataset
    }

    fun setRange(lowerBound: Double, upperBound: Double) {
        this.lowerBound = lowerBound
        this.upperBound = upperBound
    }

    /** Changes to the EDT-confined dataset. */
    @Transient
    private val updates = EdtUpdates<ChartUpdate> { applyChartUpdates(it, ::applyValues, ::applyNames) }

    /**
     * Called by coupling producers via reflection. The values are copied, since a producer may hand over a buffer it
     * keeps writing to before the update lands on the event thread.
     */
    @Consumable
    fun setBarValues(newPoint: DoubleArray) = updates.post(ChartUpdate.Values(newPoint.copyOf()))

    /** Stops applying updates; called when the owning component leaves the workspace. */
    override fun close() = updates.close()

    /** Lands pending updates before XStream writes the dataset. */
    private fun writeReplace(): Any {
        updates.flush()
        return this
    }

    private fun applyValues(newPoint: DoubleArray) {
        // Take care of size mismatches
        if (newPoint.size != numBars) {
            dataset.clear()
            numBars = newPoint.size
        }

        // Write the data
        for (i in newPoint.indices) {
            dataset.setValue(newPoint[i], "Values", componentName(i))
        }
    }

    private fun applyNames(names: List<String>) {
        this.names = names
        if (dataset.columnCount > 0) {
            // The producer now sends one value per name, so the bars are rebuilt to match: one whose
            // neuron was deleted goes rather than lingering under a stand-in number, and one whose
            // neuron came back shows at zero until the next update rather than being missing.
            val previous = (0 until dataset.columnCount).map { dataset.getValue(0, it) }
            dataset.clear()
            numBars = if (names.isEmpty()) previous.size else names.size
            (0 until numBars).forEach { i ->
                dataset.setValue(previous.getOrNull(i) ?: 0.0, "Values", componentName(i))
            }
        }
    }

    private fun componentName(i: Int) = componentLabel(names, i)

    override val name: String
        get() = "Bar chart"

    companion object {
        /**
         * Returns a properly initialized xstream object.
         */
        @JvmStatic
        fun getXStream(): XStream {
            return getSimbrainXStream()
        }
    }

    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    private fun readResolve(): Any {
        return this
    }
}
