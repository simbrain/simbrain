package org.simbrain.network.events

import org.simbrain.network.NetworkModel
import org.simbrain.util.Events

/**
 * All Network events are defined here. Main docs at [Events].
 */
class NetworkEvents: Events() {
    val zoomToFitPage = NoArgEvent(interval = 20)
    val updated = NoArgEvent()
    val modelAdded = OneArgEvent<NetworkModel>()
    val modelRemoved = OneArgEvent<NetworkModel>()
    val updateActionsChanged = NoArgEvent()
    val freeWeightVisibilityChanged = OneArgEvent<Boolean>()
    val selected = OneArgEvent<List<NetworkModel>>()
}
