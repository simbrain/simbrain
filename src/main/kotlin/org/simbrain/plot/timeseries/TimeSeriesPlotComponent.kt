package org.simbrain.plot.timeseries

import com.thoughtworks.xstream.XStream
import kotlinx.coroutines.Dispatchers
import org.simbrain.plot.XYSeriesConverter
import org.simbrain.plot.timeseries.TimeSeriesModel.TimeSeries
import org.simbrain.util.DoubleArrayConverter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.*
import java.io.InputStream
import java.io.OutputStream

class TimeSeriesPlotComponent @JvmOverloads constructor(name: String, val model: TimeSeriesModel = TimeSeriesModel()) : WorkspaceComponent(name) {

    /** Whether scalar couplings automatically replace series names with their producers' names. */
    var useScalarCouplingNames = true

    override var workspace: Workspace
        get() = super.workspace
        set(workspace) {
            // Workspace object is not available in the constructor.
            super.workspace = workspace

            onCoupledProducer { consumer, producer ->
                if (consumer === model) {
                    model.syncTimeSeries(producer.displayComponents)
                } else if (consumer is TimeSeries && useScalarCouplingNames) {
                    applyScalarNames()
                }
            }

            model.events.timeSeriesAdded.on(Dispatchers.Default) { addedContainer: TimeSeries ->
                this.fireAttributeContainerAdded(addedContainer)
            }

            model.events.timeSeriesRemoved.on(Dispatchers.Default) { removedContainer: TimeSeries ->
                this.fireAttributeContainerRemoved(removedContainer)
            }
        }

    /**
     * Name the series that are themselves targets of scalar couplings, as produced by plotting a selection of
     * neurons. The producing attribute's name is enough on its own when every such series comes from the same
     * kind of attribute, e.g. all activations; the method name is appended only when it is what distinguishes
     * them, e.g. one neuron's activation plotted alongside its bias. They are named as a set because adding a
     * coupling can change which of those two forms applies to the series already present.
     */
    private fun applyScalarNames() {
        val scalarCouplings = couplingManager.couplings
            .filter { coupling -> model.timeSeriesList.any { it === coupling.consumer.baseObject } }
        if (scalarCouplings.isEmpty()) return
        val methodDistinguishes = scalarCouplings.map { it.producer.simpleMethodName }.toSet().size > 1
        val names = scalarCouplings
            .mapIndexed { index, coupling ->
                val producer = coupling.producer
                AttributeComponent(
                    "$index",
                    if (methodDistinguishes) producer.simpleDescription else producer.containerDisplayName
                )
            }
            .disambiguateNames()
            .map { it.name }
        model.renameEachTimeSeries(scalarCouplings.map { it.consumer.baseObject as TimeSeries }.zip(names))
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
