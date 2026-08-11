package org.simbrain.workspace

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

/**
 * A consumer that blocks on the event thread the way the chart models do, so that updating it while the
 * event thread is reading the coupling list reproduces the lock ordering between the two.
 */
class EventThreadBoundContainer : AttributeContainer {
    override val id = "Event thread bound"
    var received = 0

    @Consumable
    fun setValue(value: Double) {
        SwingUtilities.invokeAndWait { received++ }
    }
}

class CouplingUpdateDeadlockTest {

    @Test
    fun `updating couplings does not deadlock against the event thread reading them`() {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Network")
        workspace.addWorkspaceComponent(networkComponent)
        val neuron = Neuron()
        networkComponent.network.addNetworkModelAsync(neuron)
        val consumer = EventThreadBoundContainer()

        with(workspace.couplingManager) {
            neuron.getProducer("getActivation") couple consumer.getConsumer("setValue")
        }

        // The event thread reads the coupling list, as a plot naming itself from its couplings does, while
        // the update thread is pushing values into a consumer that has to reach the event thread
        val stop = CountDownLatch(1)
        val reader = Thread {
            while (stop.count > 0L) {
                SwingUtilities.invokeAndWait { workspace.couplingManager.couplings.size }
            }
        }
        reader.start()

        val finished = Thread {
            repeat(300) {
                workspace.couplingManager.updateCouplings()
                // Keep the cached list invalidated so each read has to rebuild it, which is what takes the lock
                with(workspace.couplingManager) {
                    val extra = Neuron().also { n -> networkComponent.network.addNetworkModelAsync(n) }
                    val coupling = neuron.getProducer("getActivation") couple extra.getConsumer("setActivation")
                    removeCoupling(coupling)
                }
            }
        }
        finished.start()
        finished.join(60_000)
        val completed = !finished.isAlive
        stop.countDown()
        reader.join(10_000)

        assertTrue(completed, "Coupling updates deadlocked against the event thread")
    }
}
