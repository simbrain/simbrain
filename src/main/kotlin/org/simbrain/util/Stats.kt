package org.simbrain.util

import org.apache.commons.math3.distribution.ChiSquaredDistribution
import org.apache.commons.math3.distribution.TDistribution
import kotlin.math.pow
import kotlin.math.sqrt

val DoubleArray.variance get() = sumOf { n -> (n - average()).pow(2.0)  } / size

val DoubleArray.mean get() = average()

val DoubleArray.stdev get() = sqrt(variance)

fun stderr(sampleStdev: Double, sampleSize: Int): Double {
    return sampleStdev / sqrt(sampleSize.toDouble())
}

fun tscore(alpha: Double, df: Int): Double {
    val tdist = TDistribution(df.toDouble())
    // Absolute because t-values from t-tables are the absolute value of inverse cumulative probabilities
    return Math.abs(tdist.inverseCumulativeProbability(alpha))
}

fun chiSquareScore(alpha: Double, df: Int): Double {
    val dist = ChiSquaredDistribution(df.toDouble())
    return dist.inverseCumulativeProbability(alpha)
}

fun confidenceIntervalForMeanOfNormalDist(mean: Double, stdev: Double, alpha: Double, N: Int):
        ClosedFloatingPointRange<Double> {
    val halfInterval = tscore(alpha / 2, N - 1) * stderr(stdev, N)
    return (mean - halfInterval)..(mean + halfInterval)
}

fun confidenceIntervalForVarianceOfNormalDist(variance: Double, alpha: Double, N: Int) = (
        (N - 1) / chiSquareScore(1 - alpha / 2, N - 1) * variance ..
                (N - 1) / chiSquareScore(alpha / 2, N - 1) * variance)
