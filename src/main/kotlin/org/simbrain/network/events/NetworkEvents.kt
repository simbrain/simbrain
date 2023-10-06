package org.simbrain.network.events

import org.simbrain.network.NetworkModel
import org.simbrain.util.Event
import org.simbrain.util.Events

/**
 * All Network events are defined here. Main docs at [Event].
 */
class NetworkEvents: Events() {
    val zoomToFitPage = NoArgEvent(interval = 20)
    val updated = NoArgEvent()
    val modelAdded = BatchAddedEvent<NetworkModel>(interval = 20)
    val modelRemoved = RemovedEvent<NetworkModel>()
    val updateActionsChanged = NoArgEvent()
    val freeWeightVisibilityChanged = AddedEvent<Boolean>()
    val selected = AddedEvent<List<NetworkModel>>()
}
