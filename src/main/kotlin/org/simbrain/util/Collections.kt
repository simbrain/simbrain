package org.simbrain.util

data class Difference<T>(val disjoint: Set<T>, val excess: Set<T>) {
    fun isIdentical() = disjoint.isEmpty() && excess.isEmpty()
}

infix fun <T> Set<T>.diff(other: Set<T>) = Difference(this - other, other - this)