package org.simbrain.network.events

import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.util.FlowEvents

/**
 * All Network events are defined here. Main docs at [FlowEvents].
 */
class NetworkEvents: FlowEvents() {
    val zoomToFitPage = NoArgEvent(interval = 20)
    val boundsChanged = NoArgEvent(interval = 20)
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
