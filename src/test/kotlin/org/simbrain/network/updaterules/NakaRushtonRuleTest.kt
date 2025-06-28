package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import kotlin.math.pow

class NakaRushtonRuleTest {

    val net = Network()
    val input1 = Neuron()
    val input2 = Neuron()
    val output = Neuron()
    val nakaRushtonRule = NakaRushtonRule()

    init {
        output.updateRule = nakaRushtonRule
        net.addNetworkModels(input1, input2, output)
        
        input1.activation = 1.0
        input1.clamped = true
        input2.activation = 0.5
        input2.clamped = true
    }

    @Test
    fun `test basic naka rushton response`() {
        val synapse = Synapse(input1, output)
        synapse.strength = 1.0
        net.addNetworkModel(synapse)
        
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        net.update()
        
        val inputSum = 1.0 // from input1
        val expected = inputSum.pow(nakaRushtonRule.steepness) / 
                      (nakaRushtonRule.semiSaturationConstant.pow(nakaRushtonRule.steepness) + 
                       inputSum.pow(nakaRushtonRule.steepness))
        
        assertEquals(expected, output.activation, 0.001)
    }

    @Test
    fun `test saturation behavior`() {
        val synapse = Synapse(input1, output)
        synapse.strength = 10.0  // Large input
        net.addNetworkModel(synapse)
        
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        net.update()
        
        // With very large input, should approach 1.0
        assertTrue(output.activation > 0.9)
        assertTrue(output.activation <= 1.0)
    }

    @Test
    fun `test semi-saturation constant effect`() {
        val synapse = Synapse(input1, output)
        synapse.strength = 1.0
        net.addNetworkModel(synapse)
        
        nakaRushtonRule.steepness = 2.0
        
        // Test with semi-saturation = 1.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        net.update()
        val activation1 = output.activation
        
        // Test with semi-saturation = 0.5 (should give higher activation)
        nakaRushtonRule.semiSaturationConstant = 0.5
        net.update()
        val activation2 = output.activation
        
        // Lower semi-saturation should yield higher activation for same input
        assertTrue(activation2 > activation1)
    }

    @Test
    fun `test steepness parameter effect`() {
        val synapse = Synapse(input1, output)
        synapse.strength = 1.0
        net.addNetworkModel(synapse)
        
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        // Test with steepness = 1.0
        nakaRushtonRule.steepness = 1.0
        net.update()
        val activation1 = output.activation
        
        // Test with steepness = 4.0 (should be more sigmoidal)
        nakaRushtonRule.steepness = 4.0
        net.update()
        val activation2 = output.activation
        
        // Both should be reasonable values
        assertTrue(activation1 > 0)
        assertTrue(activation1 < 1)
        assertTrue(activation2 > 0)
        assertTrue(activation2 < 1)
    }

    @Test
    fun `test zero input`() {
        // No synapses, so input should be 0
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        net.update()
        
        // With zero input, activation should be 0
        assertEquals(0.0, output.activation, 0.001)
    }

    @Test
    fun `test multiple inputs`() {
        val synapse1 = Synapse(input1, output)
        val synapse2 = Synapse(input2, output)
        synapse1.strength = 1.0
        synapse2.strength = 0.8
        net.addNetworkModels(synapse1, synapse2)
        
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        net.update()
        
        val totalInput = 1.0 * 1.0 + 0.5 * 0.8 // 1.4
        val expected = totalInput.pow(nakaRushtonRule.steepness) / 
                      (nakaRushtonRule.semiSaturationConstant.pow(nakaRushtonRule.steepness) + 
                       totalInput.pow(nakaRushtonRule.steepness))
        
        assertEquals(expected, output.activation, 0.001)
    }

    @Test
    fun `test time type`() {
        assertEquals(Network.TimeType.DISCRETE, nakaRushtonRule.timeType)
    }

    @Test
    fun `test copy`() {
        nakaRushtonRule.steepness = 3.5
        nakaRushtonRule.semiSaturationConstant = 2.5
        nakaRushtonRule.upperBound = 10.0
        nakaRushtonRule.lowerBound = -5.0
        nakaRushtonRule.isClipped = true
        
        val copy = nakaRushtonRule.copy()
        
        assertEquals(nakaRushtonRule.steepness, copy.steepness)
        assertEquals(nakaRushtonRule.semiSaturationConstant, copy.semiSaturationConstant)
        assertEquals(nakaRushtonRule.upperBound, copy.upperBound)
        assertEquals(nakaRushtonRule.lowerBound, copy.lowerBound)
        assertEquals(nakaRushtonRule.isClipped, copy.isClipped)
    }

    @Test
    fun `test clipping behavior`() {
        val synapse = Synapse(input1, output)
        synapse.strength = 10.0
        net.addNetworkModel(synapse)
        
        nakaRushtonRule.isClipped = true
        nakaRushtonRule.upperBound = 0.5
        nakaRushtonRule.lowerBound = 0.0
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        net.update()
        
        // Should be clipped to upper bound
        assertTrue(output.activation <= nakaRushtonRule.upperBound)
        assertTrue(output.activation >= nakaRushtonRule.lowerBound)
    }

    @Test
    fun `test negative inputs handling`() {
        val synapse = Synapse(input1, output)
        synapse.strength = -1.0 // Negative weight
        net.addNetworkModel(synapse)
        
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        net.update()
        
        // Naka-Rushton typically expects non-negative inputs
        // The function should handle this gracefully
        assertTrue(output.activation >= 0.0)
    }

    @Test
    fun `test extreme steepness values`() {
        val synapse = Synapse(input1, output)
        synapse.strength = 1.0
        net.addNetworkModel(synapse)
        
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        // Test very high steepness (should approach step function)
        nakaRushtonRule.steepness = 100.0
        net.update()
        val highSteepnessActivation = output.activation
        
        // Test very low steepness (should be more linear)
        nakaRushtonRule.steepness = 0.1
        net.update()
        val lowSteepnessActivation = output.activation
        
        // Both should be valid values
        assertTrue(highSteepnessActivation >= 0.0 && highSteepnessActivation <= 1.0)
        assertTrue(lowSteepnessActivation >= 0.0 && lowSteepnessActivation <= 1.0)
    }

    @Test
    fun `test rule name`() {
        assertEquals("Naka-Rushton", nakaRushtonRule.name)
    }

    @Test
    fun `test contrast response curve shape`() {
        val synapse = Synapse(input1, output)
        synapse.strength = 1.0
        net.addNetworkModel(synapse)
        
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        val responses = mutableListOf<Double>()
        
        // Test different input levels
        val inputs = listOf(0.0, 0.5, 1.0, 2.0, 5.0)
        inputs.forEach { inputLevel ->
            input1.activation = inputLevel
            net.update()
            responses.add(output.activation)
        }
        
        // Response should be monotonically increasing
        for (i in 1 until responses.size) {
            assertTrue(responses[i] >= responses[i-1], 
                      "Response should increase with input: ${responses[i-1]} -> ${responses[i]}")
        }
        
        // Should start near 0 and approach 1
        assertTrue(responses.first() < 0.1) // Near zero for low input
        assertTrue(responses.last() > 0.8)  // Near saturation for high input
    }
} 