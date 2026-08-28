/**
 * Tests for the update loop's lifecycle hardening: stop escalation cancelling an iteration stuck in a
 * suspend attribute, the per-coupling failure policy that keeps the rest of the couplings updating,
 * and the rejection of blocking iteration on the event dispatch thread.
 */
package org.simbrain.workspace

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.simbrain.workspace.couplings.CouplingFailure
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.SwingUtilities

class LifecycleTestContainer : AttributeContainer {
    override val id = "Lifecycle container"

    val hangReached = CompletableDeferred<Unit>()
    var received = 0.0
    var deliveries = 0

    @Producible
    fun produceScalar() = 1.5

    @Consumable
    suspend fun consumeAndHang(value: Double) {
        hangReached.complete(Unit)
        awaitCancellation()
    }

    @Consumable
    fun consumeAndFail(value: Double) {
        throw IllegalStateException("deliberately failing consumer")
    }

    @Consumable
    fun consumeScalar(value: Double) {
        received = value
        deliveries++
    }
}

@Timeout(30)
class WorkspaceLifecycleTest {

    private val workspace = Workspace()

    private val couplingManager
        get() = workspace.couplingManager

    @Test
    fun `a failing coupling does not stop later couplings from updating`() {
        val container = LifecycleTestContainer()
        val failures = CopyOnWriteArrayList<CouplingFailure>()
        couplingManager.events.couplingFailed.on(Dispatchers.Default) { failures.add(it) }
        val failing = with(couplingManager) {
            container.getProducer("produceScalar") couple container.getConsumer("consumeAndFail")
        }
        with(couplingManager) {
            container.getProducer("produceScalar") couple container.getConsumer("consumeScalar")
        }

        runBlocking {
            couplingManager.updateCouplings()
            couplingManager.updateCouplings()
            withTimeout(5000) {
                while (failures.size < 2) delay(10)
            }
        }

        assertEquals(2, container.deliveries)
        assertEquals(1.5, container.received)
        assertTrue(failures.all { it.coupling == failing })
        assertTrue(failures.all { it.cause is IllegalStateException })
    }

    @Test
    fun `stopNow cancels an iteration stuck in a suspend attribute`() {
        val container = LifecycleTestContainer()
        with(couplingManager) {
            container.getProducer("produceScalar") couple container.getConsumer("consumeAndHang")
        }
        runBlocking {
            val runJob = launch(Dispatchers.Default) { workspace.updater.run() }
            withTimeout(10_000) { container.hangReached.await() }

            workspace.updater.stopNow()
            withTimeout(10_000) { runJob.join() }
            assertFalse(workspace.updater.isRunning)
        }
    }

    @Test
    fun `a stuck iteration is detectable after a cooperative stop and stopNow rescues it`() {
        val container = LifecycleTestContainer()
        with(couplingManager) {
            container.getProducer("produceScalar") couple container.getConsumer("consumeAndHang")
        }
        runBlocking {
            val runJob = launch(Dispatchers.Default) { workspace.updater.run() }
            withTimeout(10_000) { container.hangReached.await() }

            // Repeated cooperative stops are safe no-ops; the stuck iteration stays visible, which is
            // what lets the GUI stop action escalate on a repeated press
            workspace.stop()
            workspace.stop()
            assertTrue(runJob.isActive, "Cooperative stop should not interrupt the stuck iteration")
            assertTrue(workspace.updater.hasActiveIteration)

            workspace.stopNow()
            withTimeout(10_000) { runJob.join() }
            assertFalse(workspace.updater.isRunning)
        }
    }

    @Test
    fun `blocking iteration on the event dispatch thread is rejected`() {
        var single: Throwable? = null
        var counted: Throwable? = null
        SwingUtilities.invokeAndWait {
            single = runCatching { workspace.simpleIterate() }.exceptionOrNull()
            counted = runCatching { workspace.simpleIterate(2) }.exceptionOrNull()
        }
        assertTrue(single is IllegalStateException, "simpleIterate() on the EDT should be rejected, got $single")
        assertTrue(counted is IllegalStateException, "simpleIterate(n) on the EDT should be rejected, got $counted")
    }
}
