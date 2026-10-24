package org.simbrain.network.tensor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlasTest {

    @Test
    fun `default thread count is at most four`() {
        assertTrue(Blas.defaultThreads in 1..4)
    }

    @Test
    fun `an explicit thread count is not replaced by the default`() {
        Blas.withThreads(3) {
            Blas.ensureDefaultThreads()
            assertEquals(3, Blas.numThreads)
        }
    }

    @Test
    fun `withThreads restores a real thread count`() {
        Blas.withThreads(2) { }
        assertTrue(Blas.numThreads >= 1)
    }
}
