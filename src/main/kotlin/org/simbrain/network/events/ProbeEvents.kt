package org.simbrain.network.events

/**
 * See [org.simbrain.util.FlowEvents]
 */
class ProbeEvents: SupervisedModelEvents() {
    val stalenessChanged = NoArgEvent()
}
