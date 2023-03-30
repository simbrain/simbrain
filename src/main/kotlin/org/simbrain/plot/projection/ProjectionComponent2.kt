package org.simbrain.plot.projection

import org.simbrain.util.projection.DataPoint2
import org.simbrain.util.projection.Projector2
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.WorkspaceComponent
import java.io.OutputStream

class ProjectionComponent2(name: String): WorkspaceComponent(name), AttributeContainer {

    val projector = Projector2()

    override fun save(output: OutputStream?, format: String?) {
        TODO("Not yet implemented")
    }

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
}