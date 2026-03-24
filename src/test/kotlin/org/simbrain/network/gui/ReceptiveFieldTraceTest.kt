package org.simbrain.network.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReceptiveFieldTraceTest {

    @Test
    fun `centered trace index prefers top left padded kernel at upper boundary`() {
        assertEquals(0, centeredTraceIndex(sourceIndex = 0, pad = 1, stride = 1, kernelSize = 3, targetSize = 5))
        assertEquals(1, centeredTraceIndex(sourceIndex = 1, pad = 1, stride = 1, kernelSize = 3, targetSize = 5))
    }

    @Test
    fun `centered trace index handles strided convolution`() {
        assertEquals(0, centeredTraceIndex(sourceIndex = 1, pad = 1, stride = 2, kernelSize = 3, targetSize = 3))
        assertEquals(1, centeredTraceIndex(sourceIndex = 2, pad = 1, stride = 2, kernelSize = 3, targetSize = 3))
        assertEquals(2, centeredTraceIndex(sourceIndex = 4, pad = 1, stride = 2, kernelSize = 3, targetSize = 3))
    }
}
