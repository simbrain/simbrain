package org.simbrain.network.events

import org.simbrain.util.Events2

/**
 * See [Events2].
 */
class NeuronArrayEvents2: LocationEvents2() {
    val gridModeChanged = NoArgEvent()
    val updateRuleChanged = NoArgEvent()
}