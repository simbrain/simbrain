package org.simbrain.network.events

import org.simbrain.util.Events

/**
 * See [Events].
 */
class NeuronArrayEvents: LocationEvents() {
    val visualPropertiesChanged = NoArgEvent()
    val updateRuleChanged = NoArgEvent()
}