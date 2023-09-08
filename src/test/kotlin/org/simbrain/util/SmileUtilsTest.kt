package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import smile.math.matrix.Matrix

class SmileUtilsTest {

    val testMatrix = Matrix.of(arrayOf(
        doubleArrayOf(1.0, 2.0, 3.0),
        doubleArrayOf(4.0, 5.0, 6.0),
        doubleArrayOf(7.0, 8.0, 9.0)
    ))

    @Test
    fun `test validate shape`() {
        val a = Matrix(1, 2)
        val b = Matrix(2, 1)
        assertDoesNotThrow{ a.validateSameShape(a) }
        assertThrows<IllegalArgumentException> { a.validateSameShape(b) }

        // Uncomment to check the exception formatting
        // a.validateShape(b)
    }

    @Test
    fun `test row matrix transposed`() {
        val rmt = testMatrix.rowVectorTransposed(1)
        assertEquals(3, rmt.nrow())
        assertEquals(1, rmt.ncol())
    }

    @Test
    fun `test broadcasting multiplication`() {
        val vector = doubleArrayOf(0.0, 1.0, 2.0).toMatrix()
        assertArrayEquals(doubleArrayOf(0.0, 4.0, 14.0), testMatrix.broadcastMultiply(vector).col(0))
        assertArrayEquals(doubleArrayOf(0.0, 5.0, 16.0), testMatrix.broadcastMultiply(vector).col(1))
        assertArrayEquals(doubleArrayOf(0.0, 6.0, 18.0), testMatrix.broadcastMultiply(vector).col(2))
    }


    @Test
    fun testMaxEigen() {
        assertEquals(1.0, Matrix.eye(2).maxEigenvalue())
        assertEquals(2.0, Matrix.eye(2).mul(2.0).maxEigenvalue())
        val ut=  Matrix.of(arrayOf(
            doubleArrayOf(-2.0, 4.0),
            doubleArrayOf(0.0, 7.0)
        ))
        assertEquals(7.0, ut.maxEigenvalue())
    }

    @Test
    fun testSpectralRadius() {
        assertEquals(.9, testMatrix.setSpectralRadius(.9).maxEigenvalue(), .01)
    }
}
