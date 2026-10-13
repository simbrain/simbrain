/**
 * The random source behind every [ProbabilityDistribution].
 */
package org.simbrain.util.stats

import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import org.apache.commons.math3.random.RandomGenerator as CommonsRandomGenerator
import java.util.random.RandomGenerator as JavaRandomGenerator

/**
 * SplitMix64 over a single atomic counter. Each draw is one lock-free fetch-and-add, so one instance can be sampled
 * from several threads at once (as the parallel network update can, when neurons share a distribution) without a
 * lock, and every draw still gets its own state. Gaussian samples use the JDK's ziggurat sampler
 * ([JavaRandomGenerator.nextGaussian]) instead of java.util.Random's polar method, which with java.util.Random's
 * synchronization made the JDKRandomGenerator this replaces about seven times slower per Gaussian sample.
 *
 * A seeded instance gives a reproducible sequence, but not the sequence the JDKRandomGenerator gave for that seed.
 */
class SimbrainRandomGenerator(seed: Long = ThreadLocalRandom.current().nextLong()) :
    CommonsRandomGenerator, JavaRandomGenerator {

    private val state = AtomicLong(seed)

    override fun nextLong(): Long {
        var z = state.addAndGet(GOLDEN_GAMMA)
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    override fun setSeed(seed: Long) {
        state.set(seed)
    }

    override fun setSeed(seed: Int) = setSeed(seed.toLong())

    override fun setSeed(seed: IntArray) = setSeed(seed.fold(0L) { acc, value -> acc * 31 + value })

    override fun nextBytes(bytes: ByteArray) = super<JavaRandomGenerator>.nextBytes(bytes)

    override fun nextInt(): Int = super<JavaRandomGenerator>.nextInt()

    override fun nextInt(n: Int): Int = super<JavaRandomGenerator>.nextInt(n)

    override fun nextBoolean(): Boolean = super<JavaRandomGenerator>.nextBoolean()

    override fun nextFloat(): Float = super<JavaRandomGenerator>.nextFloat()

    override fun nextDouble(): Double = super<JavaRandomGenerator>.nextDouble()

    override fun nextGaussian(): Double = super<JavaRandomGenerator>.nextGaussian()

    private companion object {
        const val GOLDEN_GAMMA = -0x61c8864680b583ebL
    }
}
