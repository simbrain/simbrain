package org.simbrain.network.learningrules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SpikingThresholdRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData

/**
 * Test to compare scalar and matrix implementations of learning rules.
 * Ensures that both versions produce equivalent results.
 */
class MatrixScalarComparisonTest {

    private lateinit var network: Network
    private val tolerance = 1e-10

    @BeforeEach
    fun setUp() {
        network = Network()
    }

    @Test
    fun testOjaRuleEquivalence() {
        testLearningRuleEquivalence(OjaRule().apply {
            learningRate = 0.1
            normalizationFactor = 1.0
        })
    }

    @Test
    fun testHebbianRuleEquivalence() {
        testLearningRuleEquivalence(HebbianRule().apply {
            learningRate = 0.1
            forgettingRate = 0.05
        })
    }

    @Test
    fun testHebbianRuleEquivalenceNoForgetting() {
        testLearningRuleEquivalence(HebbianRule().apply {
            learningRate = 0.1
            forgettingRate = 0.0
        })
    }

    //@Test
    //fun testHebbianThresholdRuleEquivalence() {
    //    testLearningRuleEquivalence(HebbianThresholdRule().apply {
    //        learningRate = 0.1
    //        outputThreshold = 0.5
    //        useSlidingOutputThreshold = false
    //    })
    //}

    //@Test
    //fun testHebbianThresholdRuleEquivalenceWithSlidingThreshold() {
    //    testLearningRuleEquivalence(HebbianThresholdRule().apply {
    //        learningRate = 0.1
    //        outputThreshold = 0.5
    //        outputThresholdMomentum = 0.1
    //        useSlidingOutputThreshold = true
    //    })
    //}

    @Test
    fun testHebbianCPCARuleEquivalence() {
        testLearningRuleEquivalence(HebbianCPCARule().apply {
            learningRate = 0.1
            m = 1.0
            theta = 0.5
            lambda = 1.0
        })
    }

    @Test
    fun testStaticSynapseRuleEquivalence() {
        testLearningRuleEquivalence(StaticSynapseRule())
    }

    @Test
    fun testSubtractiveNormalizationRuleEquivalence() {
        testLearningRuleEquivalence(SubtractiveNormalizationRule().apply {
            learningRate = 0.1
        })
    }

    //@Test
    //fun testShortTermPlasticityRuleEquivalence() {
    //    testLearningRuleEquivalence(ShortTermPlasticityRule().apply {
    //        plasticityType = 0 // STD
    //        firingThreshold = 0.5
    //        baseLineStrength = 1.0
    //        bumpRate = 0.5
    //        decayRate = 0.2
    //    })
    //}

    private fun testLearningRuleEquivalence(rule: SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>) {
        val inputSize = 3
        val outputSize = 2

        // 1) Scalar version
        // Use i.toDouble() so activations are 0.0, 1.0, 2.0
        val scalarInputs = (0 until inputSize).map { i ->
            Neuron().apply {
                updateRule = LinearRule()
                activation = i.toDouble()
            }
        }
        // Use (i+1).toDouble() so you don’t get a 0.0 output on the first neuron
        val scalarOutputs = (0 until outputSize).map { i ->
            Neuron().apply {
                updateRule = LinearRule()
                activation = (i + 1).toDouble()
            }
        }

        // Build synapses with weight = (i+j).toDouble()
        val scalarSynapses = mutableListOf<Synapse>()
        val initialWeights = Array(outputSize) { DoubleArray(inputSize) }
        for (i in 0 until outputSize) {
            for (j in 0 until inputSize) {
                val weight = (i + j).toDouble()
                Synapse(scalarInputs[j], scalarOutputs[i], weight).also { syn ->
                    syn.learningRule = rule.copy() as SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>
                    syn.learningRule.init(syn)
                    scalarSynapses += syn
                    initialWeights[i][j] = weight
                }
            }
        }

        // 2) Matrix version
        val matrixInput = NeuronArray(inputSize)
        val matrixOutput = NeuronArray(outputSize)
        // Copy activations
        scalarInputs.forEachIndexed { i, n -> matrixInput.activations[i, 0] = n.activation }
        scalarOutputs.forEachIndexed { i, n -> matrixOutput.activations[i, 0] = n.activation }

        val weightMatrix = WeightMatrix(matrixInput, matrixOutput)
        // Copy weights
        for (i in 0 until outputSize)
            for (j in 0 until inputSize)
                weightMatrix.weights[i, j] = initialWeights[i][j]

        // 3) Compare updates
        val numUpdates = 5
        with(network) {
            repeat(numUpdates) { update ->
                // scalar
                scalarSynapses.forEach { syn -> rule.apply(syn, EmptyScalarData) }
                // matrix
                val matrixRule = rule.copy() as SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>
                matrixRule.apply(weightMatrix, EmptyMatrixData)

                // assert equality
                for (i in 0 until outputSize) for (j in 0 until inputSize) {
                    val s = scalarSynapses[i * inputSize + j].strength
                    val m = weightMatrix.weights[i, j]
                    assertEquals(s, m, tolerance,
                        "[$i,$j] mismatch after update $update: scalar=$s, matrix=$m")
                }
            }
        }
    }


    @Test
    fun testLargerMatrixEquivalence() {
        // Test with larger matrices to ensure scalability
        val inputSize = 10
        val outputSize = 8
        
        val rule = HebbianRule().apply {
            learningRate = 0.05
            forgettingRate = 0.01
        }

        // Create test data
        val scalarInputs = (0 until inputSize).map { 
            Neuron().apply { 
                updateRule = LinearRule()
                activation = Math.random() 
            }
        }
        val scalarOutputs = (0 until outputSize).map { 
            Neuron().apply { 
                updateRule = LinearRule()
                activation = Math.random() 
            }
        }

        val scalarSynapses = mutableListOf<Synapse>()
        
        // Create synapses
        for (i in 0 until outputSize) {
            for (j in 0 until inputSize) {
                val weight = Math.random() * 2 - 1 // Random weight between -1 and 1
                val synapse = Synapse(scalarInputs[j], scalarOutputs[i], weight)
                synapse.learningRule = rule.copy() as SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>
                synapse.learningRule.init(synapse)
                scalarSynapses.add(synapse)
            }
        }

        // Create matrix version
        val matrixInput = NeuronArray(inputSize)
        val matrixOutput = NeuronArray(outputSize)
        val weightMatrix = WeightMatrix(matrixInput, matrixOutput)
        
        // Set activations and weights to match scalar version
        for (i in 0 until inputSize) {
            matrixInput.activations[i, 0] = scalarInputs[i].activation
        }
        for (i in 0 until outputSize) {
            matrixOutput.activations[i, 0] = scalarOutputs[i].activation
        }
        for (i in 0 until outputSize) {
            for (j in 0 until inputSize) {
                weightMatrix.weights[i, j] = scalarSynapses[i * inputSize + j].strength
            }
        }

        with(network) {
            // Apply single update
            scalarSynapses.forEach { synapse ->
                rule.apply(synapse, EmptyScalarData)
            }

            val matrixRule = rule.copy() as SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>
            matrixRule.apply(weightMatrix, EmptyMatrixData)

            // Compare results
            for (i in 0 until outputSize) {
                for (j in 0 until inputSize) {
                    val scalarWeight = scalarSynapses[i * inputSize + j].strength
                    val matrixWeight = weightMatrix.weights[i, j]
                    
                    assertEquals(
                        scalarWeight, 
                        matrixWeight, 
                        tolerance,
                        "Large matrix weights differ at position [$i,$j]: scalar=$scalarWeight, matrix=$matrixWeight"
                    )
                }
            }
        }
    }

    @Test
    fun testSTDPRuleEquivalence() {
        val stdpRule = STDPRule().apply {
            learningRate = 0.01
            wPlus = 1.0
            wMinus = 1.0
            tauPlus = 20.0
            tauMinus = 20.0
        }
        testSpikingLearningRuleEquivalence(stdpRule)
    }

    @Test
    fun testLogSTDPRuleEquivalence() {
        val logStdpRule = LogSTDPRule().apply {
            learningRate = 0.01
            wPlus = 1.0
            wMinus = 1.0
            tauPlus = 20.0
            tauMinus = 20.0
        }
        testSpikingLearningRuleEquivalence(logStdpRule)
    }

    //@Test
    //fun testPfisterGerstnerRuleEquivalence() {
    //    val pfisterRule = PfisterGerstner2006Rule().apply {
    //        a2P = 0.5
    //        a2N = 0.5
    //    }
    //    testSpikingLearningRuleEquivalence(pfisterRule)
    //}

    private fun testSpikingLearningRuleEquivalence(rule: SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>) {
        val inputSize = 2
        val outputSize = 2

        // Create scalar version with spiking neurons
        val scalarInputs = (0 until inputSize).map { i ->
            Neuron().apply { 
                updateRule = SpikingThresholdRule().apply { threshold = 0.5 }
                activation = if (i == 0) 0.8 else 0.3  // First neuron will spike
            }
        }
        val scalarOutputs = (0 until outputSize).map { i ->
            Neuron().apply { 
                updateRule = SpikingThresholdRule().apply { threshold = 0.5 }
                activation = if (i == 0) 0.7 else 0.2  // First neuron will spike
            }
        }

        val scalarSynapses = mutableListOf<Synapse>()
        val initialWeights = Array(outputSize) { DoubleArray(inputSize) }

        // Create synapses and store initial weights
        for (i in 0 until outputSize) {
            for (j in 0 until inputSize) {
                val weight = 0.5 + (i * inputSize + j) * 0.1
                val synapse = Synapse(scalarInputs[j], scalarOutputs[i], weight)
                synapse.learningRule = rule.copy() as SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>
                synapse.learningRule.init(synapse)
                scalarSynapses.add(synapse)
                initialWeights[i][j] = weight
            }
        }

        // Create matrix version with spiking neurons
        val matrixInput = NeuronArray(inputSize).apply {
            updateRule = SpikingThresholdRule().apply { threshold = 0.5 }
        }
        val matrixOutput = NeuronArray(outputSize).apply {
            updateRule = SpikingThresholdRule().apply { threshold = 0.5 }
        }
        
        // Set activations to match scalar version
        matrixInput.activations[0, 0] = 0.8  // Will spike
        matrixInput.activations[1, 0] = 0.3  // Won't spike
        matrixOutput.activations[0, 0] = 0.7  // Will spike
        matrixOutput.activations[1, 0] = 0.2  // Won't spike

        val weightMatrix = WeightMatrix(matrixInput, matrixOutput)
        
        // Set initial weights to match scalar version
        for (i in 0 until outputSize) {
            for (j in 0 until inputSize) {
                weightMatrix.weights[i, j] = initialWeights[i][j]
            }
        }

        // Add to network and update to trigger spikes
        network.addNetworkModels(scalarInputs + scalarOutputs + scalarSynapses)
        network.addNetworkModels(matrixInput, matrixOutput, weightMatrix)
        
        with(network) {
            // First update to trigger spikes
            update()
            
            // Apply learning rules
            scalarSynapses.forEach { synapse ->
                rule.apply(synapse, EmptyScalarData)
            }

            val matrixRule = rule.copy() as SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>
            matrixRule.apply(weightMatrix, EmptyMatrixData)

            // Compare results - we expect weights to be different after STDP learning
            // but scalar and matrix versions should be identical
            for (i in 0 until outputSize) {
                for (j in 0 until inputSize) {
                    val scalarWeight = scalarSynapses[i * inputSize + j].strength
                    val matrixWeight = weightMatrix.weights[i, j]
                    
                    assertEquals(
                        scalarWeight, 
                        matrixWeight, 
                        tolerance,
                        "Spiking rule weights differ at position [$i,$j]: scalar=$scalarWeight, matrix=$matrixWeight"
                    )
                }
            }
        }
    }
} 