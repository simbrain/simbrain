package org.simbrain.network.events

import org.simbrain.util.FlowEvents

/**
 * See [FlowEvents]
 */
open class LocationEvents: NetworkModelEvents() {
    val locationChanged = NoArgEvent()
}