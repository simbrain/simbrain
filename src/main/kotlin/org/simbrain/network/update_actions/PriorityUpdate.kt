package org.simbrain.network.update_actions

import org.simbrain.network.core.Network
import org.simbrain.workspace.updater.UpdateAction

/**
 * Loose neurons (neurons not in groups) are updated in accordance with an
 * ordered priority list. User sets the priority for each neuron. Default
 * priority value is 0. Elements with smaller priority value are updated first.
 *
 * @author jyoshimi
 */
class PriorityUpdate(private val network: Network): UpdateAction("Loose neurons (priority) and synapses", "Priority update of loose items") {
    override suspend fun invoke() {
        network.updateNeuronsByPriority()
        network.updateAllButNeurons()
    }
}