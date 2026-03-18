package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.totalFanInStrength
import org.simbrain.network.subnetworks.CompetitiveNetwork

class CompetitiveGroupTest {

    var net = Network()
    val competitive = CompetitiveNetwork(2, 2)

    init {
        net.addNetworkModelsAsync(competitive)
    }

    @Test
    fun `Test copy function`() {
        competitive.learningRate = .8
        val competitive2 = competitive.copy()
        net.addNetworkModelsAsync(competitive2)
        assertEquals(2, competitive2.competitive.neuronList.size)
        assertEquals(competitive.updateMethod, competitive2.updateMethod)
        assertEquals(competitive.learningRate, competitive2.learningRate)
        assertEquals(competitive.winValue, competitive2.winValue)
        assertEquals(competitive.loseValue, competitive2.loseValue)
        assertEquals(competitive.normalizeInputs, competitive2.normalizeInputs)
        assertEquals(competitive.useLeakyLearning, competitive2.useLeakyLearning)
        assertEquals(competitive.leakyLearningRate, competitive2.leakyLearningRate)
        assertEquals(competitive.synapseDecayPercent, competitive2.synapseDecayPercent)
        assertEquals(competitive.useActivationDynamics, competitive2.useActivationDynamics)
        assertEquals(competitive.activationDecay, competitive2.activationDecay)
        assertEquals(competitive.addNoise, competitive2.addNoise)
    }

    @Test
    fun `one node wins and takes win value`() {
        competitive.winValue = 2.0
        competitive.loseValue = 0.0
        competitive.inputLayer.activationArray = doubleArrayOf(1.0, 0.0)
        repeat(2) {
            net.update()
        }
        assertEquals(2.0, competitive.competitive.activations.sum())
    }

    @Test
    fun `network learns two patterns`() {
        // Pattern 1
        competitive.inputLayer.activationArray = doubleArrayOf(1.0, 0.0)
        repeat(2) {
            net.update()
        }
        // Pattern 2
        competitive.inputLayer.activationArray = doubleArrayOf(0.0, 1.0)
        repeat(2) {
            net.update()
        }

        // Test retrieval
        competitive.inputLayer.activationArray = doubleArrayOf(1.0, 0.0)
        net.update()
        val winner1 = competitive.competitive.neuronList[competitive.competitive.activationArray.indexOfFirst { it == 1.0 }]
        competitive.inputLayer.activationArray = doubleArrayOf(0.0, 0.1)
        net.update()
        val winner2 = competitive.competitive.neuronList[competitive.competitive.activationArray.indexOfFirst { it == 1.0 }]
        assertNotEquals(winner1, winner2)
    }

    @Test
    fun `test normalize weights`() {
        competitive.normalizeIncomingWeights()
        assertEquals(1.0, competitive.competitive.neuronList[0].fanIn.sumOf { it.strength })
        assertEquals(1.0, competitive.competitive.neuronList[1].fanIn.sumOf { it.strength })
    }

    @Test
    fun `test leaky learning`() {
        competitive.useLeakyLearning = true
        // Set weights to low known values so leaky learning clearly increases them
        competitive.weights.synapses.forEach { it.strength = 0.1 }
        competitive.inputLayer.activationArray = doubleArrayOf(1.0, 0.0)
        net.update()
        val loser = competitive.competitive.neuronList[competitive.competitive.activationArray.indexOfFirst { it == 0.0 }]
        val before = loser.totalFanInStrength()
        net.update()
        val after = loser.totalFanInStrength()
        assertTrue(after > before)
    }

    @Test
    fun `test normalization enabled vs disabled`() {
        val net2 = Network()
        val competitive2 = CompetitiveNetwork(2, 2)
        net2.addNetworkModelsAsync(competitive2)

        competitive.normalizeInputs = true
        competitive2.normalizeInputs = false

        competitive.inputLayer.activationArray = doubleArrayOf(0.5, 0.5)
        competitive2.inputLayer.activationArray = doubleArrayOf(1.0, 1.0)

        repeat(3) {
            net.update()
            net2.update()
        }

        val winner1 = competitive.competitive.neuronList[competitive.competitive.activationArray.indexOfFirst { it == 1.0 }]
        val winner2 = competitive2.competitive.neuronList[competitive2.competitive.activationArray.indexOfFirst { it == 1.0 }]

        assertNotNull(winner1)
        assertNotNull(winner2)
    }

    @Test
    fun `test Alvarez-Squire decay`() {
        competitive.updateMethod = CompetitiveNetwork.UpdateMethod.ALVAREZ_SQUIRE
        competitive.synapseDecayPercent = 0.1
        competitive.learningRate = 0.0  // Disable learning so only decay occurs

        competitive.inputLayer.activationArray = doubleArrayOf(1.0, 0.0)
        net.update()

        val initialWeights = competitive.competitive.neuronList.flatMap { n -> n.fanIn.map { it.strength } }

        repeat(10) {
            net.update()
        }

        val decayedWeights = competitive.competitive.neuronList.flatMap { n -> n.fanIn.map { it.strength } }

        assertTrue(decayedWeights.sum() < initialWeights.sum())
    }

    @Test
    fun `test activation dynamics with decay`() {
        // Without activation dynamics, winner gets fixed winValue
        val netNormal = Network()
        val compNormal = CompetitiveNetwork(2, 2)
        netNormal.addNetworkModelsAsync(compNormal)
        compNormal.useActivationDynamics = false
        compNormal.learningRate = 0.0
        compNormal.weights.synapses.forEach { it.strength = 0.25 }

        // With activation dynamics, winner gets decay * activation + weightedInputs
        val netAD = Network()
        val compAD = CompetitiveNetwork(2, 2)
        netAD.addNetworkModelsAsync(compAD)
        compAD.useActivationDynamics = true
        compAD.activationDecay = 0.7
        compAD.addNoise = false
        compAD.learningRate = 0.0
        compAD.weights.synapses.forEach { it.strength = 0.25 }

        compNormal.inputLayer.activationArray = doubleArrayOf(1.0, 0.0)
        compAD.inputLayer.activationArray = doubleArrayOf(1.0, 0.0)

        netNormal.update()
        netAD.update()

        val normalWinnerActivation = compNormal.competitive.activationArray.max()
        val dynamicsWinnerActivation = compAD.competitive.activationArray.max()

        // Normal mode sets winner to winValue (1.0)
        assertEquals(compNormal.winValue, normalWinnerActivation)
        // Activation dynamics mode should produce a different value than the fixed winValue
        assertNotEquals(compAD.winValue, dynamicsWinnerActivation,
            "Activation dynamics should compute activation from decay formula, not use fixed winValue")
        assertTrue(dynamicsWinnerActivation > 0.0,
            "Winner with activation dynamics should have positive activation")
    }

    @Test
    fun `test noise injection`() {
        val netN = Network()
        val compN = CompetitiveNetwork(2, 2)
        netN.addNetworkModelsAsync(compN)

        compN.useActivationDynamics = true
        compN.addNoise = true
        compN.activationDecay = 0.3  // Low decay so activation does not saturate at 1.0
        compN.learningRate = 0.0     // Disable learning to keep weights stable
        // Set small weights so weighted input is small and noise is significant
        compN.weights.synapses.forEach { it.strength = 0.05 }

        compN.inputLayer.activationArray = doubleArrayOf(1.0, 0.0)

        val activations = mutableListOf<Double>()
        repeat(20) {
            netN.update()
            val maxActivation = compN.competitive.activationArray.max()
            activations.add(maxActivation)
        }

        val uniqueActivations = activations.distinct()
        assertTrue(uniqueActivations.size > 1, "Noise should produce varying winner activations across iterations")
    }

    @Test
    fun `test leaky learning prevents dead neurons`() {
        val net3 = Network()
        val competitive3 = CompetitiveNetwork(2, 3)
        net3.addNetworkModelsAsync(competitive3)

        competitive3.useLeakyLearning = true
        competitive3.leakyLearningRate = 0.025

        competitive3.inputLayer.activationArray = doubleArrayOf(1.0, 0.0)
        repeat(5) { net3.update() }

        competitive3.inputLayer.activationArray = doubleArrayOf(0.0, 1.0)
        repeat(5) { net3.update() }

        val allNeuronsHaveNonZeroWeights = competitive3.competitive.neuronList.all { neuron ->
            neuron.fanIn.any { it.strength > 0.1 }
        }

        assertTrue(allNeuronsHaveNonZeroWeights)
    }

    @Test
    fun `test Rummelhart-Zipser vs Alvarez-Squire produce different results`() {
        val netRZ = Network()
        val competitiveRZ = CompetitiveNetwork(2, 2)
        netRZ.addNetworkModelsAsync(competitiveRZ)

        val netAS = Network()
        val competitiveAS = CompetitiveNetwork(2, 2)
        netAS.addNetworkModelsAsync(competitiveAS)

        competitiveRZ.updateMethod = CompetitiveNetwork.UpdateMethod.RUMM_ZIPSER
        competitiveAS.updateMethod = CompetitiveNetwork.UpdateMethod.ALVAREZ_SQUIRE
        competitiveAS.synapseDecayPercent = 0.01

        competitiveRZ.inputLayer.activationArray = doubleArrayOf(1.0, 0.0)
        competitiveAS.inputLayer.activationArray = doubleArrayOf(1.0, 0.0)

        repeat(10) {
            netRZ.update()
            netAS.update()
        }

        val weightsRZ = competitiveRZ.competitive.neuronList.flatMap { n -> n.fanIn.map { it.strength } }
        val weightsAS = competitiveAS.competitive.neuronList.flatMap { n -> n.fanIn.map { it.strength } }

        assertNotEquals(weightsRZ.sum(), weightsAS.sum(), 0.01)
    }
}
