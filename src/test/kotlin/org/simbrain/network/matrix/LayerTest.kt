package org.simbrain.network.matrix

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import smile.math.matrix.Matrix

class LayerTest {

    var net = Network()
    var na1 = NeuronArray(2)
    var na2 = NeuronArray(2)
    var wm = WeightMatrix(na1, na2)

    init {
        net.addNetworkModels(na1, na2, wm)
    }

    @Test
    internal fun `getWeightedInputs returns correct values`() {
        na1.activations = Matrix.column(doubleArrayOf(1.0,-1.0))
        wm.diagonalize()
        net.update()
        assertArrayEquals(doubleArrayOf(1.0,-1.0), wm.getSummedPSRs(), 0.0)
    }
}