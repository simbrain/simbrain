package org.simbrain.plot.barchart

import com.thoughtworks.xstream.XStream
import org.jfree.data.category.DefaultCategoryDataset
import org.simbrain.plot.chartSeriesColor
import org.simbrain.util.UserParameter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.runOnEventThread
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import java.awt.Color

/**
 * Data for a JFreeChart bar chart.
 */
class BarChartModel : AttributeContainer, EditableObject {

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
     * Names for the bars in the barchart. Set via coupling events in
     * [BarChartComponent].
     */
    private var barNames = arrayOf<String>()

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

    /**
     * Called by coupling producers via reflection.
     */
    @Consumable
    fun setBarValues(newPoint: DoubleArray) {
        runOnEventThread {
            // Take care of size mismatches
            if (newPoint.size != numBars) {
                dataset.clear()
                numBars = newPoint.size
            }

            // Write the data
            for (i in newPoint.indices) {
                dataset.setValue(newPoint[i], "Values", barName(i))
            }
        }
    }

    /**
     * Set the bar names and immediately rename any bars already in the dataset, so a label change shows
     * without waiting for the next value update.
     */
    fun setBarNames(names: Array<String>) {
        runOnEventThread {
            barNames = names
            if (dataset.columnCount > 0) {
                val values = (0 until dataset.columnCount).map { dataset.getValue(0, it) }
                dataset.clear()
                values.forEachIndexed { i, value -> dataset.setValue(value, "Values", barName(i)) }
            }
        }
    }

    private fun barName(i: Int) = if (i < barNames.size) barNames[i] else "${i + 1}"

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
