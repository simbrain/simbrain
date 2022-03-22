package org.simbrain.network.update_actions

import org.simbrain.network.NetworkModel
import org.simbrain.workspace.updater.UpdateAction

/**
 * Action to update a specific network model (NeuronGroup, SynapseGroup, etc.).
 *
 * @author jyoshimi
 */
class UpdateNetworkModel(private val networkModel: NetworkModel) : UpdateAction(
    networkModel.label,
    "Update ${networkModel.label}"
) {
    override suspend operator fun invoke() {
        networkModel.update()
    }
}