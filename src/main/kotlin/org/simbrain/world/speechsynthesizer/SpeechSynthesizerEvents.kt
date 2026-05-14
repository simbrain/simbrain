package org.simbrain.world.speechsynthesizer

import org.simbrain.util.Events

class SpeechSynthesizerEvents : Events() {
    val speakingChanged = OneArgEvent<String>()
    val transcriptionChanged = NoArgEvent()
    val codecChanged = NoArgEvent()
}
