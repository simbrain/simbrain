package org.simbrain.plot.timeseries

import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.simbrain.plot.ChartColorMap
import org.simbrain.plot.TimeSeriesEvents
import org.simbrain.util.UserParameter
import org.simbrain.util.WithXStreamPropertyConverter
import org.simbrain.util.createXStreamPropertyConverter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.runOnEventThread
import org.simbrain.workspace.AttributeComponent
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Workspace
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.SwingUtilities

/**
 * Data model for a time series plot. A time series consumes an array of
 * doubles, with one component for each member of the time series.
 *
 * To couple to scalar consumer just couple to a specific time series.
 *
 * There is no support for representing separate scalar values in a single time series.
 */
class TimeSeriesModel : AttributeContainer, EditableObject {

    @Transient
    lateinit var timeSupplier: () -> Int

    /**
     * Time Series Data.
     */
    @Transient
    var dataset = XYSeriesCollection()
        private set

    @UserParameter(
        label = "Auto Range", description = "If true, automatically adjusts the range of the time series data " +
                "based on the maximum and minimum values present at a given time", order = 10
    )
    var isAutoRange = true

    var rangeUpperBound by GuiEditable(
        initValue = 1.0,
        label = "Range upper bound",
        description = "Range upper bound in fixed range mode (auto-range turned off)",
        onUpdate = { enableWidget(!widgetValue(TimeSeriesModel::isAutoRange)) },
        order = 20
    )

    var rangeLowerBound by GuiEditable(
        initValue = 0.0,
        label = "Range lower bound",
        description = "Range lower bound in fixed range mode (auto-range turned off)",
        onUpdate = { enableWidget(!widgetValue(TimeSeriesModel::isAutoRange)) },
        order = 30
    )

    var useAutoRangeMinimumUpperBound = false

    var autoRangeMinimumUpperBound by GuiEditable(
        initValue = 1.0,
        label = "Auto Range Minimum Upper Bound",
        description = "When auto range is on, if the range is less than this value, the range will be set to this value",
        useCheckboxFrom = TimeSeriesModel::useAutoRangeMinimumUpperBound,
        order = 40
    )

    var useAutoRangeMaximumLowerBound = false

    var autoRangeMaximumLowerBound by GuiEditable(
        initValue = 0.0,
        label = "Auto Range Maximum Lower Bound",
        description = "When auto range is on, if the range is greater than this value, the range will be set to this value",
        useCheckboxFrom = TimeSeriesModel::useAutoRangeMaximumLowerBound,
        order = 50
    )

    @UserParameter(
        label = "Fixed Width", description = "If true, the time series window never extends beyond a fixed with",
        order = 60
    )
    var fixedWidth = false
        set(value) {
            field = value
            if (value) {
                for (s in dataset.series) {
                    (s as XYSeries?)!!.maximumItemCount = windowSize
                }
            } else {
                for (s in dataset.series) {
                    (s as XYSeries?)!!.maximumItemCount = Int.MAX_VALUE
                }
            }
        }

    var windowSize by GuiEditable(
        initValue = 100,
        label = "Window size",
        description = "Size of window when fixed width is used.",
        conditionallyEnabledBy = TimeSeriesModel::fixedWidth,
        order = 70
    )

    /**
     * Which plots the desktop window shows. Window composition rather than plot data, so deliberately
     * not user-editable here: embedded plot panels (trainer dialogs, simulations) have no recurrence
     * views, and their preferences dialog should not offer a selector that does nothing. It is
     * controlled from the desktop component's toolbar and View menu, and lives on the model only so
     * it persists with the plot.
     */
    var recurrenceView = RecurrenceView.TIME_SERIES

    var recurrenceMode by GuiEditable(
        initValue = RecurrenceMode.THRESHOLD,
        label = "Mode",
        description = "Threshold marks pairs of times whose states are within the threshold distance; " +
                "spectrum colors every pair by its distance",
        tab = "Recurrence",
        order = 110
    )

    var recurrenceThreshold by GuiEditable(
        initValue = 0.1,
        label = "Threshold",
        description = "Fraction of the largest pairwise distance in the window within which two states " +
                "count as recurrent",
        min = 0.0,
        max = 1.0,
        onUpdate = { enableWidget(widgetValue(TimeSeriesModel::recurrenceMode) == RecurrenceMode.THRESHOLD) },
        tab = "Recurrence",
        order = 120
    )

    var recurrenceColorMap by GuiEditable(
        initValue = ChartColorMap.JET,
        label = "Color map",
        description = "How distances are mapped to colors in spectrum mode",
        onUpdate = { enableWidget(widgetValue(TimeSeriesModel::recurrenceMode) == RecurrenceMode.SPECTRUM) },
        tab = "Recurrence",
        order = 130
    )

    var recurrenceEmbeddingDimension by GuiEditable(
        initValue = 1,
        label = "Embedding dimension",
        description = "Number of consecutive samples treated as one state via time-delay embedding; " +
                "1 compares raw values",
        min = 1,
        tab = "Recurrence",
        order = 140
    )

    var recurrenceEmbeddingDelay by GuiEditable(
        initValue = 1,
        label = "Embedding delay",
        description = "Lag in samples between the components of each embedded state",
        min = 1,
        onUpdate = { enableWidget(widgetValue(TimeSeriesModel::recurrenceEmbeddingDimension) > 1) },
        tab = "Recurrence",
        order = 150
    )

    var recurrenceMaxPoints by GuiEditable(
        initValue = 200,
        label = "Max points",
        description = "Most recent points of the series used for the recurrence matrix, bounding its size " +
                "when the series grows without a fixed width",
        min = 2,
        max = RECURRENCE_MAX_POINTS_LIMIT,
        tab = "Recurrence",
        order = 160
    )

    /**
     * List of time series objects which can be coupled to. Copy-on-write because the chart panel walks it on
     * the event thread during every repaint, while couplings can add to it from the workspace update thread.
     */
    val timeSeriesList: MutableList<TimeSeries> = CopyOnWriteArrayList()

    @Transient
    var events = TimeSeriesEvents()
        private set

    /**
     * Construct a time series model.
     *
     * @param timeSupplier the supplier for the x-axis of the graph
     */
    init {
        fixedWidth = fixedWidth // Force update by triggering custom setter
    }

    /**
     * Create specified number of data sources.
     */
    fun addTimeSeries(numSeries: Int) {
        for (i in 0 until numSeries) {
            addTimeSeries()
        }
    }

    fun clearData() {
        val seriesCount = dataset.seriesCount
        var i = 0
        while (seriesCount > i) {
            dataset.getSeries(i).clear()
            ++i
        }
    }

    /**
     * Add scalar data to a specified time series. Called by scripts.
     *
     * @param seriesIndex index of data source to use
     * @param time        data for x axis
     * @param value       data for y axis Adds a data source to the chart with
     * the specified description.
     */
    fun addData(seriesIndex: Int, time: Double, value: Double) {
        if (seriesIndex < dataset.seriesCount) {
            val currentSeries = dataset.getSeries(seriesIndex)
            currentSeries.add(time, value)
        }
    }

    /**
     * Adds a [TimeSeries] with a default description, skipping numbers still in use so that removing a series
     * from the middle of the chart does not make the next add collide with a surviving key.
     */
    fun addTimeSeries() {
        val usedKeys = timeSeriesList.map { it.series.key.toString() }.toSet()
        val description = generateSequence(1) { it + 1 }
            .map { "Series $it" }
            .first { it !in usedKeys }
        addTimeSeries(description)
    }

    /**
     * Adds a [TimeSeries] to the chart with a specified
     * description.
     *
     * @param description description for the time series
     * @return a reference to the series, or null if the model is in scalar mode
     */
    fun addTimeSeries(description: String): TimeSeries {
        lateinit var sts: TimeSeries
        // On the event thread because the dataset is being painted from there
        runOnEventThread {
            sts = TimeSeries(addXYSeries(description))
            timeSeriesList.add(sts)
        }
        events.timeSeriesAdded.fire(sts)
        return sts
    }

    @Consumable
    fun setValues(array: DoubleArray) {
        if (timeSeriesList.isEmpty()) {
            addTimeSeries(array.size)
        }
        var i = 0
        while (i < array.size && i < timeSeriesList.size) {
            timeSeriesList[i].setValue(array[i])
            i++
        }
    }

    /**
     * Adds an xy series to the chart with the specified description.
     */
    private fun addXYSeries(description: String): XYSeries {
        val xy = newXYSeries(description)
        dataset.addSeries(xy)
        return xy
    }

    /**
     * A series configured like the chart's others but not yet added to the dataset, for callers that control
     * dataset membership and ordering themselves.
     */
    private fun newXYSeries(description: String) = XYSeries(description).apply {
        maximumItemCount = windowSize
        this.description = description
    }

    /**
     * Rename the existing series in place, preserving their data. Expects one description per series. Keys
     * move through unique placeholders first because [XYSeriesCollection] vetoes a rename that would
     * momentarily collide with another series' current key.
     */
    fun renameTimeSeries(descriptions: List<String>) {
        if (descriptions.size != timeSeriesList.size) return
        renameEachTimeSeries(timeSeriesList.zip(descriptions))
    }

    /**
     * Rename the given series in place, preserving their data, leaving any series not named here untouched.
     * See [renameTimeSeries] for why the keys move through placeholders.
     */
    fun renameEachTimeSeries(renames: List<Pair<TimeSeries, String>>) {
        if (renames.none { (ts, description) -> ts.description != description }) return
        runOnEventThread {
            renames.forEachIndexed { i, (ts, _) -> ts.series.key = "\u200B__renaming__$i" }
            renames.forEach { (ts, description) ->
                ts.series.key = description
                ts.description = description
                ts.series.fireSeriesChanged()
            }
        }
    }

    /**
     * Make the series match [components] one for one, following each component by its
     * [AttributeComponent.key] rather than by position. A component already on the chart keeps its series and
     * therefore its history, one that has gone has its series removed, and a new one gets a new series, so
     * deleting a neuron from a coupled collection costs only that neuron's data.
     *
     * Series carrying no key yet, from a plot that was just created or reopened from a file, adopt the
     * incoming keys positionally when the counts line up, rather than being discarded and rebuilt.
     */
    fun syncTimeSeries(components: List<AttributeComponent>) {
        if (components.isEmpty()) return
        runOnEventThread {
            if (timeSeriesList.none { it.componentKey != null } && timeSeriesList.size == components.size) {
                timeSeriesList.zip(components).forEach { (ts, component) -> ts.componentKey = component.key }
            }
            val existingByKey = timeSeriesList.filter { it.componentKey != null }.associateBy { it.componentKey }
            // Park every key out of the way first: XYSeriesCollection rejects a key that momentarily
            // collides with another series', which reordering and renaming together can easily produce.
            timeSeriesList.forEachIndexed { i, ts -> ts.series.key = "\u200B__syncing__$i" }
            val ordered = components.map { component ->
                (existingByKey[component.key] ?: TimeSeries(newXYSeries(component.name)).also {
                    events.timeSeriesAdded.fire(it)
                }).apply {
                    componentKey = component.key
                    series.key = component.name
                    description = component.name
                }
            }
            timeSeriesList.filter { removed -> ordered.none { it === removed } }
                .forEach { events.timeSeriesRemoved.fire(it) }
            dataset.removeAllSeries()
            ordered.forEach { dataset.addSeries(it.series) }
            timeSeriesList.clear()
            timeSeriesList.addAll(ordered)
            ordered.forEach { it.series.fireSeriesChanged() }
        }
    }

    /**
     * Remove all [TimeSeries] objects.
     */
    fun removeAllTimeSeries() {
        val removed = timeSeriesList.toList()
        runOnEventThread {
            removed.forEach { dataset.removeSeries(it.series) }
            timeSeriesList.clear()
        }
        removed.forEach { events.timeSeriesRemoved.fire(it) }
    }

    /**
     * Remove a specific scalar time series.
     *
     * @param ts the time series to remove.
     */
    fun removeTimeSeries(ts: TimeSeries) {
        runOnEventThread {
            dataset.removeSeries(ts.series)
            timeSeriesList.remove(ts)
        }
        events.timeSeriesRemoved.fire(ts)
    }

    /**
     * Removes the last data source from the chart.
     */
    fun removeLastTimeSeries() {
        if (timeSeriesList.size > 0) {
            removeTimeSeries(timeSeriesList[timeSeriesList.size - 1])
        }
    }

    /**
     * The name to used in coupling descriptions.
     */
    override val name: String
        get() = "TimeSeriesPlot"

    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    private fun readResolve(): Any {
        events = TimeSeriesEvents()
        dataset = XYSeriesCollection()
        timeSeriesList.forEach { dataset.addSeries(it.series) }
        return this
    }

    override val id: String
        get() = "Time Series"

    companion object: WithXStreamPropertyConverter {

        /**
         * Hard ceiling on points per recurrence matrix: the n-squared rebuild runs on the event
         * thread, so an unbounded value would freeze the interface or exhaust memory.
         */
        const val RECURRENCE_MAX_POINTS_LIMIT = 500

        override val xStreamPropertyConverter = createXStreamPropertyConverter<TimeSeriesModel>(
            marshal = {
                on(TimeSeriesModel::timeSeriesList) { writer, context ->
                    writer.startNode("timeSeriesList")
                    forEach {
                        writer.startNode("timeSeries")
                        context.convertAnother(it.series)
                        writer.endNode()
                    }
                    writer.endNode()
                }
            }, unmarshal = {
                on("timeSeriesList") { reader, context ->
                    while (reader.hasMoreChildren()) {
                        reader.moveDown()
                        val series = context.convertAnother(reader.value, XYSeries::class.java) as XYSeries
                        withConstructedObject {
                            val sts = TimeSeries(series)
                            timeSeriesList.add(sts)
                            dataset.addSeries(sts.series)
                            events.timeSeriesAdded.fire(sts)
                        }
                        reader.moveUp()
                    }
                }

            }
        )
    }

    /**
     * Encapsulates a single time series for scalar couplings to attach to.
     */
    inner class TimeSeries(
        /**
         * The represented time series
         */
        var series: XYSeries
    ) : AttributeContainer {

        /**
         * Label for the series
         */
        var description: String
            get() = series.description
            set(value) {series.description = value}

        /**
         * Which component of a coupled array producer this series follows, as an
         * [org.simbrain.workspace.AttributeComponent.key], or null for a series not driven by an array
         * coupling. See [syncTimeSeries].
         */
        var componentKey: String? = null

        @Consumable
        fun setValue(value: Double) {
            try {
                SwingUtilities.invokeAndWait {
                    series.add(timeSupplier(), value as Number)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
        }

        override val id: String
            get() = description

    }
}

/**
 * How a recurrence plot renders the pairwise distances between a series' states.
 */
enum class RecurrenceMode(private val label: String) {
    THRESHOLD("Threshold"), SPECTRUM("Spectrum");

    override fun toString() = label
}

/**
 * Which plots the time series window shows, so the user can focus on the line chart, the recurrence
 * structure, or watch both at once.
 */
enum class RecurrenceView(private val label: String) {
    TIME_SERIES("Time series only"), BOTH("Time series and recurrence"), RECURRENCE("Recurrence only");

    override fun toString() = label
}

fun Workspace.createTimeSeriesModel(): TimeSeriesModel {
    return TimeSeriesModel()
}
