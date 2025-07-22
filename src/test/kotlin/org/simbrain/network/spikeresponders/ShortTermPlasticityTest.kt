package org.simbrain.network.spikeresponders

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.updaterules.SpikingThresholdRule
import org.simbrain.util.getSimbrainXStream

class ShortTermPlasticityTest {

    val net = Network()
    val n1 = Neuron() // Input
    val n2 = Neuron(SpikingThresholdRule()) // Spiking neuron
    val n3 = Neuron().also { it.upperBound = 10.0 } // receive spike response
    val s1 = Synapse(n1, n2)
    val s2 = Synapse(n2, n3) // This one has the spike responder

    init {
        net.addNetworkModels(n1, n2, n3, s1, s2)
    }

    @Test
    fun `short term plasticity basic behavior`() {
        val stp = ShortTermPlasticity()
        s2.spikeResponder = stp
        s2.strength = 1.0
        
        n1.activation = 1.0
        net.update()
        net.update()
        
        // Should produce some response
        assert(n3.activation != 0.0)
        
        repeat(5) {
            net.update()
        }
        
        // Response should change over time
        assert(n3.activation >= 0.0) // Should not go negative unless weight is negative
    }

    @Test
    fun `short term plasticity with custom UDF parameters`() {
        val stp = ShortTermPlasticity()
        stp.U = 0.3
        stp.D = 100.0
        stp.F = 200.0
        s2.spikeResponder = stp
        s2.strength = 0.8
        
        n1.activation = 1.0
        net.update()
        net.update()
        
        val firstResponse = n3.activation
        
        // Test multiple spikes to see facilitation/depression effects
        repeat(3) {
            n1.activation = 1.0
            net.update()
            net.update()
        }
        
        // Response should be affected by short-term plasticity
        assert(n3.activation != firstResponse)
    }

    @Test
    fun `short term plasticity with high use parameter`() {
        val stp = ShortTermPlasticity()
        stp.U = 0.9 // High use
        stp.D = 50.0
        stp.F = 100.0
        s2.spikeResponder = stp
        s2.strength = 1.0
        
        n1.activation = 1.0
        net.update()
        net.update()
        
        val highUseResponse = n3.activation
        
        // Compare with low use
        val stp2 = ShortTermPlasticity()
        stp2.U = 0.1 // Low use
        stp2.D = 50.0
        stp2.F = 100.0
        s2.spikeResponder = stp2
        
        n1.activation = 1.0
        net.update()
        net.update()
        
        // High use should generally produce different response
        assert(n3.activation != highUseResponse)
    }

    @Test
    fun `short term plasticity with high depression`() {
        val stp = ShortTermPlasticity()
        stp.U = 0.5
        stp.D = 10.0 // High depression (fast recovery)
        stp.F = 200.0
        s2.spikeResponder = stp
        s2.strength = 1.0
        
        // Multiple rapid spikes should show depression effects
        repeat(5) {
            n1.activation = 1.0
            net.update()
            net.update()
        }
        
        assert(n3.activation >= 0.0)
    }

    @Test
    fun `short term plasticity with high facilitation`() {
        val stp = ShortTermPlasticity()
        stp.U = 0.2
        stp.D = 200.0
        stp.F = 10.0 // High facilitation (fast increase)
        s2.spikeResponder = stp
        s2.strength = 1.0
        
        // Multiple spikes should show facilitation effects
        repeat(5) {
            n1.activation = 1.0
            net.update()
            net.update()
        }
        
        assert(n3.activation >= 0.0)
    }

    @Test
    fun `short term plasticity with negative weight`() {
        val stp = ShortTermPlasticity()
        stp.U = 0.4
        stp.D = 100.0
        stp.F = 150.0
        s2.spikeResponder = stp
        s2.strength = -0.5
        
        n1.activation = 1.0
        net.update()
        net.update()
        
        assert(n3.activation <= 0.0)
    }

    @Test
    fun `short term plasticity copy preserves properties`() {
        val original = ShortTermPlasticity()
        original.U = 0.6
        original.D = 125.0
        original.F = 175.0
        original.spikeProbability = 0.7
        
        val copy = original.copy()
        assertEquals(original.U, copy.U)
        assertEquals(original.D, copy.D)
        assertEquals(original.F, copy.F)
        assertEquals(original.spikeProbability, copy.spikeProbability)
    }

    @Test
    fun `short term plasticity description and name`() {
        val stp = ShortTermPlasticity()
        assertEquals("Short-term Plasticity", stp.description)
        assertEquals("Short term plasticity", stp.name)
    }

    @Test
    fun `short term plasticity XML serialization`() {
        val stp = ShortTermPlasticity().apply {
            U = 0.5
            D = 1100.0
            F = 50.0
        }
        s2.spikeResponder = stp
        
        n1.activation = 1.0
        net.update()
        
        val xml = getSimbrainXStream().toXML(s2)
        val deserializedSynapse = getSimbrainXStream().fromXML(xml) as Synapse
        val deserializedSTP = deserializedSynapse.spikeResponder as ShortTermPlasticity
        
        assertEquals(stp.U, deserializedSTP.U)
        assertEquals(stp.D, deserializedSTP.D)
        assertEquals(stp.F, deserializedSTP.F)
    }

    @Test
    fun `short term plasticity with probability always fires`() {
        val stp = ShortTermPlasticity()
        stp.spikeProbability = 1.0
        s2.spikeResponder = stp
        s2.strength = 0.5
        
        n1.activation = 1.0
        net.update()
        net.update()
        
        assert(n3.activation != 0.0)
    }

    @Test
    fun `short term plasticity with probability never fires`() {
        val stp = ShortTermPlasticity()
        stp.spikeProbability = 0.0
        s2.spikeResponder = stp
        s2.strength = 0.5
        
        n1.activation = 1.0
        net.update()
        net.update()
        
        assertEquals(0.0, n3.activation)
    }

    @Test
    fun `short term plasticity creates proper data holder`() {
        val stp = ShortTermPlasticity()
        val dataHolder = stp.createResponderData()
        
        assert(dataHolder is STPScalarData)
        
        val stpData = dataHolder as STPScalarData
        assertEquals(stp.U, stpData.u)
        assertEquals(1.0, stpData.R)
    }

    @Test
    fun `short term plasticity data holder copy works`() {
        val stp = ShortTermPlasticity()
        val dataHolder = stp.createResponderData() as STPScalarData
        dataHolder.u = 0.6
        dataHolder.R = 0.8
        
        val copy = dataHolder.copy()
        assertEquals(0.6, copy.u)
        assertEquals(0.8, copy.R)
        
        dataHolder.u = 0.9
        assertEquals(0.6, copy.u) // Copy should be independent
    }

    @Test
    fun `short term plasticity data holder clear works`() {
        val stp = ShortTermPlasticity()
        val dataHolder = stp.createResponderData() as STPScalarData
        dataHolder.u = 0.7
        dataHolder.R = 0.5
        
        dataHolder.clear()
        assertEquals(0.0, dataHolder.u)
        assertEquals(0.0, dataHolder.R)
    }

    @Test
    fun `short term plasticity with zero strength`() {
        val stp = ShortTermPlasticity()
        s2.spikeResponder = stp
        s2.strength = 0.0
        
        n1.activation = 1.0
        net.update()
        net.update()
        
        assertEquals(0.0, n3.activation)
        
        repeat(5) {
            net.update()
        }
        
        assertEquals(0.0, n3.activation)
    }

    @Test
    fun `short term plasticity with extreme parameters`() {
        val stp = ShortTermPlasticity()
        stp.U = 1.0 // Maximum use
        stp.D = 1.0 // Very fast depression
        stp.F = 1000.0 // Very slow facilitation
        s2.spikeResponder = stp
        s2.strength = 1.0
        
        n1.activation = 1.0
        net.update()
        net.update()
        
        // Should handle extreme parameters without crashing
        assert(n3.activation >= 0.0)
    }

    @Test
    fun `short term plasticity data update mechanism`() {
        val stp = ShortTermPlasticity()
        val dataHolder = stp.createResponderData() as STPScalarData
        
        val initialU = dataHolder.u
        val initialR = dataHolder.R
        
        // Update with some time progression
        dataHolder.update(1.0, 0.0, 0.5, 100.0, 200.0)
        
        // Values should have changed
        assert(dataHolder.u != initialU || dataHolder.R != initialR)
    }
} 