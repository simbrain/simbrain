package org.simbrain.world.soundworld

import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream

class SoundWorldComponent @JvmOverloads constructor(name: String, val soundWorld: SoundWorld = SoundWorld()): WorkspaceComponent(name) {

    override fun getAttributeContainers(): List<AttributeContainer> {
        return listOf(soundWorld.generator)
    }

    override fun save(output: OutputStream?, format: String?) {
        getSimbrainXStream().toXML(soundWorld, output)
    }

    companion object {
        fun open(input: InputStream?, name: String?, format: String?): SoundWorldComponent {
            val newWorld = getSimbrainXStream().fromXML(input) as SoundWorld
            return SoundWorldComponent(name ?: "??", newWorld)
        }
    }

}