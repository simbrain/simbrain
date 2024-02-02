package org.simbrain.network.events

import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.util.Events

/**
 * See [Events].
 */
class NeuronEvents: LocationEvents() {

    val activationChanged = ChangedEvent<Double>()
    val spiked = AddedEvent<Boolean>()
    val colorChanged = NoArgEvent()
    val updateRuleChanged = ChangedEvent<NeuronUpdateRule<*, *>>()
    val priorityChanged = NoArgEvent()
}