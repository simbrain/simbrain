package org.simbrain.plot.piechart

import com.thoughtworks.xstream.XStream
import org.jfree.data.general.DefaultPieDataset
import org.simbrain.util.UserParameter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import javax.swing.SwingUtilities
import kotlin.math.abs

/**
 * Model data for pie charts.
 */
class PieChartModel : AttributeContainer, EditableObject {

    override val id: String
        get() = "Pie Chart"

    /**
     * JFreeChart dataset for pie charts.
     */
    val dataset = DefaultPieDataset<String>()

    @UserParameter(
        label = "Empty pie threshold",
        description = "If the total input to the chart is below this number it becomes empty"
    )
    var emptyPieThreshold = 1e-10

    private var isUninitialized: Boolean? = null

    /**
     * Names for the "slices" in the barchart. Can be set via coupling events
     * in [PieChartComponent].
     */
    var sliceNames = arrayOf<String>()

    /**
     * Track how many slices there are. If an array with a different number of
     * components is sent to this component, numSlices is updated.
     */
    private var numSlices = 0

    init {
        emptyPie()
    }

    private fun updatePieStatus() {
        if (isUninitialized == true) {
            dataset.clear()
            isUninitialized = false
        }
    }

    /**
     * Show this when there is no data or effectively no data.
     */
    private fun emptyPie() {
        isUninitialized = true
        dataset.clear()
        dataset.setValue("Empty pie", 1.0)
    }

    /**
     * Called by coupling producers via reflection.
     */
    @Consumable
    fun setValues(vector: DoubleArray) {
        if (vector.isEmpty()) {
            throw IllegalArgumentException("Pie chart supplied with empty array")
        }
        try {
            SwingUtilities.invokeAndWait {
                updatePieStatus()

                // Take care of size mismatches
                if (vector.size != numSlices) {
                    dataset.clear()
                    numSlices = vector.size
                }

                val total = vector.sumOf { abs(it) }

                // For minimal activation case just show a single pie slice
                if (total < emptyPieThreshold) {
                    emptyPie()
                    return@invokeAndWait
                }
                for (i in vector.indices) {
                    if (i < sliceNames.size) {
                        dataset.setValue(sliceNames[i], abs(vector[i] / total))
                    } else {
                        dataset.setValue("$i", abs(vector[i] / total))
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override val name: String
        get() = "Pie chart"

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
