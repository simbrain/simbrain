package org.simbrain.network.events

import org.simbrain.util.FlowEvents

/**
 * See [FlowEvents].
 */
class NeuronArrayEvents: LocationEvents() {
    val visualPropertiesChanged = NoArgEvent()
    val updateRuleChanged = NoArgEvent()
}