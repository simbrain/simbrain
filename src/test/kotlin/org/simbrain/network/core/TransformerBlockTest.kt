package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import smile.math.matrix.Matrix

class TransformerBlockTest {

    var net = Network()
    val sequenceSize = 2
    val blockSize = 3
    val block = TransformerBlock(sequenceSize, blockSize, 10)

    init {
        block.randomize()
        net.addNetworkModels(block)
    }

    @Test
    fun `initial test stub`() {
        block.addInputs(Matrix(sequenceSize, blockSize).apply { fill(1.0)})
        with(net) {
            block.update()
        }
        print(block.selfAttention)
        print(block.activations)
    }

}