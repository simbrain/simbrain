package org.simbrain.world.soundworld

import org.simbrain.util.Events

class PhonemeSynthesizerEvents : Events() {
    /** Fires when the currently-playing phoneme string changes (empty string = idle). */
    val speakingChanged = OneArgEvent<String>()
}
