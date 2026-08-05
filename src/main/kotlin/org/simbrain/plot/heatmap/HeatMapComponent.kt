package org.simbrain.plot.heatmap

import com.thoughtworks.xstream.XStream
import org.simbrain.util.DoubleArrayConverter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream

class HeatMapComponent @JvmOverloads constructor(
    name: String,
    val model: HeatMapModel = HeatMapModel()
) : WorkspaceComponent(name) {

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
