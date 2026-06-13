package org.simbrain.world.speechsynthesizer

import org.simbrain.util.FlowEvents

class SpeechSynthesizerEvents : FlowEvents() {
    val speakingChanged = OneArgEvent<String>()
    val transcriptionChanged = NoArgEvent()
    val codecChanged = NoArgEvent()
}
