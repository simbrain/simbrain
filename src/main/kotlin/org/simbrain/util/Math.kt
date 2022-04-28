package org.simbrain.util

import smile.math.matrix.Matrix
import kotlin.math.pow

/**
 * Numeric utilities in Kotlin. Comparable to [SimbrainMath].
 */

fun <T : Comparable<T>> T.clip(lowerBound: T, upperBound: T) =
    maxOf(minOf(lowerBound, upperBound), minOf(maxOf(upperBound, lowerBound), this))

fun <T : Comparable<T>> T.clip(range: ClosedRange<T>) =
    maxOf(minOf(range.start, range.endInclusive), minOf(maxOf(range.endInclusive, range.start), this))

/**
 * Reference: https://stackoverflow.com/a/23088000
 */
fun Double.format(digits: Int) = "%.${digits}f".format(this)

infix fun Iterable<Double>.squaredError(other: Iterable<Double>) =
    this.zip(other).map { (a, b) -> (a - b).let { it * it } }

infix fun Iterable<Double>.sse(other: Iterable<Double>) = (this squaredError other).sum()

infix fun Iterable<Double>.mse(other: Iterable<Double>) = (this squaredError other).average()

@JvmName("squaredErrorInt")
infix fun Iterable<Int>.squaredError(other: Iterable<Int>) = this.zip(other).map { (a, b) -> (a - b).let { it * it } }

@JvmName("sseInt")
infix fun Iterable<Int>.sse(other: Iterable<Int>) = (this squaredError other).sum()

@JvmName("mseInt")
infix fun Iterable<Int>.mse(other: Iterable<Int>) = (this squaredError other).average()

fun getOneHotMat(index: Int, size: Int, amount: Double = 1.0): Matrix {
    val ret = Matrix(size, 1)
    ret[index, 0] = amount
    return ret
}

fun getOneHotArray(index: Int, size: Int, amount: Double = 1.0): DoubleArray {
    val ret = DoubleArray(size)
    ret[index] = amount
    return ret
}

/**
 * https://stackoverflow.com/questions/7513434/convert-a-double-array-to-a-float-array
 */
fun DoubleArray.toFloatArray(): FloatArray {
    return map { it.toFloat() }.toFloatArray()
}
fun FloatArray.toDoubleArray(): DoubleArray {
    return map { it.toDouble() }.toDoubleArray()
}
fun IntArray.toDoubleArray(): DoubleArray {
    return map { it.toDouble() }.toDoubleArray()
}
fun DoubleArray.toIntArray(): IntArray {
    return map { it.toInt() }.toIntArray()
}


fun Any?.isRealValued(): Boolean {
        return this is Double || this is Float
}

fun Any?.isIntegerValued(): Boolean {
        return this is Short || this is Long ||  this is Int || this is Byte
}

/**
 * Return a string representation of a 2-d array in the form of a list of list of doubles
 */
@JvmName("listOfListToCSV")
fun List<List<Double>>.toCsvString(): String {
    return joinToString("\n") { it.joinToString(",") }
}

@JvmName("listOfArrayToCSV")
fun List<DoubleArray>.toCsvString(): String {
    return joinToString("\n") { it.joinToString(",") }
}

fun DoubleArray.variance(): Double {
    val avg = average()
    return sumOf { n -> (n - avg).pow(2.0)  } / size
}

fun DoubleArray.stdev(): Double = kotlin.math.sqrt(variance())

/**
 * https://en.wikipedia.org/wiki/Geometric_progression
 *  a, ar, ar^2, ...
 */
fun createGeometricProgression(initialValue: Double, ratio: Double): Sequence<Double> {
    return sequence{
        var current = initialValue
        while (true) {
            yield(current)
            current *= ratio
        }
    }
}