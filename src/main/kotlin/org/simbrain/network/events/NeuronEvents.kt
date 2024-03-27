package org.simbrain.network.events

import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.util.Events

/**
 * See [Events].
 */
class NeuronEvents: LocationEvents() {

    val activationChanged = ChangedEvent<Double>()
    val spiked = AddedEvent<Boolean>()
    val colorChanged = NoArgEvent()
    val updateRuleChanged = BatchChangedEvent<NeuronUpdateRule<*, *>>(interval = 10)
    val priorityChanged = NoArgEvent()
}