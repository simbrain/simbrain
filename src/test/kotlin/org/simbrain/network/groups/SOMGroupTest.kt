package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.subnetworks.SOMNetwork

class SOMNetworkTest {

    var net = Network()
    val som = SOMNetwork(5, 10)

    init {
        net.addNetworkModelsAsync(som)
    }

    @Test
    fun `Test create function`() {
        assertEquals(10, som.som.neuronList.size)
        assertEquals(5, som.inputLayer.neuronList.size)
        assertEquals(som.initNeighborhoodSize, som.neighborhoodSize)
        assertEquals(som.initialLearningRate, som.somLearningRate)
        assertEquals(0.0, som.winDistance)
        assertEquals(null, som.winner)
    }

    @Test
    fun `Test copy function`() {
        val som2 = som.copy()
        net.addNetworkModelsAsync(som2)
        // SOMNetwork runtime state
        assertEquals(som.som.neuronList.size, som2.som.neuronList.size)
        assertEquals(som.inputLayer.neuronList.size, som2.inputLayer.neuronList.size)
        assertEquals(som.neighborhoodSize, som2.neighborhoodSize)
        assertEquals(som.somLearningRate, som2.somLearningRate)
        assertEquals(som.winner, som2.winner)
        // SOMNetwork params
        assertEquals(som.initNeighborhoodSize, som2.initNeighborhoodSize)
        assertEquals(som.initialLearningRate, som2.initialLearningRate)
        assertEquals(som.learningDecayRate, som2.learningDecayRate)
        assertEquals(som.neighborhoodDecayAmount, som2.neighborhoodDecayAmount)
    }

    @Test
    fun `Test reset function`() {
        repeat(5) {
            net.update()
        }
        som.reset()
        assertEquals(som.initNeighborhoodSize, som.neighborhoodSize)
        assertEquals(som.initialLearningRate, som.somLearningRate)
    }

    @Test
    fun `Test decay function`() {
        // Decay = 0.0
        som.learningDecayRate = 0.0
        som.neighborhoodDecayAmount = 0.0

        assertEquals(som.initialLearningRate, som.somLearningRate)
        assertEquals(som.initNeighborhoodSize, som.neighborhoodSize)

        // Decay with default values
        som.learningDecayRate = 0.002
        som.neighborhoodDecayAmount = 0.05

        repeat(3) {
            net.update()
        }
        assertEquals(99.85, som.neighborhoodSize, 0.001)
        assertEquals(0.05964, som.somLearningRate, 0.00001)
    }
}
