package org.simbrain.util

fun <T : Comparable<T>> T.clip(lowerBound: T, upperBound: T) =
        maxOf(minOf(lowerBound, upperBound), minOf(maxOf(upperBound, lowerBound), this))

fun <T : Comparable<T>> T.clip(range: ClosedRange<T>) =
        maxOf(minOf(range.start, range.endInclusive), minOf(maxOf(range.endInclusive, range.start), this))

/**
 * Reference: https://stackoverflow.com/a/23088000
 */
fun Double.format(digits: Int) = "%.${digits}f".format(this)

infix fun Iterable<Double>.sse(other: Iterable<Double>)
        = this.zip(other).map { (a, b) -> (a - b).let { it * it } }.sum()

infix fun Iterable<Double>.mse(other: Iterable<Double>)
        = this.zip(other).map { (a, b) -> (a - b).let { it * it } }.average()

@JvmName("sseInt")
infix fun Iterable<Int>.sse(other: Iterable<Int>)
        = this.zip(other).map { (a, b) -> (a - b).let { it * it } }.sum()

@JvmName("mseInt")
infix fun Iterable<Int>.mse(other: Iterable<Int>)
        = this.zip(other).map { (a, b) -> (a - b).let { it * it } }.average()