package org.simbrain.util

import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.stats.distributions.TwoValued
import smile.math.matrix.Matrix
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

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
fun Double.roundToString(digits: Int) = format(digits)

fun DoubleArray.summedSquares() =
    sumOf { it * it }

infix fun Iterable<Double>.squaredError(other: Iterable<Double>) =
    this.zip(other).map { (a, b) -> (a - b).let { it * it } }

infix fun Iterable<Double>.sse(other: Iterable<Double>) = (this squaredError other).sum()

infix fun Iterable<Double>.mse(other: Iterable<Double>) = (this squaredError other).average()

infix fun DoubleArray.squaredError(other: DoubleArray) =
    this.zip(other).map { (a, b) -> (a - b).let { it * it } }

infix fun DoubleArray.dot(other: DoubleArray) = this.zip(other).sumOf { (a, b) -> a * b }

infix fun DoubleArray.sse(other: DoubleArray) = (this squaredError other).sum()

infix fun DoubleArray.mse(other: DoubleArray) = (this squaredError other).average()

@JvmName("squaredErrorInt")
infix fun Iterable<Int>.squaredError(other: Iterable<Int>) = this.zip(other).map { (a, b) -> (a - b).let { it * it } }

@JvmName("sseInt")
infix fun Iterable<Int>.sse(other: Iterable<Int>) = (this squaredError other).sum()

@JvmName("mseInt")
infix fun Iterable<Int>.mse(other: Iterable<Int>) = (this squaredError other).average()

/**
 * Creates a one-hot vector (represented with a Smile Matrix) with [size] elements, and [index] set to [amount], the
 * "hot" value, and the rest set to 0. Zero-indexed.
 */
fun getOneHot(index: Int, size: Int, amount: Double = 1.0): Matrix {
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

/**
 * Parse the provided value into a double if possible, else throw an exception
 */
fun tryParsingDouble(value: Any?): Double {
    if (value is Double) {
        return value
    }
    if (value is String) {
        return value.toDouble()
    }
    if (value is Int) {
        return value.toDouble()
    }
    throw NumberFormatException("Tried to parse a value that was not double into double")
}

/**
 * Parse the provided value into an integer if possible, else throw an exception.
 */
fun tryParsingInt(value: Any?): Int {
    if (value is Int) {
        return value
    }
    if (value is String) {
        return value.toInt()
    }
    if (value is Double) {
        return value.toInt()
    }
    throw NumberFormatException("Tried to parse a value that was not int into int")
}

fun DoubleArray.minus(other: DoubleArray): DoubleArray {
    return zip(other).map { (a, b) -> a - b }.toDoubleArray()
}

/**
 * Return a string representation of the vector rounded to the specified precision
 */
fun DoubleArray.format(precision: Int): String {
    return joinToString(",") { it.format(precision) }
}

fun DoubleArray.euclideanDistance(other: DoubleArray) = this.zip(other) { a, b -> (a - b) * (a - b) }
    .sum()
    .let { sqrt(it) }

operator fun FloatArray.minus(other: FloatArray) = (this zip other).map { (a, b) -> a - b }.toFloatArray()

operator fun FloatArray.div(scalar: Float) = map { it / scalar }.toFloatArray()

operator fun FloatArray.times(scalar: Float) = map { it * scalar }.toFloatArray()

fun createMatrix(m: Int, n: Int, binaryOperation: (i: Int, j: Int) -> Double): Array<DoubleArray> {
    val matrix = Array(m) { DoubleArray(n) }
    for (i in 0 until m) {
        for (j in 0 until n) {
            matrix[i][j] = binaryOperation(i, j)
        }
    }
    return matrix
}

fun computeCorrelationMatrix(data: Array<DoubleArray>) = createMatrix(data.size, data.size) { i, j ->
    computeCorrelation(data[i], data[j])
}

fun computeCovarianceMatrix(data: Array<DoubleArray>) = createMatrix(data.size, data.size) { i, j ->
    computeCovariance(data[i], data[j])
}

fun computeSimilarityMatrix(data: Array<DoubleArray>) = createMatrix(data.size, data.size) { i, j ->
    data[i].euclideanDistance(data[j])
}

fun computeCosineSimilarityMatrix(data: Array<DoubleArray>) = createMatrix(data.size, data.size) { i, j ->
    smile.math.MathEx.cos(data[i], data[j])
}

fun computeDotProductMatrix(data: Array<DoubleArray>) = createMatrix(data.size, data.size) { i, j ->
    data[i] dot data[j]
}

fun DoubleArray.outerProduct(other: DoubleArray) = toMatrix().mt(other.toMatrix())


context(Random)
fun ClosedRange<Double>.sample() = nextDouble(start, endInclusive)

/**
 * Add the entries of a double array to another double array and return the result as a new array.
 */
fun DoubleArray.add(other: DoubleArray) = (this zip other).map { (a, b) -> a + b }.toDoubleArray()

/**
 * Add the entries of a double array in-place to a double array.
 */
fun DoubleArray.addi(other: DoubleArray) {
    val size = min(this.size, other.size)
    for (i in 0 until size) {
        this[i] += other[i]
    }
}

fun Matrix.twoValueRandomize(lower: Double, upper: Double): Matrix = apply { randomize(TwoValued(lower, upper)) }

fun Matrix.binaryRandomize() = apply { twoValueRandomize(0.0, 1.0) }

fun Matrix.bipolarRandomize() = apply { twoValueRandomize(-1.0, 1.0) }

// TODO: Use for unit tests
fun main() {
    print(Matrix(10,10).binaryRandomize())
    print(Matrix(10,10).bipolarRandomize())
    print(Matrix(10,10).twoValueRandomize(-4.0, 4.0))
}