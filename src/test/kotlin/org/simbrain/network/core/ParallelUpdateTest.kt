/**
 * Tests for the parallel buffered network update: chunking, equivalence with a serial update, and the serial
 * fallback for update rules that are not neuron-local.
 */
package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.learningrules.STDPRule
import org.simbrain.network.spikeresponders.StepResponder
import org.simbrain.network.updaterules.IACRule
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.network.updaterules.LinearRule
import kotlin.random.Random

class ParallelUpdateTest {

    private fun buildSpikingNetwork(seed: Int): Pair<Network, List<Synapse>> {
        val rng = Random(seed)
        val net = Network()
        net.timeStep = 0.5
        val neurons = List(300) { i ->
            Neuron(IzhikevichRule().apply {
                backgroundCurrent = 2.0 + 10.0 * (i % 7) / 7.0
                if (i >= 240) { a = 0.1; d = 2.0 }
            })
        }
        val synapses = buildList {
            for (src in neurons.indices) for (tgt in neurons.indices) {
                if (src == tgt || rng.nextDouble() >= 0.2) continue
                add(Synapse(neurons[src], neurons[tgt]).apply {
                    forceSetStrength(if (src < 240) 2.0 * rng.nextDouble() else -4.0 * rng.nextDouble())
                    spikeResponder = StepResponder(1)
                    if (src % 3 == 0) learningRule = STDPRule()
                })
            }
        }
        net.addNetworkModelsAsync(neurons, usePlacementManager = false)
        net.addNetworkModelsAsync(synapses, usePlacementManager = false)
        return net to synapses
    }

    private fun runAndRecord(parallel: Boolean): Pair<DoubleArray, DoubleArray> {
        parallelUpdateOverride = parallel
        try {
            val (net, synapses) = buildSpikingNetwork(7)
            repeat(400) { net.update() }
            return net.flatNeuronList.map { it.activation }.toDoubleArray() to
                    synapses.map { it.strength }.toDoubleArray()
        } finally {
            parallelUpdateOverride = null
        }
    }

    @Test
    fun `parallel update matches serial update exactly`() {
        val (serialActivations, serialStrengths) = runAndRecord(parallel = false)
        val (parallelActivations, parallelStrengths) = runAndRecord(parallel = true)
        assertArrayEquals(serialActivations, parallelActivations)
        assertArrayEquals(serialStrengths, parallelStrengths)
        val initialStrengths = buildSpikingNetwork(7).second.map { it.strength }.toDoubleArray()
        val changed = serialStrengths.indices.count { serialStrengths[it] != initialStrengths[it] }
        assertTrue(changed > 1000, "STDP should have changed many weights, changed $changed")
    }

    @Test
    fun `large network is split into several neuron chunks`() {
        val (net, _) = buildSpikingNetwork(7)
        val plan = NetworkUpdatePlan(net.flatNeuronList + net.freeSynapses)
        assertTrue(plan.neuronChunks.size > 1)
        assertEquals(net.flatNeuronList, plan.neuronChunks.flatten())
    }

    @Test
    fun `rule that is not neuron local forces a serial neuron update`() {
        val neurons = List(200) { Neuron(LinearRule()) }
        val synapses = neurons.flatMap { src -> neurons.filter { it !== src }.map { Synapse(src, it) } }
        val plan = NetworkUpdatePlan(neurons + synapses)
        assertTrue(plan.neuronUpdateChunks.size > 1)
        neurons[17].updateRule = IACRule()
        assertEquals(listOf(neurons), plan.neuronUpdateChunks)
    }

    @Test
    fun `chunks are contiguous and balanced by cost`() {
        val items = List(1000) { it }
        val chunks = chunkByCost(items) { 100 }
        assertTrue(chunks.size > 1)
        assertEquals(items, chunks.flatten())
        val sizes = chunks.map { it.size }
        assertTrue(sizes.max() - sizes.min() <= 1)
    }

    @Test
    fun `cheap work stays in a single chunk`() {
        val items = List(50) { it }
        assertEquals(listOf(items), chunkByCost(items) { 10 })
    }
}
