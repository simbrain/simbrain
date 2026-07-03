package org.simbrain.network.events

/**
 * See [org.simbrain.util.FlowEvents]
 */
open class SupervisedModelEvents: LocationEvents() {
    val customInfoUpdated = NoArgEvent()
}
