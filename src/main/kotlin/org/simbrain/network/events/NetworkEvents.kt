package org.simbrain.network.events

import org.simbrain.network.NetworkModel
import org.simbrain.util.Event
import org.simbrain.util.Events2

/**
 * All Network events are defined here. Main docs at [Event].
 */
class NetworkEvents2: Events2() {
    val zoomToFitPage = NoArgEvent(debounce = 20)
    val updated = NoArgEvent()
    val modelAdded = AddedEvent<NetworkModel>()
    val modelRemoved = RemovedEvent<NetworkModel>()
    val updateActionsChanged = NoArgEvent()
    val freeWeightVisibilityChanged = AddedEvent<Boolean>()
}
