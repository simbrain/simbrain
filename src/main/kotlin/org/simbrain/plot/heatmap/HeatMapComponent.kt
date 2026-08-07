/**
 * Workspace component wrapper for the heat map, owning its serialization.
 */
package org.simbrain.plot.heatmap

import com.thoughtworks.xstream.XStream
import org.simbrain.util.DoubleArrayConverter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.couplings.Coupling
import java.io.InputStream
import java.io.OutputStream

class HeatMapComponent @JvmOverloads constructor(
    name: String,
    val model: HeatMapModel = HeatMapModel()
) : WorkspaceComponent(name) {

    override var workspace: Workspace
        get() = super.workspace
        set(value) {
            super.workspace = value
            value.couplingManager.events.couplingAdded.on { coupling ->
                updateRowLabels(coupling)
            }
            value.couplingManager.events.attributeContainerChanged.on { container ->
                couplingManager.couplings
                    .filter { it.producer.baseObject === container }
                    .forEach(::updateRowLabels)
            }
        }

    init {
        model.timeSupplier = { workspace.time }
    }

    override val attributeContainers: List<AttributeContainer> get() = listOf(model)

    override fun save(output: OutputStream, format: String?) {
        heatMapXStream.toXML(model, output)
    }

    override fun hasChangedSinceLastSave() = false

    override val xml: String get() = heatMapXStream.toXML(model)

    private fun updateRowLabels(coupling: Coupling) {
        if (coupling.consumer.baseObject === model) {
            val labels = coupling.producer.labelArray.map { it ?: "" }
            if (labels.isNotEmpty() && labels != model.rowLabels) {
                model.setRowLabels(labels)
            }
        }
    }

    companion object {

        fun open(input: InputStream, name: String, format: String?): HeatMapComponent {
            val model = heatMapXStream.fromXML(input) as HeatMapModel
            return HeatMapComponent(name, model)
        }

        val heatMapXStream: XStream
            get() = getSimbrainXStream().apply { registerConverter(DoubleArrayConverter()) }
    }
}
