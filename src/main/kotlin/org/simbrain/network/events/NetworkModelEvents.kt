package org.simbrain.network.events

import org.simbrain.network.NetworkModel
import org.simbrain.util.Events

/**
 * See [Events].
 */
open class NetworkModelEvents: Events() {

    val selected = AddedEvent<NetworkModel>()
    var deleted = RemovedEvent<NetworkModel>()
    var labelChanged = ChangedEvent<String>()
    var clampChanged = NoArgEvent()
    var updated = NoArgEvent()

}