package org.simbrain.network.events

import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.util.Events2

/**
 * See [Events2].
 */
class NeuronEvents2: LocationEvents2() {

    val activationChanged = ChangedEvent<Double>()
    val spiked = AddedEvent<Boolean>()
    val colorChanged = NoArgEvent()
    val updateRuleChanged = ChangedEvent<NeuronUpdateRule<*, *>>()
}