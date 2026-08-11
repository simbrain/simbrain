package org.simbrain.plot.piechart

import com.thoughtworks.xstream.XStream
import org.jfree.data.general.DefaultPieDataset
import org.simbrain.util.UserParameter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.runOnEventThread
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
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
     * Names of the incoming array's components, one per slice. Set via coupling events in
     * [PieChartComponent]. Setting new names renames any slices already in the dataset, so a label change
     * shows without waiting for the next value update.
     */
    var componentNames = listOf<String>()
        set(value) {
            runOnEventThread {
                field = value
                if (isUninitialized == false && dataset.itemCount > 0) {
                    // The producer now sends one value per name, so the slices are rebuilt to match: one
                    // whose neuron was deleted goes rather than lingering under a stand-in number, and one
                    // whose neuron came back shows at zero until the next update rather than being missing.
                    val previous = (0 until dataset.itemCount).map { dataset.getValue(it) }
                    dataset.clear()
                    numSlices = if (value.isEmpty()) previous.size else value.size
                    (0 until numSlices).forEach { i ->
                        dataset.setValue(componentName(i), previous.getOrNull(i) ?: 0.0)
                    }
                }
            }
        }

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
        runOnEventThread {
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
                return@runOnEventThread
            }
            for (i in vector.indices) {
                dataset.setValue(componentName(i), abs(vector[i] / total))
            }
        }
    }

    private fun componentName(i: Int) = componentNames.getOrElse(i) { "$i" }

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
