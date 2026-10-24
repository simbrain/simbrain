package org.simbrain.plot.barchart

import com.thoughtworks.xstream.XStream
import kotlinx.coroutines.cancel
import org.jfree.data.category.DefaultCategoryDataset
import org.simbrain.plot.chartSeriesColor
import org.simbrain.util.UserParameter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.UiScope
import org.simbrain.util.uiInbox
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
     * Names of the incoming array's components, one per bar. Set via coupling events in [BarChartComponent].
     */
    private var componentNames = listOf<String>()

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

    /** A change to apply to [dataset] on the EDT. */
    private sealed interface Update {
        class Values(val values: DoubleArray) : Update
        class Names(val names: List<String>) : Update
    }

    @Transient
    private val ui = UiScope()

    /**
     * Changes to the EDT-confined dataset, applied in the order they were made without the caller waiting on the
     * EDT. Values sent each iteration are latest-wins: one superseded by a later one in the same batch is skipped.
     */
    @Transient
    private val updates = ui.uiInbox<Update> { batch ->
        val lastValues = batch.indexOfLast { it is Update.Values }
        batch.forEachIndexed { i, update ->
            when (update) {
                is Update.Values -> if (i == lastValues) applyValues(update.values)
                is Update.Names -> applyNames(update.names)
            }
        }
    }

    /**
     * Called by coupling producers via reflection.
     */
    @Consumable
    fun setBarValues(newPoint: DoubleArray) = updates.post(Update.Values(newPoint))

    /**
     * Set the component names and rename any bars already in the dataset, so a label change shows without
     * waiting for the next value update.
     */
    fun setComponentNames(names: List<String>) = updates.post(Update.Names(names))

    /** Stops applying updates; called when the owning component closes. */
    fun close() = ui.cancel()

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
        componentNames = names
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

    private fun componentName(i: Int) = componentNames.getOrElse(i) { "${i + 1}" }

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
