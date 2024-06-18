package org.simbrain.network.update_actions

import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.workspace.updater.UpdateAction

/**
 * Action to update a specific network model (NeuronGroup, SynapseGroup, etc.).
 *
 * @author jyoshimi
 */
class UpdateNetworkModel(private val networkModel: NetworkModel, val network: Network) : UpdateAction(
    networkModel.label,
    "Update ${networkModel.label}"
) {
    override suspend fun run() {
        with(network) { networkModel.update() }
    }
}