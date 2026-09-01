package org.simbrain.network.events

import org.simbrain.util.FlowEvents

/**
 * See [FlowEvents]
 */
class GapJunctionEvents : NetworkModelEvents() {
    val conductanceUpdated = NoArgEvent()
    val locationChanged = NoArgEvent()
}
