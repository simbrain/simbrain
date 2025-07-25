package org.simbrain.network.trainers

import org.simbrain.network.trainers.SamplingUtils

/**
 * Demonstration of different sampling strategies
 */
fun main() {
    println("=== Language Model Sampling Strategies Demo ===\n")
    
    // Example logits from a language model (higher values = more probable)
    val logits = doubleArrayOf(1.0, 2.0, 0.5, 3.0, 1.5)
    val tokens = listOf("the", "cat", "sat", "on", "mat")
    
    println("Logits: ${logits.contentToString()}")
    println("Tokens: $tokens")
    println("Token probabilities: ${getProbabilities(logits).mapIndexed { i, p -> "${tokens[i]}: ${"%.3f".format(p)}" }}")
    println()
    
    // Test different sampling strategies
    println("1. Greedy Sampling (always picks most probable):")
    repeat(5) {
        val index = SamplingUtils.greedySampling(logits)
        println("   Sample ${it + 1}: '${tokens[index]}' (index $index)")
    }
    println()
    
    println("2. Top-K Sampling (k=3, temperature=1.0):")
    repeat(10) {
        val index = SamplingUtils.topKSampling(logits, k = 3, temperature = 1.0)
        println("   Sample ${it + 1}: '${tokens[index]}' (index $index)")
    }
    println()
    
    println("3. Top-K Sampling (k=3, temperature=0.1) - More deterministic:")
    repeat(10) {
        val index = SamplingUtils.topKSampling(logits, k = 3, temperature = 0.1)
        println("   Sample ${it + 1}: '${tokens[index]}' (index $index)")
    }
    println()
    
    println("4. Top-K Sampling (k=3, temperature=2.0) - More random:")
    repeat(10) {
        val index = SamplingUtils.topKSampling(logits, k = 3, temperature = 2.0)
        println("   Sample ${it + 1}: '${tokens[index]}' (index $index)")
    }
    println()
    
    println("5. Top-P Sampling (p=0.8, temperature=1.0):")
    repeat(10) {
        val index = SamplingUtils.topPSampling(logits, p = 0.8, temperature = 1.0)
        println("   Sample ${it + 1}: '${tokens[index]}' (index $index)")
    }
    println()
    
    println("6. Random Sampling (temperature=1.0):")
    repeat(10) {
        val index = SamplingUtils.randomSampling(logits, temperature = 1.0)
        println("   Sample ${it + 1}: '${tokens[index]}' (index $index)")
    }
    println()
    
    println("7. Random Sampling (temperature=0.1) - More deterministic:")
    repeat(10) {
        val index = SamplingUtils.randomSampling(logits, temperature = 0.1)
        println("   Sample ${it + 1}: '${tokens[index]}' (index $index)")
    }
    println()
    
    println("8. Random Sampling (temperature=5.0) - More uniform:")
    repeat(10) {
        val index = SamplingUtils.randomSampling(logits, temperature = 5.0)
        println("   Sample ${it + 1}: '${tokens[index]}' (index $index)")
    }
    println()
    
    println("=== Key Observations ===")
    println("• Greedy always picks the same token (most probable)")
    println("• Top-K samples from the K most probable tokens")
    println("• Top-P samples from tokens whose cumulative probability exceeds P")
    println("• Temperature controls randomness: lower = more deterministic, higher = more random")
    println("• Temperature affects the 'sharpness' of the probability distribution")
}

private fun getProbabilities(logits: DoubleArray): List<Double> {
    val maxLogit = logits.maxOrNull() ?: 0.0
    val expLogits = logits.map { Math.exp(it - maxLogit) }
    val sumExp = expLogits.sum()
    return expLogits.map { it / sumExp }
} 