package org.simbrain.custom_sims.simulations.hebb

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TrainedPatternsTest {

    @Test
    fun `exact matches take the built-in name and repeats are counted`() {
        val patterns = builtInPatterns(100)
        val trained = TrainedPatterns(patterns, 0.5)
        trained.record(patterns.getValue("Circle"))
        trained.record(patterns.getValue("Cross"))
        trained.record(patterns.getValue("Circle"))
        assertEquals(listOf("Circle ×2", "Cross"), trained.labels)
    }

    @Test
    fun `near matches show how many units differ`() {
        val patterns = builtInPatterns(100)
        val trained = TrainedPatterns(patterns, 0.5)
        val noisySquare = patterns.getValue("Square").copyOf().apply {
            for (i in 0 until 3) this[i] = 1.0 - this[i]
        }
        trained.record(noisySquare)
        assertEquals(listOf("Square ~3"), trained.labels)
    }

    @Test
    fun `blank and full patterns are not named after sparse built-ins`() {
        val trained = TrainedPatterns(builtInPatterns(100), 0.5, nameInverses = true)
        trained.record(DoubleArray(100))
        trained.record(DoubleArray(100) { 1.0 })
        assertEquals(listOf("Pattern 1", "Pattern 2"), trained.labels)
    }

    @Test
    fun `different patterns with the same near-match name get a letter`() {
        val patterns = builtInPatterns(100)
        val trained = TrainedPatterns(patterns, 0.5)
        fun circleFlipping(vararg indices: Int) = patterns.getValue("Circle").copyOf().apply {
            indices.forEach { this[it] = 1.0 - this[it] }
        }
        trained.record(circleFlipping(0, 1))
        trained.record(circleFlipping(2, 3))
        trained.record(circleFlipping(0, 1))
        assertEquals(listOf("Circle ~2 ×2", "Circle ~2b"), trained.labels)
    }

    @Test
    fun `unrelated patterns are numbered in training order`() {
        val patterns = builtInPatterns(100)
        val trained = TrainedPatterns(patterns, 0.5)
        trained.record(DoubleArray(100) { if (it % 2 == 0) 1.0 else 0.0 })
        trained.record(DoubleArray(100) { if (it % 3 == 0) 1.0 else 0.0 })
        assertEquals(listOf("Pattern 1", "Pattern 2"), trained.labels)
    }

    @Test
    fun `inverted patterns are named only when requested`() {
        val patterns = builtInPatterns(100, bipolar = true)
        val inverseCross = patterns.getValue("Cross").map { -it }.toDoubleArray()
        val withInverses = TrainedPatterns(patterns, 0.0, nameInverses = true)
        withInverses.record(inverseCross)
        assertEquals(listOf("Cross (inverted)"), withInverses.labels)
        val withoutInverses = TrainedPatterns(patterns, 0.0)
        withoutInverses.record(inverseCross)
        assertEquals(listOf("Pattern 1"), withoutInverses.labels)
    }

    @Test
    fun `clearing restarts numbering`() {
        val trained = TrainedPatterns(builtInPatterns(100), 0.5)
        trained.record(DoubleArray(100) { if (it % 2 == 0) 1.0 else 0.0 })
        trained.clear()
        trained.record(DoubleArray(100) { if (it % 3 == 0) 1.0 else 0.0 })
        assertEquals(listOf("Pattern 1"), trained.labels)
    }
}
