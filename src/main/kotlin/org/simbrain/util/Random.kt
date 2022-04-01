package org.simbrain.util

import org.simbrain.util.math.ProbabilityDistribution
import smile.math.matrix.Matrix
import kotlin.random.Random

fun Random.nextBoolean(probability: Double) = nextDouble() < probability

fun Random.nextNegate() = if (nextBoolean()) 1 else -1

fun FloatArray.randomize(dist: ProbabilityDistribution) {
    forEachIndexed { i, _ -> this[i] = dist.nextDouble().toFloat() }
}

fun DoubleArray.randomize(dist: ProbabilityDistribution) {
    forEachIndexed { i, _ -> this[i] = dist.nextDouble() }
}

fun Matrix.randomize(dist: ProbabilityDistribution) {
    (0 until nrows()).forEach{i ->
        (0 until ncols()).forEach{j ->
            set(i,j,dist.nextDouble())
        }
    }
}