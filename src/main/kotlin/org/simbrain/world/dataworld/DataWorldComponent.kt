package org.simbrain.world.dataworld

import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream

class DataWorldComponent @JvmOverloads constructor(name: String, val dataWorld: DataWorld = DataWorld()): WorkspaceComponent(name) {

    override val attributeContainers: List<AttributeContainer> get() = listOf(dataWorld)

    override fun save(output: OutputStream, format: String?) {
        getSimbrainXStream().toXML(dataWorld, output)
    }

    override suspend fun update() {
        dataWorld.update()
    }

    companion object {
        fun open(input: InputStream?, name: String?, format: String?): DataWorldComponent {
            val newWorld = getSimbrainXStream().fromXML(input) as DataWorld
            return DataWorldComponent(name ?: "??", newWorld)
        }
    }

}