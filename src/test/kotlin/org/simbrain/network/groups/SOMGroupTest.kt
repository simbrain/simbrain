package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.neurongroups.SOMGroup

class SOMGroupTest {

    var net = Network()
    val SOM = SOMGroup(10)

    init {
        net.addNetworkModelsAsync(SOM)
    }
    @Test
    fun `Test create function`() {
        assertEquals(10, SOM.params.numNeurons)
        assertEquals(SOM.params.initNeighborhoodSize, SOM.neighborhoodSize)
        assertEquals(SOM.params.initialLearningRate, SOM.learningRate)
        assertEquals(0.0, SOM.winDistance)
        assertEquals(0.0, SOM.distance)
        assertEquals(0.0, SOM.value)
        assertEquals(null, SOM.winner)
    }

    @Test
    fun `Test copy function`() {
        val SOM2 = SOM.copy()
        net.addNetworkModelsAsync(SOM2)
        // SOMGroup Components
        assertEquals(SOM.params.numNeurons, SOM2.params.numNeurons)
        assertEquals(SOM.neighborhoodSize, SOM2.neighborhoodSize)
        assertEquals(SOM.learningRate, SOM2.learningRate)
        assertEquals(SOM.distance, SOM2.distance)
        assertEquals(SOM.value, SOM2.value)
        assertEquals(SOM.winner, SOM2.winner)
        // SOMGroup Params
        assertEquals(SOM.params.initNeighborhoodSize, SOM2.params.initNeighborhoodSize)
        assertEquals(SOM.params.initialLearningRate, SOM2.params.initialLearningRate)
        assertEquals(SOM.params.learningDecayRate, SOM2.params.learningDecayRate)
        assertEquals(SOM.params.neighborhoodDecayAmount, SOM2.params.neighborhoodDecayAmount)
    }

    @Test
    fun `Test reset function`() {
        repeat(5) {
            net.update()
        }
        SOM.reset()
        assertEquals(SOM.params.initNeighborhoodSize, SOM.neighborhoodSize)
        assertEquals(SOM.params.initialLearningRate, SOM.learningRate)
    }

    @Test
    fun `Test decay function`() {
        // Decay = 0.0
        SOM.params.learningDecayRate = 0.0
        SOM.params.neighborhoodDecayAmount = 0.0

        assertEquals(SOM.params.initialLearningRate, SOM.learningRate)
        assertEquals(SOM.params.initNeighborhoodSize, SOM.neighborhoodSize)

        // Decay with default values
        SOM.params.learningDecayRate = 0.002
        SOM.params.neighborhoodDecayAmount = 0.05

        repeat(3) {
            net.update()
        }
        assertEquals(99.85, SOM.neighborhoodSize, 0.001) // Starts at 100.0
        assertEquals(0.0498, SOM.learningRate, 0.001) // Starts at 0.06
    }

    @Test
    fun `Test winner function`() {

    }
}