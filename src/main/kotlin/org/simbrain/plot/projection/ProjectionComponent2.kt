package org.simbrain.plot.projection

import com.thoughtworks.xstream.XStream
import org.simbrain.util.DoubleArrayConverter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.projection.DataPoint2
import org.simbrain.util.projection.KDTreeConvertor
import org.simbrain.util.projection.Projector2
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream

class ProjectionComponent2 @JvmOverloads constructor(name: String, val projector: Projector2 = Projector2()):
    WorkspaceComponent(name), AttributeContainer {

    @Consumable
    fun addPoint(newPoint: DoubleArray) {
        if (newPoint.size != projector.dimension) {
            projector.dimension = newPoint.size
        }
        projector.addDataPoint(DataPoint2(newPoint))
    }

    override fun getAttributeContainers(): List<AttributeContainer> {
        val container: MutableList<AttributeContainer> = ArrayList()
        container.add(this)
        return container
    }


    override fun getXML(): String {
        return getProjectorXStream().toXML(projector)
    }

    companion object {

        private fun getProjectorXStream(): XStream {
            val xstream = getSimbrainXStream()
            xstream.registerConverter(KDTreeConvertor())
            xstream.registerConverter(DoubleArrayConverter())
            return xstream
        }

        @JvmStatic
        fun open(input: InputStream?, name: String, format: String): ProjectionComponent2 {
            val proj = getProjectorXStream().fromXML(input) as Projector2
            return ProjectionComponent2(name, proj)
        }
    }

    override fun save(output: OutputStream?, format: String?) {
        getProjectorXStream().toXML(projector, output)
    }

}