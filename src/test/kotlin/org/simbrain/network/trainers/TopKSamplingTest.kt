/**
 * Top-k sampling selects its candidates by running insertion rather than a full sort; these
 * tests pin it to the sort-based selection, including tie order, so seeded runs stay identical.
 */
package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class TopKSamplingTest {

    private fun sortBasedTopK(k: Int, probabilities: DoubleArray, random: Random): Int {
        val indexed = probabilities.mapIndexed { index, prob -> index to prob }
            .sortedByDescending { it.second }
            .take(k)
        val sum = indexed.sumOf { it.second }
        var cumulative = 0.0
        for ((index, prob) in indexed) {
            cumulative += prob / sum
            if (random.nextDouble() <= cumulative) return index
        }
        return indexed.last().first
    }

    @Test
    fun `top k samples the same tokens as a sort based selection`() {
        val source = Random(11)
        for (size in listOf(1, 3, 10, 500)) {
            for (k in listOf(1, 2, 5, 40, 1000)) {
                repeat(20) {
                    val raw = DoubleArray(size) { source.nextDouble() }
                    val probabilities = raw.map { it / raw.sum() }.toDoubleArray()
                    val seed = source.nextInt()
                    assertEquals(
                        sortBasedTopK(k, probabilities, Random(seed)),
                        SamplingStrategy.TopK(k).sample(probabilities, Random(seed)),
                        "size=$size k=$k",
                    )
                }
            }
        }
    }

    @Test
    fun `tied probabilities keep the lower index first`() {
        val probabilities = doubleArrayOf(0.1, 0.3, 0.1, 0.3, 0.1, 0.1)
        for (seed in 0 until 50) {
            for (k in 1..6) {
                assertEquals(
                    sortBasedTopK(k, probabilities, Random(seed)),
                    SamplingStrategy.TopK(k).sample(probabilities, Random(seed)),
                    "seed=$seed k=$k",
                )
            }
        }
    }
}
