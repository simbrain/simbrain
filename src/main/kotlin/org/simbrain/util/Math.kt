package org.simbrain.util

import org.simbrain.util.math.SimbrainMath
import smile.math.matrix.Matrix

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

/**
 * Creates a one-hot vector with [size] elements, and [index] set to [amount], the "hot" value, and the rest
 * set to 0. 0-indexed.
 */
fun getOneHotMat(index: Int, size: Int, amount: Double = 1.0): Matrix {
    if (index < 0 || index >= size) {
        throw IllegalArgumentException("Index $index of one-hot vector with $size components must be between 0 and $size")
    }
    val ret = Matrix(size, 1)
    ret[index, 0] = amount
    return ret
}

fun getOneHotArray(index: Int, size: Int, amount: Double = 1.0): DoubleArray {
    val ret = DoubleArray(size)
    ret[index] = amount
    return ret
}

fun getDiagonal2DDoubleArray(rows: Int, cols: Int): Array<DoubleArray> {
    return Matrix.eye(rows, cols).toArray()
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

/**
 * Returns the vector sum of a list of arrays.
 */
fun List<DoubleArray>.sum(): DoubleArray {
    return reduce(SimbrainMath::addVector)
}

/**
 * Create a sequence of double starting at start, incremented by step, and stopping at stop.
 */
fun makeDoubleSequence(start: Double, stop: Double, step: Double): Sequence<Double> {
    return sequence {
        var current = start
        while (current <= stop) {
            yield(current)
            current += step
        }
    }
}

/**
 * Apply a function to a sequence of doubles and return the result as [f(start),....f(stop)].
 * Useful to create display strings or strings that can be pasted into numpy arrays for example.
 */
fun makeStringArray(start: Double, stop: Double, f: (Double) -> Double, step: Double = 1.0): String {
    return "[${makeDoubleSequence(start, stop, step).map(f).joinToString(",")}]"
}