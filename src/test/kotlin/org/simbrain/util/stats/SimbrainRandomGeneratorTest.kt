/**
 * SimbrainRandomGenerator reproduces SplitMix64, repeats for a seed, never hands two threads the same state, and
 * gives uniform and Gaussian samples with the expected moments.
 */
package org.simbrain.util.stats

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.sqrt

class SimbrainRandomGeneratorTest {

    @Test
    fun `produces the reference SplitMix64 sequence`() {
        val generator = SimbrainRandomGenerator(1234567)
        assertArrayEquals(
            longArrayOf(6457827717110365317, 3203168211198807973, -8629252141511181193),
            LongArray(3) { generator.nextLong() }
        )
    }

    @Test
    fun `the same seed gives the same sequence`() {
        val a = SimbrainRandomGenerator(42)
        val b = SimbrainRandomGenerator(7).apply { setSeed(42L) }
        assertArrayEquals(DoubleArray(100) { a.nextGaussian() }, DoubleArray(100) { b.nextGaussian() })
    }

    @Test
    fun `threads sharing one instance never draw the same state`() {
        val generator = SimbrainRandomGenerator(1)
        val seen = ConcurrentHashMap.newKeySet<Long>()
        val threads = List(8) { thread { repeat(50_000) { seen.add(generator.nextLong()) } } }
        threads.forEach { it.join() }
        assertEquals(8 * 50_000, seen.size)
    }

    @Test
    fun `gaussian samples have zero mean and unit variance`() {
        val generator = SimbrainRandomGenerator(3)
        val n = 1_000_000
        val samples = DoubleArray(n) { generator.nextGaussian() }
        val mean = samples.average()
        val sd = sqrt(samples.sumOf { (it - mean) * (it - mean) } / n)
        assertEquals(0.0, mean, 0.005)
        assertEquals(1.0, sd, 0.005)
        // About 4.6% of a standard normal lies beyond two standard deviations
        assertEquals(0.0455, samples.count { abs(it) > 2 } / n.toDouble(), 0.002)
    }

    @Test
    fun `uniform doubles stay in the unit interval with mean one half`() {
        val generator = SimbrainRandomGenerator(5)
        val samples = DoubleArray(200_000) { generator.nextDouble() }
        assertTrue(samples.all { it >= 0.0 && it < 1.0 })
        assertEquals(0.5, samples.average(), 0.005)
    }
}
