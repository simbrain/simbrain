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
            onCoupledProducer { _, producer ->
                val names = producer.displayNames
                if (names != model.componentNames) {
                    model.setComponentNames(names)
                }
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

    companion object {

        fun open(input: InputStream, name: String, format: String?): HeatMapComponent {
            val model = heatMapXStream.fromXML(input) as HeatMapModel
            return HeatMapComponent(name, model)
        }

        val heatMapXStream: XStream
            get() = getSimbrainXStream().apply { registerConverter(DoubleArrayConverter()) }
    }
}
