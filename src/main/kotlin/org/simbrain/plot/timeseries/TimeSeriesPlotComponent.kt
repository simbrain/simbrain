package org.simbrain.plot.timeseries

import com.thoughtworks.xstream.XStream
import kotlinx.coroutines.Dispatchers
import org.simbrain.plot.XYSeriesConverter
import org.simbrain.plot.timeseries.TimeSeriesModel.TimeSeries
import org.simbrain.util.DoubleArrayConverter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producer
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.couplings.Coupling
import java.io.InputStream
import java.io.OutputStream

class TimeSeriesPlotComponent @JvmOverloads constructor(name: String, val model: TimeSeriesModel = TimeSeriesModel()) : WorkspaceComponent(name) {

    override var workspace: Workspace
        get() = super.workspace
        set(workspace) {
            // Workspace object is not available in the constructor.
            super.workspace = workspace

            workspace.couplingManager.events.couplingAdded.on { c: Coupling ->
                // A new array coupling is being added to this time series
                if (c.consumer.baseObject === model) {
                    applyProducerLabels(c.producer)
                }
            }

            workspace.couplingManager.events.attributeContainerChanged.on { container ->
                workspace.couplingManager.couplings
                    .filter { it.consumer.baseObject === model && it.producer.baseObject === container }
                    .forEach { applyProducerLabels(it.producer) }
            }

            model.events.timeSeriesAdded.on(Dispatchers.Default) { addedContainer: TimeSeries ->
                this.fireAttributeContainerAdded(addedContainer)
            }

            model.events.timeSeriesRemoved.on(Dispatchers.Default) { removedContainer: TimeSeries ->
                this.fireAttributeContainerRemoved(removedContainer)
            }
        }

    /**
     * Name the model's series after the producer's label array, e.g. neuron labels: renaming in place when the
     * series count already matches (preserving collected data), rebuilding otherwise. Duplicate labels are
     * disambiguated with a positional suffix.
     */
    private fun applyProducerLabels(producer: Producer) {
        val rawLabels = producer.labelArray.map { it ?: "" }
        if (rawLabels.isEmpty()) return
        val duplicateCounts = rawLabels.groupingBy { it }.eachCount()
        val timesSeen = HashMap<String, Int>()
        val labels = rawLabels.map { label ->
            val index = timesSeen.merge(label, 1, Int::plus)!! - 1
            if ((duplicateCounts[label] ?: 0) > 1) "$label[$index]" else label
        }
        when {
            labels == model.timeSeriesList.map { it.description } -> {}
            labels.size == model.timeSeriesList.size -> model.renameTimeSeries(labels)
            else -> {
                model.removeAllTimeSeries()
                labels.forEach { model.addTimeSeries(it) }
            }
        }
    }

    override val attributeContainers: List<AttributeContainer>
        get() {
            val containers: MutableList<AttributeContainer> = ArrayList()
            containers.add(model)
            containers.addAll(model.timeSeriesList)
            return containers
        }

    fun addTimeSeries(name: String) = model.addTimeSeries(name)

    override fun save(output: OutputStream, format: String?) {
        timeSeriesXStream.toXML(model, output)
    }

    override fun hasChangedSinceLastSave(): Boolean {
        return false
    }

    override val xml: String
        get() = timeSeriesXStream.toXML(model)

    init {
        model.timeSupplier = { workspace.time }
    }

    companion object {
        /**
         * Opens a saved time series plot.
         *
         * @param input  stream
         * @param name   name of file
         * @param format format
         * @return bar chart component to be opened
         */
        fun open(input: InputStream, name: String, format: String?): TimeSeriesPlotComponent {
            val dataModel = timeSeriesXStream.fromXML(input) as TimeSeriesModel
            return TimeSeriesPlotComponent(name, dataModel)
        }

        val timeSeriesXStream: XStream
            get() {
                val xstream = getSimbrainXStream()
                xstream.registerConverter(DoubleArrayConverter())
                xstream.registerConverter(XYSeriesConverter())
                return xstream
            }
    }
}
