package org.simbrain.network

import org.junit.jupiter.api.Test
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.trainers.Xavier
import kotlin.math.sqrt

class WeightInitializationStrategiesTest {

    @Test
    fun `test Xavier`() {
        val numInputs = 30
        val numOutputs = 10
        val na1 = NeuronArray(numInputs)
        val na2 = NeuronArray(numOutputs)
        val wm = WeightMatrix(na1, na2)
        val xavier = Xavier(42L)
        xavier.initializeWeights(wm)

        val amplitude = sqrt(6.0 / (numInputs + numOutputs))

        assert(wm.weightArray.all { it in -amplitude..amplitude })

    }

}