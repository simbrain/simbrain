package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NumberKtTest {

    @Test
    fun `test one hot`() {
        var oneHot = getOneHot(2, 10)
        assertEquals(0.0, oneHot[0,0])
        assertEquals(1.0, oneHot[2,0])
        assertEquals(10L, oneHot.size())

        oneHot = getOneHot(2, 10, 2.0)
        assertEquals(2.0, oneHot[2,0])
    }
}

