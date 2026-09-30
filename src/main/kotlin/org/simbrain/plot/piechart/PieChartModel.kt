package org.simbrain.plot.piechart

import com.thoughtworks.xstream.XStream
import kotlinx.coroutines.cancel
import org.jfree.data.general.DefaultPieDataset
import org.simbrain.util.UserParameter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.UiScope
import org.simbrain.util.uiInbox
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
    var componentNames: List<String>
        get() = names
        set(value) = updates.post(Update.Names(value))

    /** The names currently applied to the dataset. Workspaces saved before this field load through [componentNames]. */
    private var names = listOf<String>()

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
    fun setValues(vector: DoubleArray) {
        if (vector.isEmpty()) {
            throw IllegalArgumentException("Pie chart supplied with empty array")
        }
        updates.post(Update.Values(vector))
    }

    /** Stops applying updates; called when the owning component closes. */
    fun close() = ui.cancel()

    /** Lands pending updates before XStream writes the dataset. */
    private fun writeReplace(): Any {
        updates.flush()
        return this
    }

    private fun applyValues(vector: DoubleArray) {
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
            return
        }
        for (i in vector.indices) {
            dataset.setValue(componentName(i), abs(vector[i] / total))
        }
    }

    private fun applyNames(value: List<String>) {
        names = value
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

    private fun componentName(i: Int) = names.getOrElse(i) { "$i" }

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
