package org.simbrain.network.update_actions

import org.simbrain.network.core.Network
import org.simbrain.workspace.updater.UpdateAction

/**
 * Network models are updated in accordance with an
 * ordered priority list. User sets the priority for each neuron. The default
 * priority value is 0. Elements with smaller priority value are updated first.
 *
 * @author jyoshimi
 */
class PriorityUpdate(private val network: Network): UpdateAction("Priority Update", "Priority update of all network models") {
    override suspend fun run() {
        network.updateModelsByPriority()
    }
}