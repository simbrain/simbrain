package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.connectAllToAll
import org.simbrain.network.core.totalFanInStrength
import org.simbrain.network.neurongroups.CompetitiveGroup
import org.simbrain.network.neurongroups.NeuronGroup

class CompetitiveGroupTest {

    var net = Network()
    val competitive = CompetitiveGroup(2)
    lateinit var weights: List<Synapse>
    val inputs = NeuronGroup(2).apply {
        setClamped(true)
    }

    init {
        with(net) {
            net.addNetworkModels(inputs, competitive)
            weights = connectAllToAll(inputs, competitive, 0.1)
        }
    }

    @Test
    fun `Test copy function`() {
        competitive.params.learningRate = .8
        val competitive2 = competitive.copy()
        net.addNetworkModels(competitive2)
        assertEquals(2, competitive2.neuronList.size)
        assertEquals(competitive.params.updateMethod, competitive2.params.updateMethod)
        assertEquals(competitive.params.learningRate, competitive2.params.learningRate)
        assertEquals(competitive.params.winValue, competitive2.params.winValue)
        assertEquals(competitive.params.loseValue, competitive2.params.loseValue)
        assertEquals(competitive.params.normalizeInputs, competitive2.params.normalizeInputs)
        assertEquals(competitive.params.useLeakyLearning, competitive2.params.useLeakyLearning)
        assertEquals(competitive.params.leakyLearningRate, competitive2.params.leakyLearningRate)
        assertEquals(competitive.params.synpaseDecayPercent, competitive2.params.synpaseDecayPercent)
    }

    @Test
    fun `one node wins and takes win value`() {
        competitive.params.winValue = 2.0
        competitive.params.loseValue = 0.0
        inputs.activations = doubleArrayOf(1.0, 0.0)
        repeat(2) {
            net.update()
        }
        assertEquals(2.0, competitive.activations.sum())
    }

    @Test
    fun `network learns two patterns`() {
        // Pattern 1
        inputs.activations = doubleArrayOf(1.0, 0.0)
        repeat(2) {
            net.update()
        }
        // Pattern 2
        inputs.activations = doubleArrayOf(0.0, 1.0)
        repeat(2) {
            net.update()
        }

        // Test retrieval
        inputs.activations = doubleArrayOf(1.0, 0.0)
        net.update()
        val winner1 = competitive.neuronList[competitive.activations.indexOfFirst { it == 1.0 }]
        inputs.activations = doubleArrayOf(0.0, 0.1)
        net.update()
        val winner2 = competitive.neuronList[competitive.activations.indexOfFirst { it == 1.0 }]
        assertNotEquals(winner1, winner2)
    }

    @Test
    fun `test normalize weights`() {
        competitive.normalizeIncomingWeights()
        assertEquals(1.0,  competitive.getNeuron(0).fanIn.sumOf { it.strength })
        assertEquals(1.0,  competitive.getNeuron(1).fanIn.sumOf { it.strength })
    }

    @Test
    fun `test leaky learning`() {
        // Both winner and loser change in same direction
        //  Since this is initial learning weights are getting larger and not "rebalanced"
        competitive.params.useLeakyLearning = true
        inputs.activations = doubleArrayOf(1.0, 0.0)
        net.update()
        val loser = competitive.neuronList[competitive.activations.indexOfFirst { it == 0.0 }]
        val before = loser.totalFanInStrength()
        net.update()
        val after = loser.totalFanInStrength()
        assertTrue(after > before)
    }
}
