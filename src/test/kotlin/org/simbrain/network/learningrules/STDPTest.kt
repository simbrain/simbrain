package org.simbrain.network.learningrules

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.toColumnVector

class STDPTest {

    fun createSpikingNeuron(label: String): Neuron {
        return Neuron().apply {
            updateRule = IntegrateAndFireRule()
            this.label = label
        }
    }

    @Test
    fun `test STDP strengthens synapse when pre fires before post`() {
        val network = Network()
        with(network) {
            val pre = createSpikingNeuron("Pre").apply {
                (dataHolder as SpikingScalarData).lastSpikeTime = 1.0
            }
            val post = createSpikingNeuron("Post").apply {
                isSpike = true
                (dataHolder as SpikingScalarData).lastSpikeTime = 2.0
            }
            val synapse = Synapse(pre, post).apply {
                strength = 0.5
                learningRule = STDPRule().apply {
                    learningRate = 0.01
                    wPlus = 1.0
                    wMinus = 1.0
                    tauPlus = 10.0
                    tauMinus = 10.0
                }
            }
            network.addNetworkModels(pre, post, synapse)
            (synapse.learningRule as STDPRule).apply(synapse, EmptyScalarData)
            assert(synapse.strength > 0.5)
        }

    }

    @Test
    fun `test STDP weakens synapse when post fires before pre`() {
        val network = Network()
        with(network) {

            val pre = createSpikingNeuron("Pre").apply {
                (dataHolder as SpikingScalarData).lastSpikeTime = 2.0
            }
            val post = createSpikingNeuron("Post").apply {
                isSpike = true
                (dataHolder as SpikingScalarData).lastSpikeTime = 1.0
            }
            val synapse = Synapse(pre, post).apply {
                strength = 0.5
                learningRule = STDPRule().apply {
                    learningRate = 0.01
                    wPlus = 1.0
                    wMinus = 1.0
                    tauPlus = 10.0
                    tauMinus = 10.0
                }
            }
            network.addNetworkModels(pre, post, synapse)
            (synapse.learningRule as STDPRule).apply(synapse, EmptyScalarData)

            assert(synapse.strength < 0.5)
        }
    }

}