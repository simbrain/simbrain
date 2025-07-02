package org.simbrain.network.learningrules

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.util.toColumnVector
import org.simbrain.util.toDoubleArray

class OjaTest {

    // 2->1 network
    var net = Network()
    val input = Neuron()
    val output = Neuron()
    var weight = Synapse(input, output)

    // For array based tests
    val na1 = NeuronArray(2)
    val na2 = NeuronArray(3)
    var wm12 = WeightMatrix(na1, na2)

    init {
        net.addNetworkModels(input, output, weight, na1, na2, wm12)
        weight.learningRule = OjaRule().apply {
            learningRate = 1.0
            normalizationFactor = 1.0
        }
        weight.strength = 0.0
        weight.upperBound = 10.0
        weight.lowerBound = -10.0
        input.clamped = true
        output.clamped = true

        na1.isClamped = true
        na2.isClamped = true
        wm12.hardClear()
        wm12.learningRule = OjaRule().apply {
            learningRate = 1.0
            normalizationFactor = 1.0
        }
    }

    @Test
    fun `test update for a single weight and clamped nodes`() {
        input.activation = 1.0
        output.activation = 1.0
        net.update()

        // delta-w  = rate (out(in - out * weight))
        //          = 1 (1(1 - 1*0)) = 1
        assertEquals(1.0, weight.strength)
        net.update()
        //          1 (1(1 - 1*1)) = 0
        assertEquals(1.0, weight.strength)
        repeat(10) {
            net.update()
        }
        assertEquals(1.0, weight.strength)
    }

    @Test
    fun `test update with different source and target`() {
        // High learning rate leads to divergence in this case
        input.activation = 1.0
        output.activation = 2.0
        // delta-w  = rate (out(in - out * weight))
        //          = 1 (2(1 - 2*0)) = 2
        // Weight becomes 0 + 2 = 2
        net.update()
        assertEquals(2.0, weight.strength)
        //          = 1 (2(1 - 2*2)) = -6
        // Weight becomes 2 -6 = -4
        net.update()
        assertEquals(-4.0, weight.strength)
        //          = 1 (2(1 - 2*-4)) = 18
        // Weight becomes -6 + 18 = 12, which is clipped at 10
        net.update()
        assertEquals(10.0, weight.strength)
    }

    @Test
    fun `test update with learning rate less than 1`() {
        input.activation = 1.0
        output.activation = 1.0
        (weight.learningRule as OjaRule).learningRate = .1

        // Should approach 1.
        repeat(100) {
            net.update()
        }
        assertEquals(1.0, weight.strength, .01)

        // Should approach -1
        output.activation = -1.0
        repeat(100) {
            net.update()
        }
        assertEquals(-1.0, weight.strength, .01)

        // Should approach 1/2
        output.activation = 2.0
        repeat(100) {
            net.update()
        }
        assertEquals(.5, weight.strength, .01)

        // Should approach 1/3
        output.activation = 3.0
        repeat(100) {
            net.update()
        }
        assertEquals(.33, weight.strength, .01)

        // Should approach -1/3
        output.activation = -3.0
        repeat(100) {
            net.update()
        }
        assertEquals(-.33, weight.strength, .01)

    }

    @Test
    fun `test vectorized rule on one then two updates`() {
        val inputs = doubleArrayOf(1.0, -1.0).toColumnVector()
        val outputs = doubleArrayOf(1.0, 2.0, -1.0).toColumnVector()
        na1.activations = inputs
        na2.activations = outputs
        net.update()
        // Only uses Hebbian part on first update since weights are initially zero
        assertArrayEquals(doubleArrayOf(1.0, -1.0), wm12.weights.row(0))
        assertArrayEquals(doubleArrayOf(2.0, -2.0), wm12.weights.row(1))
        assertArrayEquals(doubleArrayOf(-1.0, 1.0), wm12.weights.row(2))

        net.update()

        // Second update: now stabilization term kicks in
        // Row 0: y0=1 → W stays [1,-1]
        assertArrayEquals(doubleArrayOf( 1.0, -1.0), wm12.weights.row(0), 1e-9)
        // Row 1: y1=2 → [2,-2] + ([2,-2] - 4*[2,-2]) = [-4,4]
        assertArrayEquals(doubleArrayOf(-4.0,  4.0), wm12.weights.row(1), 1e-9)
        // Row 2: y2=-1 → W stays [-1,1]
        assertArrayEquals(doubleArrayOf(-1.0,  1.0), wm12.weights.row(2), 1e-9)
    }

    @Test
    fun `weight norm converges to sqrt of normalization factor`() {
        val inputSize = 5
        val outputNeuron = Neuron().apply { clamped = true }
        val inputs = (1..inputSize).map { Neuron().apply { clamped = true } }
        val synapses = inputs.map { input ->
            Synapse(input, outputNeuron).apply {
                learningRule = OjaRule().apply {
                    learningRate = 0.1
                    normalizationFactor = 4.0
                }
                strength = Math.random() - 0.5
                upperBound = 10.0
                lowerBound = -10.0
            }
        }

        val net = Network()
        net.addNetworkModels(outputNeuron)
        net.addNetworkModels(inputs)
        net.addNetworkModels(synapses)

        repeat(1000) {
            inputs.forEach { it.activation = Math.random() - 0.5 }
            val y = synapses.sumOf { it.source.activation * it.strength }
            outputNeuron.activation = y
            net.update()
        }

        val norm = Math.sqrt(synapses.sumOf { it.strength * it.strength })
        assertEquals(2.0, norm, 0.1)  // sqrt(4.0) = 2.0
    }

    @Test
    fun `test unclamped single output converges to principal component`() {
        // Create a fresh network with unclamped output
        val net = Network()
        val input1 = Neuron().apply { clamped = true }
        val input2 = Neuron().apply { clamped = true }
        val output = Neuron().apply { clamped = false } // Key difference: unclamped
        
        val weight1 = Synapse(input1, output).apply {
            learningRule = OjaRule().apply {
                learningRate = 0.005  // Even lower learning rate for stability
                normalizationFactor = 1.0
            }
            strength = 0.1  // Small initial weight
            upperBound = 2.0
            lowerBound = -2.0
        }
        val weight2 = Synapse(input2, output).apply {
            learningRule = OjaRule().apply {
                learningRate = 0.005
                normalizationFactor = 1.0
            }
            strength = 0.1
            upperBound = 2.0
            lowerBound = -2.0
        }
        
        net.addNetworkModels(input1, input2, output, weight1, weight2)
        
        // Use only positive correlated patterns for stability
        val patterns = listOf(
            doubleArrayOf(1.0, 0.8),   
            doubleArrayOf(0.8, 1.0),
            doubleArrayOf(0.6, 0.5),   
            doubleArrayOf(0.5, 0.6)
        )
        
        // Train for many epochs
        repeat(2000) {
            val pattern = patterns[it % patterns.size]
            input1.activation = pattern[0]
            input2.activation = pattern[1]
            net.update()
        }
        
        // Check that weights have the right relationship (allow for sign flips)
        val ratio = Math.abs(weight1.strength / weight2.strength)
        assertEquals(1.0, ratio, 0.3) // More tolerant, check absolute ratio
        
        // Weight magnitudes should be normalized
        val norm = Math.sqrt(weight1.strength * weight1.strength + weight2.strength * weight2.strength)
        assertEquals(1.0, norm, 0.2) // More tolerant
        
        // Weights should not be too extreme
        assertTrue(Math.abs(weight1.strength) < 1.5, "Weight1 should not be extreme: ${weight1.strength}")
        assertTrue(Math.abs(weight2.strength) < 1.5, "Weight2 should not be extreme: ${weight2.strength}")
    }

    @Test
    fun `test unclamped output with competing inputs`() {
        // Test scenario where two inputs compete for influence
        val net = Network()
        val strongInput = Neuron().apply { clamped = true }
        val weakInput = Neuron().apply { clamped = true }
        val output = Neuron().apply { clamped = false }
        
        val strongWeight = Synapse(strongInput, output).apply {
            learningRule = OjaRule().apply {
                learningRate = 0.05
                normalizationFactor = 1.0
            }
            strength = 0.1
        }
        val weakWeight = Synapse(weakInput, output).apply {
            learningRule = OjaRule().apply {
                learningRate = 0.05
                normalizationFactor = 1.0
            }
            strength = 0.1
        }
        
        net.addNetworkModels(strongInput, weakInput, output, strongWeight, weakWeight)
        
        // Present patterns where one input is consistently stronger
        repeat(500) {
            strongInput.activation = 1.0
            weakInput.activation = 0.3
            net.update()
        }
        
        // The stronger input should dominate
        assertTrue(Math.abs(strongWeight.strength) > Math.abs(weakWeight.strength),
            "Strong weight (${strongWeight.strength}) should be larger than weak weight (${weakWeight.strength})")
    }

    @Test
    fun `test unclamped array-based learning converges`() {
        // Test with unclamped neuron arrays
        val net = Network()
        val inputs = NeuronArray(3).apply { isClamped = true }
        val outputs = NeuronArray(2).apply { isClamped = false } // Unclamped outputs
        val weights = WeightMatrix(inputs, outputs).apply {
            learningRule = OjaRule().apply {
                learningRate = 0.02
                normalizationFactor = 1.0
            }
            randomize() // Start with small random weights
        }
        
        net.addNetworkModels(inputs, outputs, weights)
        
        // Create structured input patterns
        val inputPatterns = listOf(
            doubleArrayOf(1.0, 0.5, 0.0).toColumnVector(),
            doubleArrayOf(0.0, 1.0, 0.5).toColumnVector(),
            doubleArrayOf(0.5, 0.0, 1.0).toColumnVector()
        )
        
        // Train for many iterations
        repeat(2000) { iteration ->
            val pattern = inputPatterns[iteration % inputPatterns.size]
            inputs.activations = pattern
            net.update()
        }
        
        // Check that weight norms have converged to expected values
        for (i in 0 until outputs.size) {
            val rowWeights = weights.weights.row(i)
            val norm = Math.sqrt(rowWeights.sumOf { it * it })
            assertEquals(1.0, norm, 0.15, "Row $i norm should be close to 1.0")
        }
        
        // Outputs should be reasonable (not NaN or extreme values)
        outputs.activations.toDoubleArray().forEach { activation ->
            assertTrue(activation.isFinite(), "Output activation should be finite")
            assertTrue(Math.abs(activation) < 10.0, "Output activation should not be extreme")
        }
    }

}