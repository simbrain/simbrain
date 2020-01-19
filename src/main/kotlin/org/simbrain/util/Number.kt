package org.simbrain.util

fun <T : Comparable<T>> T.clip(lowerBound: T, upperBound: T) =
        maxOf(minOf(lowerBound, upperBound), minOf(maxOf(upperBound, lowerBound), this))

/**
 * Reference: https://stackoverflow.com/a/23088000
 */
fun Double.format(digits: Int) = "%.${digits}f".format(this)