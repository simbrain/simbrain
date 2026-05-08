package org.simbrain.world.nettalk

import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream

class NetTalkComponent @JvmOverloads constructor(
    name: String,
    val nettalk: NetTalk = NetTalk()
) : WorkspaceComponent(name) {

    override val attributeContainers: List<AttributeContainer>
        get() = listOf(nettalk)

    override suspend fun update() {
        nettalk.update()
    }

    override fun save(output: OutputStream, format: String?) {
        getSimbrainXStream().toXML(nettalk, output)
    }

    companion object {
        @JvmStatic
        fun open(input: InputStream?, name: String?, format: String?): NetTalkComponent {
            val nettalk = getSimbrainXStream().fromXML(input) as NetTalk
            return NetTalkComponent(name ?: "NETtalk", nettalk)
        }
    }
}
