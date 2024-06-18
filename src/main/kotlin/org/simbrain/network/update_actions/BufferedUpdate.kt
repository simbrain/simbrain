package org.simbrain.network.update_actions

import org.simbrain.network.core.Network
import org.simbrain.workspace.updater.UpdateAction

/**
 * Buffered update of all network models. Write values to appropriate buffers. Then read all values from them,
 * which allows for deterministic updating independently of update order.
 *
 * @author jyoshimi
 */
class BufferedUpdate(private val network: Network) : UpdateAction("Buffered update", "Buffered update of all top-level network models") {
    override suspend fun run() {
        network.bufferedUpdate()
    }
}