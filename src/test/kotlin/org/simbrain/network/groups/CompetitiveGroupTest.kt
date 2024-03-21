package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.clamp
import org.simbrain.network.neurongroups.CompetitiveGroup

class CompetitiveGroupTest {

    var net = Network()
    val competitive = CompetitiveGroup(2)

    init {
        net.addNetworkModelsAsync(competitive)
    }

    @Test
    fun `Test create function`() {
        assertEquals(10, competitive.params.numNeurons)
        assertEquals(competitive.params.DEFAULT_UPDATE_METHOD, competitive.params.updateMethod)
        assertEquals(competitive.params.DEFAULT_LEARNING_RATE, competitive.params.learningRate)
        assertEquals(competitive.params.DEFAULT_WIN_VALUE, competitive.params.winValue)
        assertEquals(competitive.params.DEFAULT_LOSE_VALUE, competitive.params.loseValue)
        assertEquals(competitive.params.DEFAULT_NORM_INPUTS, competitive.params.normalizeInputs)
        assertEquals(competitive.params.DEFAULT_USE_LEAKY, competitive.params.useLeakyLearning)
        assertEquals(competitive.params.DEFAULT_LEAKY_RATE, competitive.params.leakyLearningRate)
        assertEquals(competitive.params.DEFAULT_DECAY_PERCENT, competitive.params.synpaseDecayPercent)
    }

    @Test
    fun `Test copy function`() {
        val competitive2 = competitive.copy()
        net.addNetworkModelsAsync(competitive2)
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
    fun `Test winner node`() {
        val inputlayer = doubleArrayOf(1.0, 0.0)

        //competitive.inputs = inputlayer
        repeat(5) {
            net.update()
        }
        println(competitive.inputs)
        println(competitive.outputs)
        val competitive = CompetitiveGroup(2).apply {
            competitive.outgoingWeights.clamp(true)
        }
    }
}
