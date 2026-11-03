/**
 * The random source behind every [ProbabilityDistribution], and keyed draws ([keyedUnitDouble]) from the SplitMix64
 * hashing it is built on, such as a synapse's spike probability.
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

    override fun nextLong(): Long = mix64(state.addAndGet(GOLDEN_GAMMA))

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

    companion object {
        internal const val GOLDEN_GAMMA = -0x61c8864680b583ebL
    }
}

/**
 * SplitMix64's output function: scrambles [z] so that nearby inputs give unrelated outputs. Hashing a key with it
 * gives a draw that depends only on the key, for randomness that must not depend on the order of the draws.
 */
internal fun mix64(z: Long): Long {
    var x = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
    x = (x xor (x ushr 27)) * -0x6b2fb644ecceee15L
    return x xor (x ushr 31)
}

/**
 * A double in [0, 1) from the top 53 bits of [bits], as [JavaRandomGenerator.nextDouble] makes one.
 */
private fun unitDouble(bits: Long): Double = (bits ushr 11) * (1.0 / (1L shl 53))

/**
 * A double in [0, 1) that depends only on [seed] and the keys, hashed in order, for a draw that must come out the same
 * whatever order the draws are made in.
 */
fun keyedUnitDouble(seed: Long, key1: Long, key2: Long, key3: Long): Double {
    var key = mix64(seed + SimbrainRandomGenerator.GOLDEN_GAMMA * key1)
    key = mix64(key + SimbrainRandomGenerator.GOLDEN_GAMMA * key2)
    return unitDouble(mix64(key + SimbrainRandomGenerator.GOLDEN_GAMMA * key3))
}
