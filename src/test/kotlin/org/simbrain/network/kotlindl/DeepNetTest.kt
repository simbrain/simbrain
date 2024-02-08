package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.ConvPadding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class DeepNetTest {

    @Test
    fun `test simple ff`() {
        val deepNet = DeepNet(
            arrayListOf(TFInputLayer(2), TFDenseLayer(5), TFDenseLayer(7)),
            4
        )
        assertEquals(3, deepNet.deepNetLayers.layers.size)
        deepNet.update()
        assertEquals(2, deepNet.inputSize)
        assertEquals(7, deepNet.outputArray.size)
    }

    @Test
    fun `test conv net`() {
        val deepNet = DeepNet(
            arrayListOf(
                TFInputLayer(5, 5, 3),
                TFConv2DLayer().apply {
                    // Number of filters must match channels of previous layer
                    nfilters = 3
                    // Does not seem to care if kernel size does not evenly divide input layer
                    kernelSize = intArrayOf(5,5)
                    // strides = intArrayOf(2,2,2,2)
                    padding = ConvPadding.VALID
                },
                TFFlattenLayer()
            ),
            4
        )
        println(deepNet.outputArray.size)
        deepNet.update()
        assertEquals(3, deepNet.deepNetLayers.layers.size)
    }


}