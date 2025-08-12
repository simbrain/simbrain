package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import kotlin.math.abs

class SoftmaxRuleTest {

    val net = Network()
    val rule = SoftmaxRule()
    var na = NeuronArray(2).apply {
        updateRule = rule
    }

    init {
        net.addNetworkModelsAsync(na)
    }

    @Test
    fun `Values should sum to 1`() {
        na.addInputs(doubleArrayOf(.5, .7))
        net.update()
        assertEquals(1.0, na.activationArray.sum(), 1e-10)
    }

    @Test
    fun `Equal inputs should produce equal outputs`() {
        na.addInputs(doubleArrayOf(.85, .85))
        net.update()
        assertEquals(na.activationArray[0], na.activationArray[1], 1e-10)
    }

    @Test
    fun `The component receiving the most input should have the highest value`() {
        na.addInputs(doubleArrayOf(1.0, 0.5))
        net.update()
        assertTrue(na.activationArray[0] > na.activationArray[1])
    }

    @Test
    fun `Temperature less than 1 should create sharper distribution`() {
        rule.temperature = 0.5
        na.addInputs(doubleArrayOf(1.0, 0.8))
        net.update()
        
        // With lower temperature, the difference between outputs should be larger
        val difference = abs(na.activationArray[0] - na.activationArray[1])
        
        // Test with standard temperature (1.0) for comparison
        rule.temperature = 1.0
        na.addInputs(doubleArrayOf(1.0, 0.8))
        net.update()
        val standardDifference = abs(na.activationArray[0] - na.activationArray[1])
        
        // Lower temperature should produce larger difference (sharper distribution)
        assertTrue(difference > standardDifference, 
            "Lower temperature should create sharper distribution. Difference with T=0.5: $difference, with T=1.0: $standardDifference")
    }

    @Test
    fun `Temperature greater than 1 should create flatter distribution`() {
        rule.temperature = 2.0
        na.addInputs(doubleArrayOf(1.0, 0.8))
        net.update()
        
        // With higher temperature, the difference between outputs should be smaller
        val difference = abs(na.activationArray[0] - na.activationArray[1])
        
        // Test with standard temperature (1.0) for comparison
        rule.temperature = 1.0
        na.addInputs(doubleArrayOf(1.0, 0.8))
        net.update()
        val standardDifference = abs(na.activationArray[0] - na.activationArray[1])
        
        // Higher temperature should produce smaller difference (flatter distribution)
        assertTrue(difference < standardDifference, 
            "Higher temperature should create flatter distribution. Difference with T=2.0: $difference, with T=1.0: $standardDifference")
    }

    @Test
    fun `Very low temperature should approach one-hot encoding`() {
        rule.temperature = 0.1
        na.addInputs(doubleArrayOf(1.0, 0.5))
        net.update()
        
        // With very low temperature, the highest input should get almost all the probability
        assertTrue(na.activationArray[0] > 0.9, "Very low temperature should concentrate probability on highest input")
        assertTrue(na.activationArray[1] < 0.1, "Very low temperature should minimize probability on lower inputs")
    }

    @Test
    fun `Very high temperature should approach uniform distribution`() {
        rule.temperature = 10.0
        na.addInputs(doubleArrayOf(1.0, 0.5))
        net.update()
        
        // With very high temperature, outputs should be more uniform
        val difference = abs(na.activationArray[0] - na.activationArray[1])
        assertTrue(difference < 0.1, "Very high temperature should create nearly uniform distribution")
    }

    @Test
    fun `Test with three neurons`() {
        val na3 = NeuronArray(3).apply {
            updateRule = SoftmaxRule()
        }
        net.addNetworkModelsAsync(na3)
        
        na3.clearInputs()
        na3.addInputs(doubleArrayOf(1.0, 0.5, 0.2))
        net.update()
        
        // Values should sum to 1
        assertEquals(1.0, na3.activationArray.sum(), 1e-10)
        
        // Should be ordered by input values
        assertTrue(na3.activationArray[0] > na3.activationArray[1])
        assertTrue(na3.activationArray[1] > na3.activationArray[2])
    }

    @Test
    fun `Test temperature effect on three neurons`() {
        val na3 = NeuronArray(3).apply {
            updateRule = SoftmaxRule()
        }
        net.addNetworkModelsAsync(na3)
        
        na3.clearInputs()
        na3.addInputs(doubleArrayOf(1.0, 0.8, 0.6))
        
        // Test with low temperature
        (na3.updateRule as SoftmaxRule).temperature = 0.5
        net.update()
        val lowTempActivations = na3.activationArray.clone()
        
        // Test with high temperature
        (na3.updateRule as SoftmaxRule).temperature = 2.0
        net.update()
        val highTempActivations = na3.activationArray.clone()
        
        // Low temperature should create more disparity between highest and lowest
        val lowTempDisparity = lowTempActivations[0] - lowTempActivations[2]
        val highTempDisparity = highTempActivations[0] - highTempActivations[2]
        
        assertTrue(lowTempDisparity > highTempDisparity, 
            "Low temperature should create larger disparity between highest and lowest values")
    }

    @Test
    fun `Test with extreme input values`() {
        na.addInputs(doubleArrayOf(100.0, 99.0))
        net.update()
        
        // Should still sum to 1 and maintain relative ordering
        assertEquals(1.0, na.activationArray.sum(), 1e-10)
        assertTrue(na.activationArray[0] > na.activationArray[1])
    }

    @Test
    fun `Test with negative input values`() {
        na.addInputs(doubleArrayOf(-1.0, -2.0))
        net.update()
        
        // Should still sum to 1 and maintain relative ordering
        assertEquals(1.0, na.activationArray.sum(), 1e-10)
        assertTrue(na.activationArray[0] > na.activationArray[1]) // -1 > -2, so first should be higher
    }

    @Test
    fun `Test with very small input differences`() {
        na.addInputs(doubleArrayOf(1.0, 0.999))
        net.update()
        
        // Should still sum to 1
        assertEquals(1.0, na.activationArray.sum(), 1e-10)
        
        // With standard temperature, should still have some difference
        assertTrue(na.activationArray[0] > na.activationArray[1])
    }

    @Test
    fun `Test copy function preserves temperature`() {
        rule.temperature = 0.7
        val copiedRule = rule.copy()
        
        assertEquals(0.7, copiedRule.temperature, 1e-10)
        assertTrue(copiedRule is SoftmaxRule)
    }

    @Test
    fun `Test bounds are correctly set`() {
        assertEquals(1.0, rule.upperBound, 1e-10)
        assertEquals(0.0, rule.lowerBound, 1e-10)
    }

    // TODO: The softmax derivative is questionable
    //@Test
    //fun `Test derivative`() {
    //    print(rule.getDerivative(doubleArrayOf(.1, .2, .3).toMatrix()))
    //}

}
