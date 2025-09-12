package org.simbrain.plot.barchart

import com.thoughtworks.xstream.XStream
import org.jfree.data.category.DefaultCategoryDataset
import org.simbrain.util.UserParameter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import java.awt.Color
import javax.swing.SwingUtilities

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
    var barColor: Color = Color.red

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
        println("setBarValues called with: ${newPoint.contentToString()}")
        try {
            SwingUtilities.invokeAndWait {
                // Take care of size mismatches
                if (newPoint.size != numBars) {
                    dataset.clear()
                    numBars = newPoint.size
                }

                // Write the data
                for (i in newPoint.indices) {
                    if (i < barNames.size) {
                        dataset.setValue(newPoint[i], "Values", barNames[i])
                    } else {
                        // TODO: May need to go to this condition for if barNames is empty
                        dataset.setValue(newPoint[i], "Values", "" + (i + 1))
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun setBarNames(names: Array<String>) {
        this.barNames = names
    }

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
