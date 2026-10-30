/**
 * The raster plot's data: a dot per spike, one row per component of each coupled array. Couplings post columns from
 * the updating thread; the series are EDT-confined and change only when those land on the EDT.
 */
package org.simbrain.plot.raster

import com.thoughtworks.xstream.XStream
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.simbrain.plot.PlotPreferences
import org.simbrain.plot.RasterPlotEvents
import org.simbrain.util.UserParameter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.util.EdtUpdates
import java.util.function.Supplier

/**
 * Data model for a raster plot.
 */
class RasterModel(timeSupplier: Supplier<Int>? = null) : EditableObject, AutoCloseable {

    /**
     * Lambda to supply time to the time series model.
     */
    @Transient
    lateinit var timeSupplier: Supplier<Int>

    val dataset: XYSeriesCollection = XYSeriesCollection()

    @Transient
    val rasterConsumerList = dataset.series.mapIndexed { index, _ -> RasterConsumer(index) }.toMutableList()

    @UserParameter(
        label = "Dot Size",
        description = "Size of dots in chart",
        order = 5)
    var dotSize: Int = PlotPreferences.rasterDotSize
        set(value) {
            field = value
            events.propertyChanged.fire()
        }

    var windowSize: Int by GuiEditable(
        initValue = PlotPreferences.rasterWindowSize,
        label = "Window Size",
        setter = {
            field = it
            events.propertyChanged.fire()
        },
        description = "How many time points can be contained in the window",
        conditionallyEnabledBy = RasterModel::isFixedWidth,
        order = 10
    )

    @UserParameter(
        label = "Fixed width",
        description = "If true, the raster window never extends beyond a fixed with",
        order = 30
    )
    var isFixedWidth: Boolean = PlotPreferences.rasterFixedWidth
        set(value) {
            field = value
            events.propertyChanged.fire()
        }

    @UserParameter(
        label = "Spike Threshold",
        description = "For nonspiking neurons activation above this is taken to be a spike",
        order = 40
    )
    var spikeThreshold: Double = 0.5

    var rowLabel: String by GuiEditable(
        initValue = "",
        label = "Row label",
        description = "Optional label for the row axis",
        setter = {
            field = it
            events.propertyChanged.fire()
        },
        order = 45
    )

    @Transient
    var events = RasterPlotEvents()
        private set

    /**
     * Names of the incoming array's components, one per row of the plot, ordinarily from a coupled neuron
     * collection. Rows beyond the names supplied fall back to their index.
     */
    var componentNames: List<String> = emptyList()
        private set

    fun setComponentNames(names: List<String>) {
        componentNames = names
        events.propertyChanged.fire()
    }

    /**
     * Highest row index the plot can show, used to size the row axis. Rows come from the components of the
     * arrays sent in, so this is the longest array seen or the number of names supplied.
     */
    var rowCount: Int = 0
        private set

    init {
        addDataSources(INITIAL_DATA_SOURCES)
        if (timeSupplier != null) {
            this.timeSupplier = timeSupplier
        }
    }

    /**
     * Create specified number of set of data sources. Adds these two existing
     * data sources.
     *
     * @param numDataSources number of data sources to initialize plot with
     */
    fun addDataSources(numDataSources: Int, names: List<String>? = null) {
        for (i in 0 until numDataSources) {
            addDataSource(names?.get(i) ?: (dataset.seriesCount + 1).toString())
        }
    }

    /**
     * Removes the last data source from the chart.
     */
    fun removeDataSource() {
        rasterConsumerList.lastOrNull()?.let { removeDataSource(it) }
    }

    /**
     * Removes a specific data source from the chart. Consumers after it shift down one series in the
     * dataset, so their indices are decremented to keep each one pointing at the same [XYSeries].
     */
    fun removeDataSource(consumer: RasterConsumer) {
        if (!rasterConsumerList.remove(consumer)) return
        dataset.removeSeries(consumer.index)
        rasterConsumerList.forEach { if (it.index > consumer.index) it.index-- }
        events.rasterConsumerRemoved.fire(consumer)
    }

    val numDataSources get() = dataset.seriesCount

    @JvmOverloads
    fun addDataSource(name: String = (dataset.seriesCount + 1).toString()) {
        val currentSize = dataset.seriesCount
        dataset.addSeries(XYSeries(name))
        val rc = RasterConsumer(currentSize)
        events.rasterConsumerAdded.fire(rc)
        rasterConsumerList.add(rc)
    }

    /** Clears every series, after the columns already sent have landed. */
    fun clearData() = updates.post(Clear)

    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    private fun readResolve(): Any {
        events = RasterPlotEvents()
        return this
    }

    /** Lands any queued columns before XStream writes the dataset, so a save right after an iteration has them. */
    private fun writeReplace(): Any {
        updates.flush()
        return this
    }

    /** Stops applying queued columns; called when the owning component leaves the workspace. */
    override fun close() = updates.close()

    /** A change to apply to the EDT-confined dataset. */
    private sealed interface Update

    /** One incoming array's spikes, captured on the updating thread and waiting to be added on the EDT. */
    private class PendingColumn(val consumer: RasterConsumer, val time: Int, val size: Int, val spikeRows: IntArray) :
        Update

    private object Clear : Update

    /**
     * Columns for the chart, added on the EDT without the updating thread waiting for it: the dataset is
     * EDT-confined, and awaiting the EDT from the coupling made every workspace iteration wait behind whatever the
     * EDT was painting.
     */
    @Transient
    private val updates = EdtUpdates<Update> { applyUpdates(it) }

    /** Applies a batch of columns and clears, notifying each touched series once rather than per point. */
    private fun applyUpdates(batch: List<Update>) {
        val touched = LinkedHashSet<XYSeries>()
        fun quiet(series: XYSeries) = series.also { if (touched.add(it)) it.notify = false }
        for (update in batch) {
            when (update) {
                is PendingColumn -> {
                    if (update.consumer !in rasterConsumerList) continue
                    if (update.size > rowCount) {
                        rowCount = update.size
                        events.propertyChanged.fire()
                    }
                    val series = quiet(dataset.getSeries(update.consumer.index))
                    if (update.spikeRows.isEmpty()) {
                        series.add(update.time, null, false)
                    } else {
                        update.spikeRows.forEach { series.add(update.time, it, false) }
                    }
                }
                Clear -> for (i in 0 until dataset.seriesCount) quiet(dataset.getSeries(i)).clear()
            }
        }
        touched.forEach { it.notify = true }
    }

    /**
     * Objects that represent separate sets of raster points, shown in a different color in the
     * chart.
     */
    inner class RasterConsumer internal constructor(index: Int) : AttributeContainer {
        /**
         * Index of this consumer in an [XYSeriesCollection]
         */
        var index: Int = 0

        init {
            this.index = index
        }

        /**
         * Plot an array of values as a vertical bar in a raster plot. Each component of the array is associated with one row of the plot.
         * Canonically used to display spiking data, represented with binary vectors. If real-values (e.g. activations) are sent in, then values above a threshold (default .5) are interpreted as spikes
         * <br></br>
         * Example 1: [0, 1, 0, 0 , 1] would show 2 dots vertically at the 2nd and 5th position at the current time
         * <br></br>
         * Example 2: [0.0, 0.6, -0.3, 0.0, 1.0] would show 2 dots vertically at the 2nd and 5th position at the current time
         */
        @Consumable
        fun setValues(values: DoubleArray) {
            val threshold = spikeThreshold
            val spikeRows = values.indices.filter { values[it] >= threshold }.toIntArray()
            updates.post(PendingColumn(this, timeSupplier.get(), values.size, spikeRows))
        }

        override val id: String
            get() = "Raster " + (index + 1)
    }

    companion object {
        /**
         * Default number of data sources for plot initialization.
         */
        private const val INITIAL_DATA_SOURCES = 1

        @JvmStatic
        val xStream: XStream
            /**
             * Returns a properly initialized xstream object.
             *
             * @return the XStream object
             */
            get() {
                val xstream = getSimbrainXStream()
                return xstream
            }
    }
}
