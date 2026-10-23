/**
 * Tests for [NetworkModelList]'s cached orderings and update plan, including a model added while another thread
 * reads the plan, as the update thread does during a run.
 */
package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.updaterules.LinearRule
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class NetworkModelListTest {

    @Test
    fun `update plan follows adds and removes`() {
        val list = NetworkModelList()
        val first = Neuron(LinearRule())
        val second = Neuron(LinearRule())
        list.add(first)
        assertEquals(listOf(first), list.updatePlan.neuronChunks.flatten())
        list.add(second)
        assertEquals(setOf(first, second), list.updatePlan.neuronChunks.flatten().toSet())
        list.remove(first)
        assertEquals(listOf(second), list.updatePlan.neuronChunks.flatten())
    }

    @Test
    fun `update plan includes a model added while another thread reads the plan`() {
        val current = AtomicReference<NetworkModelList?>()
        val readerPasses = AtomicInteger()
        val stop = AtomicReference(false)
        val reader = thread(isDaemon = true) {
            while (!stop.get()) {
                current.get()?.updatePlan
                readerPasses.incrementAndGet()
            }
        }
        try {
            repeat(5000) { trial ->
                val list = NetworkModelList().apply { add(Neuron(LinearRule())) }
                val added = Neuron(LinearRule())
                current.set(list)
                list.add(added)
                current.set(null)
                // Let the reader finish any plan it started from this list before checking it
                val passes = readerPasses.get()
                while (readerPasses.get() < passes + 2) Thread.onSpinWait()
                assertTrue(added in list.updatePlan.neuronChunks.flatten(), "trial $trial: plan misses the added neuron")
            }
        } finally {
            stop.set(true)
            reader.join()
        }
    }
}
