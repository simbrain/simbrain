package org.simbrain.network.events

import org.simbrain.network.NetworkModel
import org.simbrain.util.Events2

/**
 * See [Events2].
 */
open class NetworkModelEvents2: Events2() {

    val selected = AddedEvent<NetworkModel>()
    var deleted = RemovedEvent<NetworkModel>()
    var labelChanged = ChangedEvent<String>()
    var clampChanged = NoArgEvent()
    var updated = NoArgEvent()

}