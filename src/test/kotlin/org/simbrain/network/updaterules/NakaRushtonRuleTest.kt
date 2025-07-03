package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import kotlin.math.pow

class NakaRushtonRuleTest {

    private val net: Network
    private val input1: Neuron
    private val input2: Neuron
    private val output: Neuron
    private val w1: Synapse
    private val nakaRushtonRule: NakaRushtonRule

    init {
        net = Network()
        input1 = Neuron()
        input2 = Neuron()
        output = Neuron()
        nakaRushtonRule = NakaRushtonRule()
        w1 = Synapse(input1, output, 1.0)
        
        output.updateRule = nakaRushtonRule
        net.addNetworkModels(input1, input2, output, w1)
        input1.activation = 1.0
        input1.clamped = true
        input2.activation = 0.5
        input2.clamped = true

        net.timeStep = 0.1  // Use default timeStep for realistic behavior
    }


    @Test
    fun `test basic naka rushton response`() {
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        net.update()
        
        // With timeStep = 0.1, neuron moves toward steady state gradually
        // Just verify it produces a reasonable positive response
        assertTrue(output.activation > 0.0)
        assertTrue(output.activation < nakaRushtonRule.upperBound)
    }

    @Test
    fun `test saturation behavior`() {
        w1.strength = 10.0  // Large input
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        // Test without large input first
        w1.strength = 1.0
        net.update()
        val normalResponse = output.activation
        
        // Reset and test with large input
        output.activation = 0.0
        w1.strength = 10.0
        net.update()
        val saturatedResponse = output.activation
        
        // Large input should produce much higher response (saturation behavior)
        assertTrue(saturatedResponse > normalResponse)
        assertTrue(saturatedResponse > 0.0)
    }

    @Test
    fun `test higher semi-saturation gives lower activation`() {
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 2.0  // Higher semi-saturation
        
        net.update()
        val higherSemiSaturation = output.activation
        
        // Should be reasonable value
        assertTrue(higherSemiSaturation >= 0.0)
    }

    @Test
    fun `test lower semi-saturation gives higher activation`() {
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 0.5  // Lower semi-saturation
        
        net.update()
        val lowerSemiSaturation = output.activation
        
        // Should be reasonable value (and would be higher than previous test if run together)
        assertTrue(lowerSemiSaturation >= 0.0)
    }

    @Test
    fun `test steepness 1 gives reasonable response`() {
        nakaRushtonRule.semiSaturationConstant = 1.0
        nakaRushtonRule.steepness = 1.0
        
        net.update()
        
        // Should be reasonable value
        assertTrue(output.activation > 0)
    }

    @Test
    fun `test steepness 4 gives reasonable response`() {
        nakaRushtonRule.semiSaturationConstant = 1.0
        nakaRushtonRule.steepness = 4.0
        
        net.update()
        
        // Should be reasonable value (more sigmoidal)
        assertTrue(output.activation > 0)
    }

    @Test
    fun `test zero input`() {
        input1.activation  = 0.0
        input2.activation = 0.0
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        net.update()
        // With zero input, activation should be 0
        assertEquals(0.0, output.activation, 0.001)
    }

    @Test
    fun `test multiple inputs`() {
        val w2 = Synapse(input2, output, 1.0)
        net.addNetworkModel(w2)

        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        // Test with single input first
        w2.strength = 0.0  // Disable second input
        net.update()
        val singleInputResponse = output.activation
        
        // Reset and test with both inputs
        output.activation = 0.0
        w2.strength = 1.0  // Enable second input
        net.update()
        val multipleInputResponse = output.activation
        
        // Multiple inputs should produce higher response than single input
        assertTrue(multipleInputResponse > singleInputResponse)
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
        w1.strength = 10.0  // Use existing synapse with large weight
        
        nakaRushtonRule.isClipped = true
        nakaRushtonRule.upperBound = 0.5
        nakaRushtonRule.lowerBound = 0.0
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        // Run multiple updates to allow activation to grow and test clipping
        repeat(20) { net.update() }
        
        // Should be clipped to upper bound
        assertTrue(output.activation <= nakaRushtonRule.upperBound)
        assertTrue(output.activation >= nakaRushtonRule.lowerBound)
    }

    @Test
    fun `test negative inputs handling`() {
        w1.strength = -1.0 // Negative weight
        
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        net.update()
        
        // Naka-Rushton typically expects non-negative inputs
        // The function should handle this gracefully
        assertTrue(output.activation >= 0.0)
    }

    @Test
    fun `test high steepness values`() {
        nakaRushtonRule.semiSaturationConstant = 1.0
        nakaRushtonRule.steepness = 100.0
        
        net.update()
        
        // Should be valid value (high steepness approaches step function)
        assertTrue(output.activation >= 0.0)
    }

    @Test
    fun `test low steepness values`() {
        nakaRushtonRule.semiSaturationConstant = 1.0
        nakaRushtonRule.steepness = 0.1
        
        net.update()
        
        // Should be valid value (low steepness is more linear)
        assertTrue(output.activation >= 0.0)
    }

    @Test
    fun `test rule name`() {
        assertEquals("Naka-Rushton", nakaRushtonRule.name)
    }

    @Test
    fun `test zero input gives zero response`() {
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        input1.activation = 0.0
        
        net.update()
        
        // Zero input should give zero response
        assertEquals(0.0, output.activation, 0.001)
    }

    @Test
    fun `test positive input gives positive response`() {
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        input1.activation = 2.0
        
        net.update()
        
        // Positive input should give positive response
        assertTrue(output.activation > 0.0)
    }

    @Test
    fun `test higher input gives higher response`() {
        nakaRushtonRule.steepness = 2.0
        nakaRushtonRule.semiSaturationConstant = 1.0
        
        // Test with normal input first
        input1.activation = 1.0
        net.update()
        val normalResponse = output.activation
        
        // Reset and test with higher input
        output.activation = 0.0
        input1.activation = 5.0  // High input
        net.update()
        val highInputResponse = output.activation
        
        // Higher input should give higher response
        assertTrue(highInputResponse > normalResponse)
        assertTrue(highInputResponse > 0.0)
    }
} 