package org.simbrain.network.matrix

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import smile.math.matrix.Matrix

class LayerTest {

    var net = Network()
    var na1 = NeuronArray(net, 2)
    var na2 = NeuronArray(net, 2)
    var wm = WeightMatrix(net, na1, na2)

    @Test
    internal fun `getWeightedInputs returns correct values`() {
        na1.activations = Matrix.column(doubleArrayOf(1.0,-1.0))
        wm.diagonalize()
        assertArrayEquals(doubleArrayOf(1.0,-1.0), wm.output.col(0), 0.0)
    }
}