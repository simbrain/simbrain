package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import smile.math.matrix.Matrix
import kotlin.math.ln

class SmileUtilsTest {

    val testMatrix = Matrix.of(arrayOf(
        doubleArrayOf(1.0, 2.0, 3.0),
        doubleArrayOf(4.0, 5.0, 6.0),
        doubleArrayOf(7.0, 8.0, 9.0)
    ))

    val nonSquareMatrix = Matrix.of(arrayOf(
        doubleArrayOf(1.0, 2.0, 3.0),
        doubleArrayOf(4.0, 5.0, 6.0),
        doubleArrayOf(7.0, 8.0, 9.0),
        doubleArrayOf(10.0, 11.0, 12.0)
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
        val rmt = nonSquareMatrix.rowVectorTransposed(0)
        assertEquals(3, rmt.nrow())
        assertEquals(1, rmt.ncol())
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), rmt.toDoubleArray())
    }

    @Test
    fun `test broadcasting a vector across columns`() {
        val vector = doubleArrayOf(0.0, 1.0, 2.0).toColumnVector()
        val result = testMatrix.scaleColumns(vector)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), result.col(0))
        assertArrayEquals(doubleArrayOf(2.0, 5.0, 8.0), result.col(1))
        assertArrayEquals(doubleArrayOf(6.0, 12.0, 18.0), result.col(2))
    }

    @Test
    fun `test broadcasting a vector across rows`() {
        val vector = doubleArrayOf(0.0, 1.0, 2.0).toColumnVector()
        val result = testMatrix.scaleRows(vector)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), result.row(0))
        assertArrayEquals(doubleArrayOf(4.0, 5.0, 6.0), result.row(1))
        assertArrayEquals(doubleArrayOf(14.0, 16.0, 18.0), result.row(2))
    }

    @Test
    fun `test broadcasting multiplication on non square matrix`() {
        val vector = doubleArrayOf(0.0, 1.0, 2.0).toColumnVector()
        val result = nonSquareMatrix.scaleColumns(vector)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0, 0.0), result.col(0))
        assertArrayEquals(doubleArrayOf(2.0, 5.0, 8.0, 11.0), result.col(1))
        assertArrayEquals(doubleArrayOf(6.0, 12.0, 18.0, 24.0), result.col(2))
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

    @Test
    fun shiftUpAndPadEndWithZero() {
        assertArrayEquals(doubleArrayOf(4.0, 5.0, 6.0), nonSquareMatrix.shiftUpAndPadEndWithZero().row(0))
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), nonSquareMatrix.shiftUpAndPadEndWithZero().row(nonSquareMatrix.nrow() - 1))
    }

    @Test
    fun `test add operator`() {
        val a = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        val b = arrayOf(
            doubleArrayOf(5.0, 6.0),
            doubleArrayOf(7.0, 8.0)
        ).toMatrix()
        val c = a + b
        assertArrayEquals(doubleArrayOf(6.0, 8.0), c.row(0))
        assertArrayEquals(doubleArrayOf(10.0, 12.0), c.row(1))
    }

    @Test
    fun `test minus operator`() {
        val a = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        val b = arrayOf(
            doubleArrayOf(5.0, 6.0),
            doubleArrayOf(7.0, 8.0)
        ).toMatrix()
        val c = a - b
        assertArrayEquals(doubleArrayOf(-4.0, -4.0), c.row(0))
        assertArrayEquals(doubleArrayOf(-4.0, -4.0), c.row(1))
    }

    @Test
    fun `test scalar multiplication operator`() {
        val a = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        val b = a * 2.0
        assertArrayEquals(doubleArrayOf(2.0, 4.0), b.row(0))
        assertArrayEquals(doubleArrayOf(6.0, 8.0), b.row(1))
        val c = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        val d = -2.0 * c
        assertArrayEquals(doubleArrayOf(-2.0, -4.0), d.row(0))
        assertArrayEquals(doubleArrayOf(-6.0, -8.0), d.row(1))
    }

    @Test
    fun `test cross entropy loss`() {
        val t1 =  doubleArrayOf(0.0, 1.0, 0.0).toColumnVector()
        val a1 =  doubleArrayOf(0.2, .7, 0.1).toColumnVector()
        assertEquals(0.0, crossEntropy(t1, t1), 0.001)
        assertEquals(-ln(.7), crossEntropy(a1, t1))
    }

    @Test
    fun `test validate column vector`(){
        val a = Matrix(4, 1)
        val b = Matrix(1, 4)
        assertDoesNotThrow{a.validateColumnVector()}
        assertThrows<Error>{b.validateColumnVector()}
    }

    @Test
    fun `test copy matrix using copyFrom`(){
        val a = Matrix(3, 3)
        a.copyFrom(testMatrix)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), a.row(0))
        assertArrayEquals(doubleArrayOf(4.0, 5.0, 6.0), a.row(1))
        assertArrayEquals(doubleArrayOf(7.0, 8.0, 9.0), a.row(2))

        a.copyFrom(nonSquareMatrix, allowShapeMismatch = true)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), a.row(0))
        assertArrayEquals(doubleArrayOf(4.0, 5.0, 6.0), a.row(1))
        assertArrayEquals(doubleArrayOf(7.0, 8.0, 9.0), a.row(2))
    }

    @Test
    fun `test reshape matrix`(){
        val a = testMatrix.reshape(2,3)
        assertArrayEquals(doubleArrayOf(1.0,2.0,3.0), a.row(0))
        assertArrayEquals(doubleArrayOf(4.0,5.0,6.0), a.row(1))
        assertEquals(2, a.nrow())
        assertEquals(3, a.ncol())
        assertThrows<IllegalArgumentException>{a.validateSameShape(testMatrix)}
        val b = a.reshape(3,3)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), b.row(2))
        val c = testMatrix.reshape(4,2)
    }

    @Test
    fun `test shapeString`(){
        assertEquals("(3,3)", testMatrix.shapeString)
        assertEquals("(4,3)", nonSquareMatrix.shapeString)
    }

    @Test
    fun `test clip`(){
        val a = Matrix(3, 3)
        a.copyFrom(testMatrix)
        a.clip(2.0,7.0)
        assertArrayEquals(doubleArrayOf(2.0, 2.0, 3.0), a.row(0))
        assertArrayEquals(doubleArrayOf(7.0, 7.0, 7.0), a.row(2))
    }

    @Test
    fun `test shifting`() {
        assertArrayEquals(doubleArrayOf(3.0, 1.0, 2.0), testMatrix.shiftRight().row(0))
        assertArrayEquals(doubleArrayOf(5.0, 6.0, 4.0), testMatrix.shiftRight(2).row(1))
        assertArrayEquals(doubleArrayOf(4.0, 5.0, 6.0), testMatrix.shiftUp().row(0))
        assertArrayEquals(doubleArrayOf(7.0, 8.0, 9.0), testMatrix.shiftUp(-1).row(0))
        assertArrayEquals(doubleArrayOf(7.0, 8.0, 9.0), testMatrix.shiftUp(2).row(0))
    }

    @Test
    fun `test setrow and setrowconstant`(){
        val a = Matrix(3,3)
        a.copyFrom(testMatrix)
        a.setRowConstant(0, 10.0)
        assertArrayEquals(doubleArrayOf(10.0,10.0,10.0), a.row(0))
        a.setRow(1, doubleArrayOf(11.0, 12.0, 13.0))
        assertArrayEquals(doubleArrayOf(11.0, 12.0, 13.0), a.row(1))
        assertThrows<ArrayIndexOutOfBoundsException>{a.setRowConstant(3, 10.0)}
        assertThrows<ArrayIndexOutOfBoundsException>{a.setRow(3, doubleArrayOf(0.0,0.0,0.0))}
    }

    @Test
    fun `test flatten matrix`(){
        assertEquals(9, testMatrix.flatten().size)
    }

    @Test
    fun `test double array to matrix`(){
        val colVector =  testMatrix.flatten().toColumnVector()
        assertEquals(9, colVector.nrow())
        assertEquals(1, colVector.ncol())
    }

    @Test
    fun `test appendRow`(){
        val a = testMatrix.appendRow(doubleArrayOf(10.0, 11.0, 12.0))
        assertEquals(4, a.nrow())
        assertArrayEquals(doubleArrayOf(10.0, 11.0, 12.0), a.row(3))
    }

    @Test
    fun `test matrix building shortcut`() {
        val m1 = Matrix.of(arrayOf(doubleArrayOf(1.0,2.0,3.0)))
        val m2 = matrix[1,3](1,2,3)
        assertArrayEquals(m1.flatten(), m2.flatten())
    }


}
