package org.simbrain.plot.projection

import com.thoughtworks.xstream.XStream
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.projection.DataPoint
import org.simbrain.util.projection.KDTreeConvertor
import org.simbrain.util.projection.Projector
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream

class ProjectionComponent @JvmOverloads constructor(name: String, val projector: Projector = Projector()):
    WorkspaceComponent(name), AttributeContainer {

    @Consumable
    fun addPoint(newPoint: DoubleArray) {
        projector.addDataPoint(DataPoint(newPoint))
    }

    fun addPoint(newPoint: DataPoint) {
        projector.addDataPoint(newPoint)
    }

    @Producible
    fun getCurrentPointActivation() = with(projector) {
        dataset.currentPoint?.let { coloringManager.getActivation(it) } ?: 0.0
    }

    @Consumable
    fun setLabel(label: String?) {
        projector.dataset.currentPoint?.label = label
    }

    override fun getAttributeContainers(): List<AttributeContainer> {
        val container: MutableList<AttributeContainer> = ArrayList()
        container.add(this)
        return container
    }

    override fun getXML(): String {
        return getProjectorXStream().toXML(projector)
    }

    init {
        projector.events.pointUpdated.on(wait = true) {
            projector.coloringManager.updateAllColors()
            projector.dataset.currentPoint?.let { projector.coloringManager.activate(it) }
        }
    }

    companion object {

        private fun getProjectorXStream(): XStream {
            val xstream = getSimbrainXStream()
            xstream.registerConverter(KDTreeConvertor())
            return xstream
        }

        @JvmStatic
        fun open(input: InputStream?, name: String, format: String?): ProjectionComponent {
            val proj = getProjectorXStream().fromXML(input) as Projector
            return ProjectionComponent(name, proj)
        }
    }

    override fun save(output: OutputStream?, format: String?) {
        getProjectorXStream().toXML(projector, output)
    }

    override val id: String get() = name
}