package org.simbrain.world.nettalk

import org.simbrain.util.Events

class NetTalkEvents : Events() {
    val textChanged = NoArgEvent()
    val positionChanged = NoArgEvent()
    val decoded = NoArgEvent()
    val transcriptionChanged = NoArgEvent()
    val audioModeChanged = NoArgEvent()
    val audioSegmentChanged = NoArgEvent()
}
