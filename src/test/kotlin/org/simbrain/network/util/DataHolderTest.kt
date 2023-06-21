package org.simbrain.network.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.util.toDoubleArray
import org.simbrain.util.toMatrix

class DataHolderTest {

    @Test
    fun `test copy of biased data`() {
        val bd = BiasedScalarData(1.0)
        val bd_copy = bd.copy()
        assertEquals(1.0, bd_copy.bias)
    }

    @Test
    fun `test copy of biased matrix data`() {
        val bm = BiasedMatrixData(2)
        bm.biases = doubleArrayOf(1.0, -1.0).toMatrix()
        val bm_copy = bm.copy()
        assertArrayEquals(doubleArrayOf(1.0, -1.0), bm_copy.biases.toDoubleArray())
    }
}