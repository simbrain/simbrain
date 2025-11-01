package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.connectAllToAll
import org.simbrain.network.core.totalFanInStrength
import org.simbrain.network.neurongroups.CompetitiveGroup
import org.simbrain.network.neurongroups.NeuronGroup

class CompetitiveGroupTest {

    var net = Network()
    val competitive = CompetitiveGroup(2)
    lateinit var weights: List<Synapse>
    val inputs = NeuronGroup(2).apply {
        isClamped = true
    }

    init {
        with(net) {
            net.addNetworkModelsAsync(inputs, competitive)
            weights = connectAllToAll(inputs, competitive, 0.1)
        }
    }

    @Test
    fun `Test copy function`() {
        competitive.params.learningRate = .8
        val competitive2 = competitive.copy()
        net.addNetworkModelsAsync(competitive2)
        assertEquals(2, competitive2.neuronList.size)
        assertEquals(competitive.params.updateMethod, competitive2.params.updateMethod)
        assertEquals(competitive.params.learningRate, competitive2.params.learningRate)
        assertEquals(competitive.params.winValue, competitive2.params.winValue)
        assertEquals(competitive.params.loseValue, competitive2.params.loseValue)
        assertEquals(competitive.params.normalizeInputs, competitive2.params.normalizeInputs)
        assertEquals(competitive.params.useLeakyLearning, competitive2.params.useLeakyLearning)
        assertEquals(competitive.params.leakyLearningRate, competitive2.params.leakyLearningRate)
        assertEquals(competitive.params.synapseDecayPercent, competitive2.params.synapseDecayPercent)
        assertEquals(competitive.params.useActivationDynamics, competitive2.params.useActivationDynamics)
        assertEquals(competitive.params.activationDecay, competitive2.params.activationDecay)
        assertEquals(competitive.params.addNoise, competitive2.params.addNoise)
    }

    @Test
    fun `one node wins and takes win value`() {
        competitive.params.winValue = 2.0
        competitive.params.loseValue = 0.0
        inputs.activationArray = doubleArrayOf(1.0, 0.0)
        repeat(2) {
            net.update()
        }
        assertEquals(2.0, competitive.activations.sum())
    }

    @Test
    fun `network learns two patterns`() {
        // Pattern 1
        inputs.activationArray = doubleArrayOf(1.0, 0.0)
        repeat(2) {
            net.update()
        }
        // Pattern 2
        inputs.activationArray = doubleArrayOf(0.0, 1.0)
        repeat(2) {
            net.update()
        }

        // Test retrieval
        inputs.activationArray = doubleArrayOf(1.0, 0.0)
        net.update()
        val winner1 = competitive.neuronList[competitive.activationArray.indexOfFirst { it == 1.0 }]
        inputs.activationArray = doubleArrayOf(0.0, 0.1)
        net.update()
        val winner2 = competitive.neuronList[competitive.activationArray.indexOfFirst { it == 1.0 }]
        assertNotEquals(winner1, winner2)
    }

    @Test
    fun `test normalize weights`() {
        competitive.normalizeIncomingWeights()
        assertEquals(1.0,  competitive.getNeuron(0).fanIn.sumOf { it.strength })
        assertEquals(1.0,  competitive.getNeuron(1).fanIn.sumOf { it.strength })
    }

    @Test
    fun `test leaky learning`() {
        competitive.params.useLeakyLearning = true
        inputs.activationArray = doubleArrayOf(1.0, 0.0)
        net.update()
        val loser = competitive.neuronList[competitive.activationArray.indexOfFirst { it == 0.0 }]
        val before = loser.totalFanInStrength()
        net.update()
        val after = loser.totalFanInStrength()
        assertTrue(after > before)
    }
    
    @Test
    fun `test normalization enabled vs disabled`() {
        val net2 = Network()
        val competitive2 = CompetitiveGroup(2)
        val inputs2 = NeuronGroup(2).apply { isClamped = true }
        with(net2) {
            net2.addNetworkModelsAsync(inputs2, competitive2)
            connectAllToAll(inputs2, competitive2, 0.1)
        }
        
        competitive.params.normalizeInputs = true
        competitive2.params.normalizeInputs = false
        
        inputs.activationArray = doubleArrayOf(0.5, 0.5)
        inputs2.activationArray = doubleArrayOf(1.0, 1.0)
        
        repeat(3) {
            net.update()
            net2.update()
        }
        
        val winner1 = competitive.neuronList[competitive.activationArray.indexOfFirst { it == 1.0 }]
        val winner2 = competitive2.neuronList[competitive2.activationArray.indexOfFirst { it == 1.0 }]
        
        assertNotNull(winner1)
        assertNotNull(winner2)
    }
    
    @Test
    fun `test Alvarez-Squire decay`() {
        competitive.params.updateMethod = CompetitiveGroup.UpdateMethod.ALVAREZ_SQUIRE
        competitive.params.synapseDecayPercent = 0.01
        
        inputs.activationArray = doubleArrayOf(1.0, 0.0)
        net.update()
        
        val initialWeights = competitive.neuronList.flatMap { n -> n.fanIn.map { it.strength } }
        
        repeat(10) {
            net.update()
        }
        
        val decayedWeights = competitive.neuronList.flatMap { n -> n.fanIn.map { it.strength } }
        
        assertTrue(decayedWeights.sum() < initialWeights.sum())
    }
    
    @Test
    fun `test activation dynamics with decay`() {
        competitive.params.useActivationDynamics = true
        competitive.params.activationDecay = 0.7
        competitive.params.addNoise = false
        
        inputs.activationArray = doubleArrayOf(1.0, 0.0)
        net.update()
        
        val winner = competitive.neuronList[competitive.activationArray.indexOfFirst { it > 0.0 }]
        val firstActivation = winner.activation
        
        net.update()
        val secondActivation = winner.activation
        
        assertTrue(secondActivation != 1.0)
        assertTrue(secondActivation > 0.0)
    }
    
    @Test
    fun `test noise injection`() {
        competitive.params.useActivationDynamics = true
        competitive.params.addNoise = true
        competitive.params.activationDecay = 0.7
        
        inputs.activationArray = doubleArrayOf(1.0, 0.0)
        
        val activations = mutableListOf<Double>()
        repeat(10) {
            net.update()
            val winner = competitive.neuronList[competitive.activationArray.indexOfFirst { it > 0.0 }]
            activations.add(winner.activation)
        }
        
        val uniqueActivations = activations.distinct()
        assertTrue(uniqueActivations.size > 1)
    }
    
    @Test
    fun `test leaky learning prevents dead neurons`() {
        val net3 = Network()
        val competitive3 = CompetitiveGroup(3)
        val inputs3 = NeuronGroup(2).apply { isClamped = true }
        with(net3) {
            net3.addNetworkModelsAsync(inputs3, competitive3)
            connectAllToAll(inputs3, competitive3, 0.1)
        }
        
        competitive3.params.useLeakyLearning = true
        competitive3.params.leakyLearningRate = 0.025
        
        inputs3.activationArray = doubleArrayOf(1.0, 0.0)
        repeat(5) { net3.update() }
        
        inputs3.activationArray = doubleArrayOf(0.0, 1.0)
        repeat(5) { net3.update() }
        
        val allNeuronsHaveNonZeroWeights = competitive3.neuronList.all { neuron ->
            neuron.fanIn.any { it.strength > 0.1 }
        }
        
        assertTrue(allNeuronsHaveNonZeroWeights)
    }
    
    @Test
    fun `test Rummelhart-Zipser vs Alvarez-Squire produce different results`() {
        val netRZ = Network()
        val competitiveRZ = CompetitiveGroup(2)
        val inputsRZ = NeuronGroup(2).apply { isClamped = true }
        with(netRZ) {
            netRZ.addNetworkModelsAsync(inputsRZ, competitiveRZ)
            connectAllToAll(inputsRZ, competitiveRZ, 0.1)
        }
        
        val netAS = Network()
        val competitiveAS = CompetitiveGroup(2)
        val inputsAS = NeuronGroup(2).apply { isClamped = true }
        with(netAS) {
            netAS.addNetworkModelsAsync(inputsAS, competitiveAS)
            connectAllToAll(inputsAS, competitiveAS, 0.1)
        }
        
        competitiveRZ.params.updateMethod = CompetitiveGroup.UpdateMethod.RUMM_ZIPSER
        competitiveAS.params.updateMethod = CompetitiveGroup.UpdateMethod.ALVAREZ_SQUIRE
        competitiveAS.params.synapseDecayPercent = 0.01
        
        inputsRZ.activationArray = doubleArrayOf(1.0, 0.0)
        inputsAS.activationArray = doubleArrayOf(1.0, 0.0)
        
        repeat(10) {
            netRZ.update()
            netAS.update()
        }
        
        val weightsRZ = competitiveRZ.neuronList.flatMap { n -> n.fanIn.map { it.strength } }
        val weightsAS = competitiveAS.neuronList.flatMap { n -> n.fanIn.map { it.strength } }
        
        assertNotEquals(weightsRZ.sum(), weightsAS.sum(), 0.01)
    }
}
