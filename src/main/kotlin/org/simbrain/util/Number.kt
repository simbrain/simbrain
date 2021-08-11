package org.simbrain.util

import smile.math.matrix.Matrix

/**
 * Numeric utilities in Kotlin. Comparable to [SimbrainMath] .
 */

fun <T : Comparable<T>> T.clip(lowerBound: T, upperBound: T) =
        maxOf(minOf(lowerBound, upperBound), minOf(maxOf(upperBound, lowerBound), this))

fun <T : Comparable<T>> T.clip(range: ClosedRange<T>) =
        maxOf(minOf(range.start, range.endInclusive), minOf(maxOf(range.endInclusive, range.start), this))

/**
 * Reference: https://stackoverflow.com/a/23088000
 */
fun Double.format(digits: Int) = "%.${digits}f".format(this)

infix fun Iterable<Double>.squaredError(other: Iterable<Double>)
        = this.zip(other).map { (a, b) -> (a - b).let { it * it } }

infix fun Iterable<Double>.sse(other: Iterable<Double>)
        = (this squaredError other).sum()

infix fun Iterable<Double>.mse(other: Iterable<Double>)
        = (this squaredError other).average()

@JvmName("squaredErrorInt")
infix fun Iterable<Int>.squaredError(other: Iterable<Int>)
        = this.zip(other).map { (a, b) -> (a - b).let { it * it } }

@JvmName("sseInt")
infix fun Iterable<Int>.sse(other: Iterable<Int>)
        = (this squaredError other).sum()

@JvmName("mseInt")
infix fun Iterable<Int>.mse(other: Iterable<Int>)
        = (this squaredError other).average()

fun getOneHotMat(index : Int, size: Int, amount: Double = 1.0): Matrix {
        val ret = Matrix(size, 1)
        ret[index, 0] = amount
        return ret
}

fun getOneHotArray(index : Int, size: Int, amount: Double = 1.0): DoubleArray {
        val ret = DoubleArray(size)
        ret[index] = amount
        return ret
}

/**
 * https://stackoverflow.com/questions/7513434/convert-a-double-array-to-a-float-array
 */
fun toFloatArray(array: DoubleArray): FloatArray {
        return array.map { it.toFloat() }.toFloatArray()
}