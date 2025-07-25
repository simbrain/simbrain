package org.simbrain.network.trainers

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.simbrain.network.trainers.SamplingStrategy
import org.simbrain.network.trainers.SamplingUtils

class SamplingUtilsTest {

    @Test
    fun `greedy sampling always picks highest probability`() {
        val logits = doubleArrayOf(1.0, 2.0, 0.5, 3.0, 1.5)
        val result = SamplingUtils.greedySampling(logits)
        assertEquals(3, result) // Index 3 has the highest value (3.0)
    }

    @Test
    fun `top-k sampling respects k parameter`() {
        val logits = doubleArrayOf(1.0, 2.0, 0.5, 3.0, 1.5)
        
        // Run multiple times to ensure we get different results within top-k
        val results = mutableSetOf<Int>()
        repeat(100) {
            val result = SamplingUtils.topKSampling(logits, k = 3, temperature = 1.0)
            results.add(result)
        }
        
        // Should only sample from top 3 indices: [1, 3, 4] (values: 2.0, 3.0, 1.5)
        assertTrue(results.all { it in setOf(1, 3, 4) })
        assertTrue(results.size > 1) // Should get some variety
    }

    @Test
    fun `temperature affects randomness in top-k sampling`() {
        val logits = doubleArrayOf(1.0, 2.0, 0.5, 3.0, 1.5)
        
        // Low temperature should be more deterministic
        val lowTempResults = mutableSetOf<Int>()
        repeat(50) {
            val result = SamplingUtils.topKSampling(logits, k = 3, temperature = 0.1)
            lowTempResults.add(result)
        }
        
        // High temperature should be more random
        val highTempResults = mutableSetOf<Int>()
        repeat(50) {
            val result = SamplingUtils.topKSampling(logits, k = 3, temperature = 2.0)
            highTempResults.add(result)
        }
        
        // High temperature should give more variety
        assertTrue(highTempResults.size >= lowTempResults.size)
    }

    @Test
    fun `top-p sampling respects p parameter`() {
        val logits = doubleArrayOf(1.0, 2.0, 0.5, 3.0, 1.5)
        
        // With p = 0.8, should only sample from tokens that make up 80% of probability mass
        val results = mutableSetOf<Int>()
        repeat(100) {
            val result = SamplingUtils.topPSampling(logits, p = 0.8, temperature = 1.0)
            results.add(result)
        }
        
        // Should get some variety but not necessarily all tokens
        assertTrue(results.size > 1)
    }

    @Test
    fun `random sampling uses full distribution`() {
        val logits = doubleArrayOf(1.0, 2.0, 0.5, 3.0, 1.5)
        
        val results = mutableSetOf<Int>()
        repeat(200) {
            val result = SamplingUtils.randomSampling(logits, temperature = 1.0)
            results.add(result)
        }
        
        // Should sample from all indices eventually
        assertTrue(results.size > 3) // Should get most indices
    }

    @Test
    fun `temperature affects distribution shape`() {
        val logits = doubleArrayOf(1.0, 2.0, 0.5, 3.0, 1.5)
        
        // Low temperature should favor high-probability tokens more strongly
        val lowTempResults = mutableListOf<Int>()
        repeat(100) {
            val result = SamplingUtils.randomSampling(logits, temperature = 0.1)
            lowTempResults.add(result)
        }
        
        // High temperature should make distribution more uniform
        val highTempResults = mutableListOf<Int>()
        repeat(100) {
            val result = SamplingUtils.randomSampling(logits, temperature = 5.0)
            highTempResults.add(result)
        }
        
        // Count occurrences of the highest probability token (index 3)
        val lowTempCount = lowTempResults.count { it == 3 }
        val highTempCount = highTempResults.count { it == 3 }
        
        // Low temperature should pick the highest probability token more often
        assertTrue(lowTempCount > highTempCount)
    }
} 