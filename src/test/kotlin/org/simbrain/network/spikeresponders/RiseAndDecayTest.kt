package org.simbrain.network.spikeresponders

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.updaterules.SpikingThresholdRule

class RiseAndDecayTest {

    val net = Network()
    val n1 = Neuron().also { it.clamped = true } // Input
    val n2 = Neuron(SpikingThresholdRule()) // Spiking neuron
    val n3 = Neuron().also { it.upperBound = 10.0 } // receive spike response
    val s1 = Synapse(n1, n2)
    val s2 = Synapse(n2, n3) // This one has the spike responder

    init {
        net.addNetworkModelsAsync(n1, n2, n3, s1, s2)
    }

    @Test
    fun `rise and decay basic behavior`() {
        val rad = RiseAndDecay()
        s2.spikeResponder = rad
        s2.strength = 1.0

        n1.activation = 1.0
        // Must propagate, spike, then initiate behavior
        net.update()
        net.update()
        net.update()
        
        val firstResponse = n3.activation
        assert(firstResponse > 0.0)
        
        // Should continue to rise and then decay
        repeat(5) {
            net.update()
        }
        
        // Response should have changed from initial
        assert(n3.activation != firstResponse)
    }

    @Test
    fun `rise and decay with negative weight`() {
        val rad = RiseAndDecay()
        n3.lowerBound = -10.0
        s2.spikeResponder = rad
        s2.strength = -0.5
        rad.timeConstant = 2.0
        
        n1.activation = 1.0
        net.update()
        net.update()
        net.update()

        assert(n3.activation < 0.0)
        
        repeat(5) {
            net.update()
        }
        
        // Should decay towards zero
        assert(n3.activation > -0.5)
    }

    @Test
    fun `rise and decay with fast time constant`() {
        val rad = RiseAndDecay()
        s2.spikeResponder = rad
        s2.strength = 1.0
        rad.timeConstant = 0.1 // Fast changes
        
        n1.activation = 1.0
        net.update()
        net.update()
        
        val initialResponse = n3.activation
        
        // Should change quickly
        repeat(3) {
            net.update()
        }
        
        assert(n3.activation != initialResponse)
    }

    @Test
    fun `rise and decay with slow time constant`() {
        val rad = RiseAndDecay()
        s2.spikeResponder = rad
        s2.strength = 1.0
        rad.timeConstant = 10.0 // Slow changes
        
        n1.activation = 1.0
        net.update()
        net.update()
        
        val initialResponse = n3.activation
        
        // Should change slowly
        repeat(2) {
            net.update()
        }
        
        // Change should be minimal with slow time constant
        val smallChange = kotlin.math.abs(n3.activation - initialResponse)
        assert(smallChange < 0.1)
    }

    @Test
    fun `rise and decay recovery mechanism`() {
        val rad = RiseAndDecay()
        s2.spikeResponder = rad
        s2.strength = 1.0
        rad.timeConstant = 1.0
        
        // First spike
        n1.activation = 1.0
        net.update()
        net.update()
        
        val firstPeak = n3.activation
        
        // Let recovery decay
        repeat(5) {
            net.update()
        }
        
        // Second spike - recovery should affect response
        n1.activation = 1.0
        net.update()
        net.update()
        
        // Response pattern should be different due to recovery state
        assert(n3.activation != firstPeak)
    }

    @Test
    fun `rise and decay multiple spikes`() {
        val rad = RiseAndDecay()
        s2.spikeResponder = rad
        s2.strength = 0.8
        rad.timeConstant = 2.0
        
        // First spike
        n1.activation = 1.0
        net.update()
        net.update()
        
        // Second spike immediately
        n1.activation = 1.0
        net.update()
        net.update()
        
        // Third spike after a delay
        repeat(3) { net.update() }
        n1.activation = 1.0
        net.update()
        net.update()
        
        // Should handle multiple spikes
        assert(n3.activation != 0.0)
    }

    @Test
    fun `rise and decay copy preserves properties`() {
        val original = RiseAndDecay()
        original.timeConstant = 5.0
        original.spikeProbability = 0.8
        
        val copy = original.copy()
        assertEquals(original.timeConstant, copy.timeConstant)
        assertEquals(original.spikeProbability, copy.spikeProbability)
    }

    @Test
    fun `rise and decay description and name`() {
        val rad = RiseAndDecay()
        assertEquals("Rise and Decay", rad.description)
        assertEquals("Rise and Decay", rad.name)
    }

    @Test
    fun `rise and decay with probability always fires`() {
        val rad = RiseAndDecay()
        rad.spikeProbability = 1.0
        s2.spikeResponder = rad
        s2.strength = 0.5
        
        n1.activation = 1.0
        net.update()
        net.update()
        net.update()
        
        assert(n3.activation != 0.0)
    }

    @Test
    fun `rise and decay with probability never fires`() {
        val rad = RiseAndDecay()
        rad.spikeProbability = 0.0
        s2.spikeResponder = rad
        s2.strength = 0.5
        
        n1.activation = 1.0
        net.update()
        net.update()
        net.update()
        
        assertEquals(0.0, n3.activation)
    }

    @Test
    fun `rise and decay creates proper data holder`() {
        val rad = RiseAndDecay()
        val dataHolder = rad.createResponderData()
        
        assert(dataHolder is RiseAndDecayData)
        
        val radData = dataHolder as RiseAndDecayData
        assertEquals(0.0, radData.recovery)
    }

    @Test
    fun `rise and decay data holder copy works`() {
        val rad = RiseAndDecay()
        val dataHolder = rad.createResponderData() as RiseAndDecayData
        dataHolder.recovery = 0.5
        
        val copy = dataHolder.copy()
        assertEquals(0.5, copy.recovery)
        
        dataHolder.recovery = 1.0
        assertEquals(0.5, copy.recovery) // Copy should be independent
    }

    @Test
    fun `rise and decay data holder clear works`() {
        val rad = RiseAndDecay()
        val dataHolder = rad.createResponderData() as RiseAndDecayData
        dataHolder.recovery = 0.7
        
        dataHolder.clear()
        assertEquals(0.0, dataHolder.recovery)
    }

    @Test
    fun `rise and decay with zero strength`() {
        val rad = RiseAndDecay()
        s2.spikeResponder = rad
        s2.strength = 0.0
        rad.timeConstant = 1.0
        
        n1.activation = 1.0
        net.update()
        net.update()
        
        assertEquals(0.0, n3.activation)
        
        repeat(5) {
            net.update()
        }
        
        assertEquals(0.0, n3.activation)
    }
} 