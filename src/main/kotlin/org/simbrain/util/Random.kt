package org.simbrain.util

import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix
import kotlin.random.Random

fun Random.nextBoolean(probability: Double) = nextDouble() < probability

fun Random.nextNegate() = if (nextBoolean()) 1 else -1

fun FloatArray.randomize(dist: ProbabilityDistribution) {
    forEachIndexed { i, _ -> this[i] = dist.sampleDouble().toFloat() }
}

fun DoubleArray.randomize(dist: ProbabilityDistribution) {
    forEachIndexed { i, _ -> this[i] = dist.sampleDouble() }
}

fun Matrix.randomize(dist: ProbabilityDistribution) {
    (0 until nrows()).forEach{i ->
        (0 until ncols()).forEach{j ->
            set(i,j,dist.sampleDouble())
        }
    }
}

fun <T> List<T>.sampleWithoutReplacement(
    random: Random = Random(Random.nextLong()),
    restartIfExhausted: Boolean = false
) = this.asSequence().sampleWithoutReplacement(random, restartIfExhausted)

fun <T> Sequence<T>.sampleWithoutReplacement(
    random: Random = Random(Random.nextLong()),
    restartIfExhausted: Boolean = false
) = sequence {
    do {
        shuffled(random).forEach { yield(it) }
    } while (restartIfExhausted)
}