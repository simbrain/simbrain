/**
 * How a language model picks the next token from its output distribution: greedily, from the k most probable tokens,
 * or from the smallest set of most probable tokens whose probabilities reach p. Top-k and top-p both narrow the
 * vocabulary to candidates and then draw one of them in proportion to its probability through [drawFromWeights].
 */
package org.simbrain.network.trainers

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import kotlin.random.Random

/**
 * Sampling strategies for language model inference
 */
sealed class SamplingStrategy: CopyableObject {

    /**
     * Sample from normalized probabilities using this strategy
     * @param probabilities Normalized probability distribution (should sum to 1.0)
     * @return Index of the sampled token
     */
    abstract fun sample(probabilities: DoubleArray, random: Random = Random): Int

    object Greedy : SamplingStrategy() {
        override fun sample(probabilities: DoubleArray, random: Random): Int {
            // Greedy sampling - always pick the most probable token
            return probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        }

        override fun copy() = this
    }

    data class TopK(@UserParameter(
        label = "k",
        description = "Number of highest probability tokens to consider for sampling."
    ) var k: Int = 5) : SamplingStrategy() {
        override fun sample(probabilities: DoubleArray, random: Random): Int {
            require(k > 0) { "k must be positive" }
            val candidates = topKIndices(probabilities, k)
            return drawFromWeights(candidates, DoubleArray(candidates.size) { probabilities[candidates[it]] }, random)
        }

        override fun copy() = TopK(k)
    }

    data class TopP(@UserParameter(
        label = "p",
        description = "Cumulative probability threshold for nucleus sampling. Higher values include more tokens (0.9 is typical)"
    ) var p: Double = 0.9) : SamplingStrategy() {
        override fun sample(probabilities: DoubleArray, random: Random): Int {
            require(p in 0.0..1.0) { "p must be between 0.0 and 1.0" }

            // The nucleus is the shortest prefix of the descending order whose probabilities reach p
            val order = topKIndices(probabilities, probabilities.size)
            var cumulativeProb = 0.0
            var nucleusSize = 0
            while (nucleusSize < order.size) {
                cumulativeProb += probabilities[order[nucleusSize++]]
                if (cumulativeProb >= p) break
            }
            val nucleus = order.copyOf(nucleusSize)
            return drawFromWeights(nucleus, DoubleArray(nucleusSize) { probabilities[nucleus[it]] }, random)
        }

        override fun copy() = TopP(p)
    }

}

/**
 * The indices of the [k] largest [probabilities] (all of them if there are fewer), largest first, with ties keeping
 * the lower index first as a stable descending sort does. A running insertion keeps this O(n·k) over LLM vocabularies
 * instead of boxing and sorting every entry. Insertion grows quadratic as k approaches the vocabulary size, so a k
 * beyond a sixteenth of it sorts instead.
 */
internal fun topKIndices(probabilities: DoubleArray, k: Int): IntArray {
    val limit = minOf(k, probabilities.size)
    if (limit > probabilities.size / 16) {
        return probabilities.indices.sortedByDescending { probabilities[it] }.take(limit).toIntArray()
    }
    val indices = IntArray(limit)
    val values = DoubleArray(limit)
    var filled = 0
    for (index in probabilities.indices) {
        val prob = probabilities[index]
        if (filled == limit && prob.compareTo(values[limit - 1]) <= 0) continue
        var slot = minOf(filled, limit - 1)
        while (slot > 0 && prob.compareTo(values[slot - 1]) > 0) {
            values[slot] = values[slot - 1]
            indices[slot] = indices[slot - 1]
            slot--
        }
        values[slot] = prob
        indices[slot] = index
        if (filled < limit) filled++
    }
    return indices
}

/**
 * One of [indices], chosen with probability proportional to its entry in [weights], which need not sum to 1. Draws a
 * single uniform and finds where it falls among the cumulative weights.
 */
internal fun drawFromWeights(indices: IntArray, weights: DoubleArray, random: Random): Int {
    val target = random.nextDouble() * weights.sum()
    var cumulative = 0.0
    for (i in indices.indices) {
        cumulative += weights[i]
        if (target < cumulative) return indices[i]
    }
    // A uniform just below 1 can round the target up to the full total
    return indices.last()
}
