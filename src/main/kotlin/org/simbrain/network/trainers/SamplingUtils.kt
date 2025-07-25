package org.simbrain.network.trainers

import org.simbrain.util.propertyeditor.EditableObject
import kotlin.math.exp

/**
 * Sampling strategies for language model inference
 */
sealed class SamplingStrategy: EditableObject {
    object Greedy : SamplingStrategy()
    data class TopK(val k: Int, val temperature: Double = 1.0) : SamplingStrategy()
    data class TopP(val p: Double, val temperature: Double = 1.0) : SamplingStrategy()
    data class Random(val temperature: Double = 1.0) : SamplingStrategy()
}

/**
 * Sampling utilities for language model inference
 */
object SamplingUtils {

    /**
     * Sample from the top-k most probable tokens
     * @param logits Raw logits from the model
     * @param k Number of top tokens to consider
     * @param temperature Temperature for softmax (higher = more random)
     * @return Index of the sampled token
     */
    fun topKSampling(logits: DoubleArray, k: Int, temperature: Double = 1.0): Int {
        require(k > 0) { "k must be positive" }
        require(temperature > 0) { "temperature must be positive" }
        
        // Apply temperature and softmax
        val scaledLogits = logits.map { it / temperature }
        val maxLogit = scaledLogits.maxOrNull() ?: 0.0
        val expLogits = scaledLogits.map { exp(it - maxLogit) }
        val sumExp = expLogits.sum()
        val probabilities = expLogits.map { it / sumExp }
        
        // Get top-k indices and probabilities
        val indexedProbs = probabilities.mapIndexed { index, prob -> index to prob }
            .sortedByDescending { it.second }
            .take(k)
        
        // Sample from top-k
        val topKProbs = indexedProbs.map { it.second }
        val topKIndices = indexedProbs.map { it.first }
        
        val random = Math.random()
        var cumulativeProb = 0.0
        
        for (i in topKProbs.indices) {
            cumulativeProb += topKProbs[i]
            if (random <= cumulativeProb) {
                return topKIndices[i]
            }
        }
        
        return topKIndices.last()
    }

    /**
     * Sample from tokens whose cumulative probability exceeds p (nucleus sampling)
     * @param logits Raw logits from the model
     * @param p Cumulative probability threshold (0.0 to 1.0)
     * @param temperature Temperature for softmax (higher = more random)
     * @return Index of the sampled token
     */
    fun topPSampling(logits: DoubleArray, p: Double, temperature: Double = 1.0): Int {
        require(p in 0.0..1.0) { "p must be between 0.0 and 1.0" }
        require(temperature > 0) { "temperature must be positive" }
        
        // Apply temperature and softmax
        val scaledLogits = logits.map { it / temperature }
        val maxLogit = scaledLogits.maxOrNull() ?: 0.0
        val expLogits = scaledLogits.map { Math.exp(it - maxLogit) }
        val sumExp = expLogits.sum()
        val probabilities = expLogits.map { it / sumExp }
        
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
        
        // Sample from nucleus
        val random = Math.random()
        var currentProb = 0.0
        
        for ((index, prob) in nucleus) {
            currentProb += prob / nucleus.sumOf { it.second }
            if (random <= currentProb) {
                return index
            }
        }
        
        return nucleus.last().first
    }

    /**
     * Greedy sampling - always pick the most probable token
     * @param logits Raw logits from the model
     * @return Index of the most probable token
     */
    fun greedySampling(logits: DoubleArray): Int {
        return logits.indices.maxByOrNull { logits[it] } ?: 0
    }

    /**
     * Random sampling from the full distribution
     * @param logits Raw logits from the model
     * @param temperature Temperature for softmax (higher = more random)
     * @return Index of the sampled token
     */
    fun randomSampling(logits: DoubleArray, temperature: Double = 1.0): Int {
        require(temperature > 0) { "temperature must be positive" }
        
        // Apply temperature and softmax
        val scaledLogits = logits.map { it / temperature }
        val maxLogit = scaledLogits.maxOrNull() ?: 0.0
        val expLogits = scaledLogits.map { Math.exp(it - maxLogit) }
        val sumExp = expLogits.sum()
        val probabilities = expLogits.map { it / sumExp }
        
        // Sample from full distribution
        val random = Math.random()
        var cumulativeProb = 0.0
        
        for (i in probabilities.indices) {
            cumulativeProb += probabilities[i]
            if (random <= cumulativeProb) {
                return i
            }
        }
        
        return probabilities.lastIndex
    }
} 