package org.simbrain.network.tensor

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.random.Random

class FloatTensorTest {

    private fun naiveMatmul(a: FloatTensor, b: FloatTensor): FloatArray {
        val out = FloatArray(a.rows * b.cols)
        for (i in 0 until a.rows) {
            for (j in 0 until b.cols) {
                var sum = 0f
                for (k in 0 until a.cols) {
                    sum += a[i, k] * b[k, j]
                }
                out[i * b.cols + j] = sum
            }
        }
        return out
    }

    private fun randomTensor(rows: Int, cols: Int, rng: Random) =
        FloatTensor(rows, cols, FloatArray(rows * cols) { rng.nextFloat() - 0.5f })

    @Test
    fun `matmul matches naive reference`() {
        val rng = Random(42)
        val a = randomTensor(7, 13, rng)
        val b = randomTensor(13, 5, rng)
        val out = FloatTensor(7, 5)
        matmul(a, b, out)
        assertArrayEquals(naiveMatmul(a, b), out.data, 1e-5f)
    }

    @Test
    fun `matmul beta accumulates into destination`() {
        val rng = Random(43)
        val a = randomTensor(4, 6, rng)
        val b = randomTensor(6, 3, rng)
        val out = FloatTensor(4, 3)
        out.fill(2f)
        matmul(a, b, out, alpha = 1f, beta = 1f)
        val expected = naiveMatmul(a, b).map { it + 2f }.toFloatArray()
        assertArrayEquals(expected, out.data, 1e-5f)
    }

    @Test
    fun `matvec matches matmul with single column`() {
        val rng = Random(44)
        val a = randomTensor(9, 11, rng)
        val x = randomTensor(11, 1, rng)
        val viaGemv = FloatTensor.vector(9)
        matvec(a, x, viaGemv)
        assertArrayEquals(naiveMatmul(a, x), viaGemv.data, 1e-5f)
    }

    @Test
    fun `axpy and scal compose to a fused update`() {
        val x = FloatTensor(1, 4, floatArrayOf(1f, 2f, 3f, 4f))
        val y = FloatTensor(1, 4, floatArrayOf(10f, 20f, 30f, 40f))
        scal(0.5f, y)
        axpy(2f, x, y)
        assertArrayEquals(floatArrayOf(7f, 14f, 21f, 28f), y.data, 1e-6f)
    }

    @Test
    fun `hadamard multiplies elementwise`() {
        val a = FloatTensor(2, 2, floatArrayOf(1f, 2f, 3f, 4f))
        val b = FloatTensor(2, 2, floatArrayOf(5f, 6f, 7f, 8f))
        val out = FloatTensor(2, 2)
        hadamard(a, b, out)
        assertArrayEquals(floatArrayOf(5f, 12f, 21f, 32f), out.data, 0f)
    }

    @Test
    fun `dot matches manual sum`() {
        val x = FloatTensor(1, 3, floatArrayOf(1f, 2f, 3f))
        val y = FloatTensor(1, 3, floatArrayOf(4f, 5f, 6f))
        assertEquals(32f, dot(x, y), 1e-6f)
    }

    @Test
    fun `mutating ops bump version and reads do not`() {
        val rng = Random(45)
        val a = randomTensor(3, 3, rng)
        val b = randomTensor(3, 3, rng)
        val out = FloatTensor(3, 3)
        val v0 = out.version
        matmul(a, b, out)
        assertEquals(v0 + 1, out.version)
        dot(a.reshaped(1, 9), b.reshaped(1, 9))
        assertEquals(v0 + 1, out.version)
        out.transformInPlace { it * 2f }
        assertEquals(v0 + 2, out.version)
    }

    @Test
    fun `snapshots are immutable`() {
        val t = FloatTensor(2, 2, floatArrayOf(1f, 2f, 3f, 4f))
        val snap = t.snapshot()
        t[0, 0] = 99f
        assertEquals(1f, snap[0, 0])
        assertThrows(IllegalStateException::class.java) { snap.fill(0f) }
    }

    @Test
    fun `reshape aliases the same buffer`() {
        val t = FloatTensor(2, 6)
        val r = t.reshaped(3, 4)
        r[0, 0] = 7f
        assertEquals(7f, t[0, 0])
        assertThrows(IllegalArgumentException::class.java) { t.reshaped(5, 2) }
    }

    @Test
    fun `shape mismatches are rejected`() {
        val a = FloatTensor(2, 3)
        val b = FloatTensor(2, 3)
        assertThrows(IllegalArgumentException::class.java) { matmul(a, b, FloatTensor(2, 3)) }
        assertThrows(IllegalArgumentException::class.java) { matvec(a, FloatTensor.vector(2), FloatTensor.vector(2)) }
        assertThrows(IllegalArgumentException::class.java) { axpy(1f, a, FloatTensor(3, 3)) }
    }

    @Test
    fun `blas thread count is settable and restored by withThreads`() {
        val before = Blas.numThreads
        Blas.withThreads(1) {
            assertEquals(1, Blas.numThreads)
            val rng = Random(46)
            val a = randomTensor(8, 8, rng)
            val b = randomTensor(8, 8, rng)
            val out = FloatTensor(8, 8)
            matmul(a, b, out)
            assertArrayEquals(naiveMatmul(a, b), out.data, 1e-5f)
        }
        assertEquals(before, Blas.numThreads)
    }
}
