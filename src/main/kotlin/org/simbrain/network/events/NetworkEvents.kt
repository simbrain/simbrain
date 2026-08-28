package org.simbrain.network.events

import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.util.FlowEvents

/**
 * All Network events are defined here. Main docs at [FlowEvents].
 */
class NetworkEvents: FlowEvents() {
    // Throttle, not debounce: auto-zoom refires these continuously while the network updates, and a
    // debounce never sees a quiet window then — an explicit zoom-to-fit press would starve forever
    val zoomToFitPage = NoArgEvent(interval = 20, timingMode = TimingMode.Throttle)
    val boundsChanged = NoArgEvent(interval = 20, timingMode = TimingMode.Throttle)
    val zoomModeChanged = OneArgEvent<Boolean>()
    val updated = NoArgAwaitableEvent()
    val modelAdded = AwaitableEvent<NetworkModel>()
    val modelRemoved = AwaitableEvent<NetworkModel>()
    val updateActionsChanged = NoArgEvent()
    val freeWeightVisibilityChanged = OneArgEvent<Boolean>()
    val synapseSpikingOnlyVisibilityChanged = OneArgEvent<Boolean>()
    val selected = OneArgEvent<List<NetworkModel>>()
    val batchNodeRemoval = BatchOneArgEvent<ScreenElement>(interval = 10)
}
