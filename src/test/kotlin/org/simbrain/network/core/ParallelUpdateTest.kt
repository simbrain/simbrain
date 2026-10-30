/**
 * Tests for the parallel buffered network update: chunking, equivalence with a serial update, and the serial
 * fallback for update rules that are not neuron-local or that share seeded noise.
 */
package org.simbrain.network.core

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.network.learningrules.STDPRule
import org.simbrain.network.spikeresponders.StepResponder
import org.simbrain.network.updaterules.IACRule
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.ProductRule
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.network.updaterules.KuramotoRule
import org.simbrain.util.awaitUntil
import kotlin.random.Random

class ParallelUpdateTest {

    private fun buildSpikingNetwork(seed: Int, spikeProbability: Double = 1.0): Pair<Network, List<Synapse>> {
        val rng = Random(seed)
        val net = Network()
        net.timeStep = 0.5
        net.randomSeed = seed.toLong()
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
                    spikeResponder = StepResponder(1).also { it.spikeProbability = spikeProbability }
                    if (src % 3 == 0) learningRule = STDPRule()
                })
            }
        }
        net.addNetworkModelsAsync(neurons, usePlacementManager = false)
        net.addNetworkModelsAsync(synapses, usePlacementManager = false)
        return net to synapses
    }

    private fun runAndRecord(parallel: Boolean, spikeProbability: Double = 1.0): Pair<DoubleArray, DoubleArray> {
        val (net, synapses) = buildSpikingNetwork(7, spikeProbability)
        net.parallelUpdate = parallel
        repeat(400) { net.update() }
        return net.flatNeuronList.map { it.activation }.toDoubleArray() to
                synapses.map { it.strength }.toDoubleArray()
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
    fun `parallel update with spike probability matches serial update exactly`() {
        val (serialActivations, serialStrengths) = runAndRecord(parallel = false, spikeProbability = 0.5)
        val (parallelActivations, parallelStrengths) = runAndRecord(parallel = true, spikeProbability = 0.5)
        assertArrayEquals(serialActivations, parallelActivations)
        assertArrayEquals(serialStrengths, parallelStrengths)
        val (allSpikesActivations, _) = runAndRecord(parallel = false)
        assertFalse(serialActivations contentEquals allSpikesActivations, "Dropping spikes should change the run")
    }

    @Test
    fun `large network is split into several neuron chunks`() {
        val (net, _) = buildSpikingNetwork(7)
        val plan = NetworkUpdatePlan(net.flatNeuronList + net.freeSynapses)
        assertTrue(plan.neuronChunks.size > 1)
        assertEquals(net.flatNeuronList, plan.neuronChunks.flatten())
    }

    /**
     * Activations after [steps] updates of [count] neurons, each fed by [fanIn] randomly chosen others, with the
     * neurons added to the network in [order]. The synapses are created in the same order either way, so each
     * neuron sums its inputs in the same order; only the order neurons update in differs.
     */
    private fun activationsAddedInOrder(
        rule: () -> NeuronUpdateRule<*, *>,
        order: (List<Neuron>) -> List<Neuron>,
        count: Int = 30,
        fanIn: Int = 3,
        steps: Int = 25
    ): DoubleArray {
        val rng = Random(11)
        val neurons = List(count) { Neuron(rule()).apply { activation = rng.nextDouble(-0.2, 1.0) } }
        val synapses = neurons.flatMap { target ->
            neurons.filter { it !== target }.shuffled(rng).take(fanIn).map { source ->
                Synapse(source, target).apply { forceSetStrength(rng.nextDouble(-1.0, 1.0)) }
            }
        }
        neurons.take(3).forEach { it.addInputValue(0.5) }
        val network = Network()
        network.addNetworkModelsAsync(order(neurons) + synapses, usePlacementManager = false)
        repeat(steps) { network.update() }
        return neurons.map { it.activation }.toDoubleArray()
    }

    @Test
    fun `rules that read their source neurons give the same result in any update order`() {
        val rules = mapOf<String, () -> NeuronUpdateRule<*, *>>(
            "IAC" to { IACRule() },
            "Kuramoto" to { KuramotoRule() },
            "Product" to { ProductRule() }
        )
        rules.forEach { (name, rule) ->
            assertArrayEquals(
                activationsAddedInOrder(rule, { it }),
                activationsAddedInOrder(rule, { it.reversed() }),
                "$name depends on update order"
            )
        }
    }

    @Test
    fun `an iac network updates the same in parallel as serially`() {
        fun run(parallel: Boolean): DoubleArray {
            val rng = Random(5)
            val neurons = List(200) { Neuron(IACRule()).apply { activation = rng.nextDouble(-0.2, 1.0) } }
            val synapses = neurons.flatMap { src ->
                neurons.filter { it !== src }.map { Synapse(src, it).apply { forceSetStrength(rng.nextDouble(-0.1, 0.1)) } }
            }
            val network = Network().apply { parallelUpdate = parallel }
            network.addNetworkModelsAsync(neurons + synapses, usePlacementManager = false)
            assertTrue(network.updatePlan.neuronUpdateChunks.size > 1)
            repeat(30) { network.update() }
            return neurons.map { it.activation }.toDoubleArray()
        }
        assertArrayEquals(run(parallel = false), run(parallel = true))
    }

    @Test
    fun `neurons sharing a rule with seeded noise update serially`() {
        val rule = LinearRule().apply { addNoise = true }
        val neurons = List(200) { Neuron(rule) }
        val synapses = neurons.flatMap { src -> neurons.filter { it !== src }.map { Synapse(src, it) } }
        val network = Network()
        network.addNetworkModelsAsync(neurons + synapses, usePlacementManager = false)
        assertTrue(network.updatePlan.neuronUpdateChunks.size > 1, "Unseeded noise is not reproducible anyway")
        rule.noiseGenerator.randomSeed = 42
        assertEquals(listOf(neurons), network.updatePlan.neuronUpdateChunks)
        neurons.forEach { it.updateRule = rule.copy() }
        assertTrue(
            network.updatePlan.neuronUpdateChunks.size > 1,
            "Neurons with their own rules each draw their own noise"
        )
    }

    @Test
    fun `a synapse given a learning rule after the plan is built learns`() {
        val network = Network()
        val source = Neuron().apply { activation = 1.0; clamped = true }
        val target = Neuron().apply { activation = 1.0; clamped = true }
        val synapse = Synapse(source, target).apply { forceSetStrength(0.5) }
        network.addNetworkModelsAsync(listOf(source, target, synapse), usePlacementManager = false)
        network.update()

        synapse.learningRule = HebbianRule().apply { learningRate = 0.1 }
        network.update()

        assertEquals(0.6, synapse.strength, 1e-9)
    }

    @Test
    fun `a deleted synapse keeps no network listener on its learning rule`() = runBlocking {
        val network = Network()
        val (source, target) = List(2) { Neuron() }
        val synapse = Synapse(source, target)
        network.addNetworkModels(listOf(source, target, synapse), usePlacementManager = false)
        assertEquals(1, synapse.events.learningRuleUpdated.subscriberCount)

        synapse.delete()
        awaitUntil { synapse.events.learningRuleUpdated.subscriberCount == 0 }
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
