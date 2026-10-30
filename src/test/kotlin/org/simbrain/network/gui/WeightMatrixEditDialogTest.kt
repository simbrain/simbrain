/**
 * The weight matrix editor follows a learning matrix, which reports an update every iteration, with at most one table
 * refresh per frame, and stops listening once the dialog is gone.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.StandardDialog
import org.simbrain.util.UiWork
import org.simbrain.util.countEdtTasks
import org.simbrain.util.withEdtHeld
import javax.swing.SwingUtilities

class WeightMatrixEditDialogTest {

    @Test
    fun `an open editor refreshes once per frame, not once per update, and stops when disposed`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val wm = WeightMatrix(NeuronArray(3), NeuronArray(2))
        network.addNetworkModelsAsync(wm.source as NeuronArray, wm.target as NeuronArray, wm)
        val node = panel.getNode(wm) as WeightMatrixNode
        lateinit var dialog: StandardDialog
        SwingUtilities.invokeAndWait { dialog = node.createEditDialog(listOf(wm))!! }
        UiWork.awaitIdle()

        val whileOpen = countEdtTasks(settleMs = 200) {
            withEdtHeld { repeat(1000) { wm.events.updated.fire() } }
        }
        // The matrix node's own throttled refresh accounts for a few; a per-update editor would add about 1000
        assertTrue(whileOpen < 50, "1000 updates posted $whileOpen EDT tasks")

        SwingUtilities.invokeAndWait { dialog.dispose() }
        UiWork.awaitIdle()
        val afterClose = countEdtTasks(settleMs = 200) {
            repeat(100) { wm.events.updated.fire() }
        }
        assertTrue(afterClose < 10, "a disposed editor still refreshed: 100 updates posted $afterClose EDT tasks")
    }
}
