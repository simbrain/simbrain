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
import java.util.concurrent.CountDownLatch
import javax.swing.SwingUtilities

class WeightMatrixEditDialogTest {

    private fun holdEdt(): CountDownLatch {
        val release = CountDownLatch(1)
        val held = CountDownLatch(1)
        SwingUtilities.invokeLater {
            held.countDown()
            release.await()
        }
        held.await()
        return release
    }

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

        val whileOpen = countEdtTasks {
            val release = holdEdt()
            repeat(1000) { wm.events.updated.fire() }
            release.countDown()
            // Sleep rather than poll, so only the work the updates caused is counted
            Thread.sleep(200)
        }
        // The matrix node's own throttled refresh accounts for a few; a per-update editor would add about 1000
        assertTrue(whileOpen < 50, "1000 updates posted $whileOpen EDT tasks")

        SwingUtilities.invokeAndWait { dialog.dispose() }
        UiWork.awaitIdle()
        val afterClose = countEdtTasks {
            repeat(100) { wm.events.updated.fire() }
            Thread.sleep(200)
        }
        assertTrue(afterClose < 10, "a disposed editor still refreshed: 100 updates posted $afterClose EDT tasks")
    }
}
