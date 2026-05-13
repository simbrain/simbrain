package org.simbrain.world.speechsynthesizer

import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream

class SpeechSynthesizerComponent @JvmOverloads constructor(
    name: String,
    val synthesizer: SpeechSynthesizer = SpeechSynthesizer()
) : WorkspaceComponent(name) {

    override val attributeContainers: List<AttributeContainer>
        get() = listOf(synthesizer)

    override fun save(output: OutputStream, format: String?) {
        getSimbrainXStream().toXML(synthesizer, output)
    }

    companion object {
        fun open(input: InputStream?, name: String?, format: String?): SpeechSynthesizerComponent {
            val synthesizer = getSimbrainXStream().fromXML(input) as SpeechSynthesizer
            return SpeechSynthesizerComponent(name ?: "Speech Synthesizer", synthesizer)
        }
    }
}
