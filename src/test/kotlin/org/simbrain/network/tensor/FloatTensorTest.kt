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
        FloatTensor.of(rows, cols, FloatArray(rows * cols) { rng.nextFloat() - 0.5f })

    @Test
    fun `matmul matches naive reference`() {
        val rng = Random(42)
        val a = randomTensor(7, 13, rng)
        val b = randomTensor(13, 5, rng)
        val out = FloatTensor(7, 5)
        matmul(a, b, out)
        assertArrayEquals(naiveMatmul(a, b), out.toFloatArray(), 1e-5f)
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
        assertArrayEquals(expected, out.toFloatArray(), 1e-5f)
    }

    @Test
    fun `matmul with transposed b matches naive on transposed data`() {
        val rng = Random(47)
        val a = randomTensor(5, 8, rng)
        val b = randomTensor(3, 8, rng)
        val bT = FloatTensor(8, 3)
        for (r in 0 until b.rows) for (c in 0 until b.cols) bT[c, r] = b[r, c]
        val out = FloatTensor(5, 3)
        matmul(a, b, out, transposeB = true)
        assertArrayEquals(naiveMatmul(a, bT), out.toFloatArray(), 1e-5f)
    }

    @Test
    fun `matmul with transposed a matches naive on transposed data`() {
        val rng = Random(51)
        val a = randomTensor(8, 5, rng)
        val aT = FloatTensor(5, 8)
        for (r in 0 until a.rows) for (c in 0 until a.cols) aT[c, r] = a[r, c]
        val b = randomTensor(8, 3, rng)
        val out = FloatTensor(5, 3)
        matmul(a, b, out, transposeA = true)
        assertArrayEquals(naiveMatmul(aT, b), out.toFloatArray(), 1e-5f)
    }

    @Test
    fun `matmul with both sides transposed matches naive on transposed data`() {
        val rng = Random(52)
        val a = randomTensor(6, 4, rng)
        val aT = FloatTensor(4, 6)
        for (r in 0 until a.rows) for (c in 0 until a.cols) aT[c, r] = a[r, c]
        val b = randomTensor(3, 6, rng)
        val bT = FloatTensor(6, 3)
        for (r in 0 until b.rows) for (c in 0 until b.cols) bT[c, r] = b[r, c]
        val out = FloatTensor(4, 3)
        matmul(a, b, out, transposeA = true, transposeB = true)
        assertArrayEquals(naiveMatmul(aT, bT), out.toFloatArray(), 1e-5f)
    }

    @Test
    fun `matvec matches matmul with single column`() {
        val rng = Random(44)
        val a = randomTensor(9, 11, rng)
        val x = randomTensor(11, 1, rng)
        val viaGemv = FloatTensor.vector(9)
        matvec(a, x, viaGemv)
        assertArrayEquals(naiveMatmul(a, x), viaGemv.toFloatArray(), 1e-5f)
    }

    @Test
    fun `matvec over leading rows writes only that prefix`() {
        val rng = Random(48)
        val a = randomTensor(6, 4, rng)
        val x = randomTensor(4, 1, rng)
        val out = FloatTensor.vector(6)
        out.fill(-1f)
        matvec(a, x, out, rowCount = 3)
        val full = naiveMatmul(a, x)
        val result = out.toFloatArray()
        for (i in 0 until 3) assertEquals(full[i], result[i], 1e-5f)
        for (i in 3 until 6) assertEquals(-1f, result[i])
    }

    @Test
    fun `transposed matvec over leading rows matches manual sum`() {
        val rng = Random(49)
        val a = randomTensor(5, 3, rng)
        val w = randomTensor(5, 1, rng)
        val out = FloatTensor.vector(3)
        matvec(a, w, out, rowCount = 4, transposeA = true)
        for (c in 0 until 3) {
            var expected = 0f
            for (r in 0 until 4) expected += a[r, c] * w[r, 0]
            assertEquals(expected, out[c, 0], 1e-5f)
        }
    }

    @Test
    fun `axpy and scal compose to a fused update`() {
        val x = FloatTensor.of(1, 4, floatArrayOf(1f, 2f, 3f, 4f))
        val y = FloatTensor.of(1, 4, floatArrayOf(10f, 20f, 30f, 40f))
        scal(0.5f, y)
        axpy(2f, x, y)
        assertArrayEquals(floatArrayOf(7f, 14f, 21f, 28f), y.toFloatArray(), 1e-6f)
    }

    @Test
    fun `hadamard multiplies elementwise`() {
        val a = FloatTensor.of(2, 2, floatArrayOf(1f, 2f, 3f, 4f))
        val b = FloatTensor.of(2, 2, floatArrayOf(5f, 6f, 7f, 8f))
        val out = FloatTensor(2, 2)
        hadamard(a, b, out)
        assertArrayEquals(floatArrayOf(5f, 12f, 21f, 32f), out.toFloatArray(), 0f)
    }

    @Test
    fun `dot matches manual sum`() {
        val x = FloatTensor.of(1, 3, floatArrayOf(1f, 2f, 3f))
        val y = FloatTensor.of(1, 3, floatArrayOf(4f, 5f, 6f))
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
        val aVersion = a.version
        val bVersion = b.version
        dot(a.reshaped(1, 9), b.reshaped(1, 9))
        assertEquals(aVersion, a.version, "a pure read must not bump its operands")
        assertEquals(bVersion, b.version, "a pure read must not bump its operands")
        out.transformInPlace { it * 2f }
        assertEquals(v0 + 2, out.version)
    }

    @Test
    fun `snapshots are immutable`() {
        val t = FloatTensor.of(2, 2, floatArrayOf(1f, 2f, 3f, 4f))
        val snap = t.snapshot()
        t[0, 0] = 99f
        assertEquals(1f, snap[0, 0])
        assertThrows(IllegalStateException::class.java) { snap.fill(0f) }
    }

    @Test
    fun `reshape aliases the same buffer`() {
        val t = FloatTensor(2, 6)
        t.fill(0f)
        val r = t.reshaped(3, 4)
        r[0, 0] = 7f
        assertEquals(7f, t[0, 0])
        assertThrows(IllegalArgumentException::class.java) { t.reshaped(5, 2) }
    }

    @Test
    fun `heap round trip preserves values`() {
        val values = floatArrayOf(1.5f, -2.5f, 3.5f, -4.5f, 5.5f, -6.5f)
        val t = FloatTensor.of(2, 3, values)
        assertArrayEquals(values, t.toFloatArray(), 0f)
        val u = FloatTensor(2, 3)
        u.copyFrom(t)
        assertArrayEquals(values, u.toFloatArray(), 0f)
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
            assertArrayEquals(naiveMatmul(a, b), out.toFloatArray(), 1e-5f)
        }
        assertEquals(before, Blas.numThreads)
    }
}
