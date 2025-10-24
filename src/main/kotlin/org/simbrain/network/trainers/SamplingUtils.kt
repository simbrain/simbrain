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
            
            // Get top-k indices and probabilities
            val indexedProbs = probabilities.mapIndexed { index, prob -> index to prob }
                .sortedByDescending { it.second }
                .take(k)
            
            // Renormalize top-k probabilities
            val topKProbs = indexedProbs.map { it.second }
            val topKIndices = indexedProbs.map { it.first }
            val sumTopK = topKProbs.sum()
            val normalizedTopKProbs = topKProbs.map { it / sumTopK }
            
            // Sample from top-k
            var cumulativeProb = 0.0
            
            for (i in normalizedTopKProbs.indices) {
                cumulativeProb += normalizedTopKProbs[i]
                if (random.nextDouble() <= cumulativeProb) {
                    return topKIndices[i]
                }
            }
            
            return topKIndices.last()
        }

        override fun copy() = TopK(k)
    }
    
    data class TopP(@UserParameter(
        label = "p", 
        description = "Cumulative probability threshold for nucleus sampling. Higher values include more tokens (0.9 is typical)"
    ) var p: Double = 0.9) : SamplingStrategy() {
        override fun sample(probabilities: DoubleArray, random: Random): Int {
            require(p in 0.0..1.0) { "p must be between 0.0 and 1.0" }
            
            // Sort by probability and find nucleus
            val indexedProbs = probabilities.mapIndexed { index, prob -> index to prob }
                .sortedByDescending { it.second }
            
            var cumulativeProb = 0.0
            val nucleus = mutableListOf<Pair<Int, Double>>()
            
            for ((index, prob) in indexedProbs) {
                cumulativeProb += prob
                nucleus.add(index to prob)
                if (cumulativeProb >= p) break
            }
            
            // Renormalize nucleus probabilities
            val nucleusSum = nucleus.sumOf { it.second }
            
            // Sample from nucleus
            var currentProb = 0.0
            
            for ((index, prob) in nucleus) {
                currentProb += prob / nucleusSum
                if (random.nextDouble() <= currentProb) {
                    return index
                }
            }
            
            return nucleus.last().first
        }

        override fun copy() = TopP(p)
    }

}
