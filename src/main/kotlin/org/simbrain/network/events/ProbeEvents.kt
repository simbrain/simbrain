package org.simbrain.network.events

/**
 * See [org.simbrain.util.FlowEvents]
 */
class ProbeEvents: LocationEvents() {
    val stalenessChanged = NoArgEvent()
}
