package org.simbrain.network.events

import org.simbrain.network.core.NetworkModel
import org.simbrain.util.FlowEvents

/**
 * See [FlowEvents].
 */
open class NetworkModelEvents: FlowEvents() {

    val selected = OneArgEvent<NetworkModel>()
    val deleted = AwaitableEvent<NetworkModel>()
    var labelChanged = ChangedEvent<String?>()
    var clampChanged = NoArgEvent()
    var updated = NoArgEvent()
    val updateGraphics = NoArgEvent(interval = 10, timingMode = TimingMode.Throttle)

}
