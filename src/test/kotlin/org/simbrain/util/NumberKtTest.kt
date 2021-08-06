package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NumberKtTest {

    @Test
    fun `test one hot matrix`() {
        var oneHot = getOneHotMat(2, 10)
        assertEquals(0.0, oneHot[0,0])
        assertEquals(1.0, oneHot[2,0])
        assertEquals(10L, oneHot.size())

        oneHot = getOneHotMat(2, 10, 2.0)
        assertEquals(2.0, oneHot[2,0])
    }

    @Test
    fun `test one hot array`() {
        var oneHot = getOneHotArray(2, 10)
        assertEquals(0.0, oneHot[0])
        assertEquals(1.0, oneHot[2])
        assertEquals(10, oneHot.size)

        oneHot = getOneHotArray(2, 10, 2.0)
        assertEquals(2.0, oneHot[2])
    }
}

