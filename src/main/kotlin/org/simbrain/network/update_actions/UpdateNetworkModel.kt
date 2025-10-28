package org.simbrain.network.update_actions

import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.util.UpdateAction

/**
 * Action to update a specific network model (NeuronGroup, SynapseGroup, etc.).
 *
 * @author jyoshimi
 */
class UpdateNetworkModel(private val networkModel: NetworkModel, val network: Network) : UpdateAction(
    "Update ${networkModel.displayName}",
    "An action that updates ${networkModel.displayName}"
) {
    override suspend fun run() {
        with(network) { networkModel.update() }
    }
}