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

fun confidenceIntervalMean(mean: Double, stdev: Double, alpha: Double, N: Int):
        ClosedFloatingPointRange<Double> {
    val halfInterval = tscore(alpha / 2, N - 1) * stderr(stdev, N)
    return (mean - halfInterval)..(mean + halfInterval)
}

fun confidenceIntervalVariance(variance: Double, alpha: Double, N: Int) = (
        (N - 1) / chiSquareScore(1 - alpha / 2, N - 1) * variance ..
                (N - 1) / chiSquareScore(alpha / 2, N - 1) * variance)

enum class MeasureType {
    COVARIANCE,
    CORRELATION
}

fun computeMeasure(x: DoubleArray, y: DoubleArray, type: MeasureType): Double {
    require(x.size == y.size) { "Arrays must have the same length" }

    val n = x.size
    var sumX = 0.0
    var sumY = 0.0
    var sumX2 = 0.0
    var sumY2 = 0.0
    var sumXY = 0.0

    for (i in 0 until n) {
        sumX += x[i]
        sumY += y[i]
        sumX2 += x[i] * x[i]
        sumY2 += y[i] * y[i]
        sumXY += x[i] * y[i]
    }

    return when (type) {
        MeasureType.COVARIANCE -> {
            val meanX = sumX / n
            val meanY = sumY / n
            (sumXY - n * meanX * meanY) / n
        }
        MeasureType.CORRELATION -> {
            val numerator = n * sumXY - sumX * sumY
            val denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))
            numerator / denominator
        }
    }
}

fun computeCovariance(x: DoubleArray, y: DoubleArray): Double {
    return computeMeasure(x, y, MeasureType.COVARIANCE)
}

fun computeCorrelation(x: DoubleArray, y: DoubleArray): Double {
    return computeMeasure(x, y, MeasureType.CORRELATION)
}
