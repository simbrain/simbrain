package org.simbrain.network.events

import org.simbrain.network.matrix.NeuronArray
import org.simbrain.util.Event

/**
 * @see [Event].
 */
open class NeuronArrayEvents(na: NeuronArray) : LocationEvents(na) {

    fun onGridModeChange(handler: Runnable) = "GridModeChange".event(handler)
    fun fireGridModeChange() = "GridModeChange"()

    fun onUpdateRuleChange(handler: Runnable) = "UpdateRuleChange".event(handler)
    fun fireUpdateRuleChange() = "UpdateRuleChange"()

}