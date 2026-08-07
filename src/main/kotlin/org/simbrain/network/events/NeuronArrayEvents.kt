package org.simbrain.network.events

import org.simbrain.util.FlowEvents

/**
 * See [FlowEvents].
 */
class NeuronArrayEvents: LocationEvents() {
    val visualPropertiesChanged = NoArgEvent()
    val updateRuleChanged = NoArgEvent()

    /**
     * The array's per-neuron labels changed.
     */
    val labelArrayChanged = NoArgEvent()
}