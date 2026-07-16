package org.simbrain.custom_sims.simulations.cogsci

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.world.odorworld.entities.EntityType

class LandmarkCognitiveMapTest {

    @Test
    fun `observing a landmark creates one node`() {
        val graph = LandmarkGraph()

        graph.observe("Flower")

        assertEquals(listOf("Flower"), graph.nodes)
        assertTrue(graph.edges.isEmpty())
    }

    @Test
    fun `new landmark creates directed transition`() {
        val graph = LandmarkGraph()

        graph.observe("Flower")
        graph.observe("Cheese")

        assertEquals(1, graph.edges["Flower" to "Cheese"])
        assertTrue(graph.edges["Cheese" to "Flower"] == null)
    }

    @Test
    fun `repeated transition increments count and reverse is separate`() {
        val graph = LandmarkGraph()

        graph.observe("Flower")
        graph.observe(null)
        graph.observe("Cheese")
        graph.observe(null)
        graph.observe("Flower")
        graph.observe(null)
        graph.observe("Cheese")
        graph.observe(null)
        graph.observe("Flower")

        assertEquals(2, graph.edges["Flower" to "Cheese"])
        assertEquals(2, graph.edges["Cheese" to "Flower"])
    }

    @Test
    fun `remaining at a landmark does not create self edge`() {
        val graph = LandmarkGraph()

        graph.observe("Flower")
        graph.observe("Flower")
        graph.observe("Flower")

        assertTrue(graph.edges.isEmpty())
    }

    @Test
    fun `reset clears nodes and transitions`() {
        val graph = LandmarkGraph()
        graph.observe("Flower")
        graph.observe("Cheese")

        graph.reset()

        assertTrue(graph.nodes.isEmpty())
        assertTrue(graph.edges.isEmpty())
    }

    @Test
    fun `sensory state represents multiple landmarks and background`() {
        val encoder = LandmarkStateEncoder(
            listOf(
                LandmarkDefinition("Flower", 0.0, 0.0, EntityType.Flower),
                LandmarkDefinition("Cheese", 100.0, 0.0, EntityType.Swiss)
            )
        )

        val atFlower = encoder.encode(0.0, 0.0)
        val midpoint = encoder.encode(50.0, 0.0)
        val farAway = encoder.encode(1_000.0, 0.0)

        assertEquals(3, atFlower.size)
        assertEquals(1.0, atFlower[0], 1e-10)
        assertTrue(midpoint[0] > 0.0)
        assertTrue(midpoint[1] > 0.0)
        assertTrue(midpoint[2] > 0.0)
        assertTrue(midpoint[2] < farAway[2])
        assertTrue(farAway[2] > 0.99)
    }

    @Test
    fun `wraparound distance contributes to sensory state`() {
        val encoder = LandmarkStateEncoder(
            landmarks = listOf(LandmarkDefinition("Flower", 10.0, 300.0, EntityType.Flower)),
            worldWidth = 600.0,
            worldHeight = 600.0
        )

        assertTrue(encoder.encode(590.0, 300.0)[0] > 0.98)
        assertEquals("Flower", encoder.nearestLabel(590.0, 300.0))
    }

    @Test
    fun `composition uses main-state-derived readouts including background`() {
        val labels = listOf("Flower", "Gouda", "Background")

        assertEquals(
            "Background (0.80) · Flower (0.20)",
            formatComposition(labels, doubleArrayOf(0.2, 0.05, 0.8))
        )
        assertEquals("Background", formatComposition(labels, doubleArrayOf(0.1, 0.1, 0.1)))
        assertEquals(java.awt.Color(120, 120, 120), dominantReadoutColor(labels, doubleArrayOf(0.2, 0.05, 0.8)))
    }

    @Test
    fun `pretraining is deterministic and produces distributed readouts`() = runBlocking {
        suspend fun pretrainedResult(): Pair<DoubleArray, Double> {
            val network = Network()
            val input = network.addNeuronCollection(3).apply { isClamped = true }
            val hidden = network.addNeuronCollection(4).apply { neuronList.forEach { it.updateRule = SigmoidalRule() } }
            val output = network.addNeuronCollection(3).apply { neuronList.forEach { it.updateRule = SigmoidalRule() } }
            val inputToHidden = SynapseGroup(input, hidden)
            val hiddenToOutput = SynapseGroup(hidden, output)
            val model = SupervisedModel(input, output)
            network.addNetworkModels(input, hidden, output, inputToHidden, hiddenToOutput, model)
            initializeDeterministicWeights(listOf(inputToHidden, hiddenToOutput), listOf(hidden, output))
            val samples = listOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0),
                doubleArrayOf(0.5, 0.5, 0.0)
            )
            val result = model.pretrain(network, samples)
            input.activationArray = samples.last()
            with(network) { model.forwardPass() }
            assertTrue(hidden.activationArray.count { it > 0.05 } > 1)
            assertTrue(output.activationArray.count { it > 0.05 } > 1)
            return inputToHidden.synapses.map { it.strength }.toDoubleArray() to result.reconstructionError
        }

        val (firstWeights, firstError) = pretrainedResult()
        val (secondWeights, secondError) = pretrainedResult()

        assertTrue(firstWeights.contentEquals(secondWeights))
        assertEquals(firstError, secondError, 1e-12)
        assertTrue(firstError < 0.03)
    }
}
