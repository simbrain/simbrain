package org.simbrain.network.update_actions

import org.simbrain.network.core.Network
import org.simbrain.workspace.updater.UpdateAction

/**
 * Buffered update of all network models.  Write values to appropriate buffers.  Then read all values from them,
 * which allows for deterministic updating independently of update order.
 *
 * @author jyoshimi
 */
class BufferedUpdate(private val network: Network) : UpdateAction("Loose neurons (buffered) and synapses", "Buffered update of loose items") {
    override suspend operator fun invoke() {
        network.asyncBufferedUpdate()
    }
}