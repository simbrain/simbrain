package org.simbrain.network.spikeresponders

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.updaterules.SpikingThresholdRule

class JumpAndDecayTest {

    val net = Network()
    val n1 = Neuron() // Input
    val n2 = Neuron(SpikingThresholdRule()) // Spiking neuron
    // receive spike response
    val n3 = Neuron().also {
        it.upperBound = 10.0
        it.lowerBound = 0.0
    }
    val s1 = Synapse(n1, n2)
    val s2 = Synapse(n2, n3) // This one has the spike responder

    init {
        net.addNetworkModels(n1, n2, n3, s1, s2)
    }

    @Test
    fun `jump and decay basic behavior`() {
        val jd = JumpAndDecay()
        s2.strength = 4.0
        jd.baseLine = 2.0
        jd.timeConstant = 0.15
        s2.spikeResponder = jd
        
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(4.0, n3.activation)
        
        repeat(10) {
            net.update()
        }
        assertEquals(2.0, n3.activation, 0.1)
    }

    @Test
    fun `jump and decay with negative weight`() {
        val jd = JumpAndDecay()
        n3.lowerBound = -10.0
        s2.strength = -0.5
        s2.spikeResponder = jd
        
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(-0.5, n3.activation)
        
        jd.timeConstant = 0.15
        repeat(10) {
            net.update()
        }
        assertEquals(0.0, n3.activation, 0.1)
    }

    @Test
    fun `jump and decay with zero baseline`() {
        val jd = JumpAndDecay()
        s2.strength = 1.0
        jd.baseLine = 0.0
        jd.timeConstant = 0.2
        s2.spikeResponder = jd
        
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(1.0, n3.activation)
        
        repeat(10) {
            net.update()
        }
        assertEquals(0.0, n3.activation, 0.1)
    }

    @Test
    fun `jump and decay with negative baseline`() {
        val jd = JumpAndDecay()
        n3.lowerBound = -10.0
        s2.strength = 2.0
        jd.baseLine = -1.0
        jd.timeConstant = 0.1
        s2.spikeResponder = jd
        
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(2.0, n3.activation)
        
        repeat(15) {
            net.update()
        }
        assertEquals(-1.0, n3.activation, 0.1)
    }

    @Test
    fun `jump and decay with slow time constant`() {
        val jd = JumpAndDecay()
        s2.strength = 1.0
        jd.baseLine = 0.0
        jd.timeConstant = 2.0 // Slow decay
        s2.spikeResponder = jd
        
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(1.0, n3.activation)
        
        // Should still be relatively high after a few updates
        repeat(3) {
            net.update()
        }
        assert(n3.activation > 0.5)
    }

    @Test
    fun `jump and decay with fast time constant`() {
        val jd = JumpAndDecay()
        s2.strength = 1.0
        jd.baseLine = 0.0
        jd.timeConstant = 0.05 // Fast decay
        s2.spikeResponder = jd
        
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(1.0, n3.activation)
        
        // Should decay quickly
        repeat(5) {
            net.update()
        }
        assertEquals(0.0, n3.activation, 0.1)
    }

    @Test
    fun `convolved jump and decay`() {
        val cjd = JumpAndDecay().apply {
            useConvolution = true
        }
        s2.spikeResponder = cjd
        s2.strength = 0.5
        
        n1.activation = 1.0
        n1.clamped = true
        net.update()
        net.update()
        assertEquals(0.5, n3.activation)
        net.update()
        assertEquals(1.0, n3.activation)
        net.update()
        assertEquals(1.5, n3.activation)
        net.update()
        assertEquals(2.0, n3.activation)
        
        n1.clamped = false
        cjd.baseLine = 0.2
        cjd.timeConstant = 0.1 // decay quick
        repeat(10) {
            net.update()
        }
        assertEquals(0.2, n3.activation, 0.1)
    }

    @Test
    fun `jump and decay copy preserves properties`() {
        val original = JumpAndDecay()
        original.baseLine = 1.5
        original.timeConstant = 0.3
        original.useConvolution = true
        original.spikeProbability = 0.7
        
        val copy = original.copy()
        assertEquals(original.baseLine, copy.baseLine)
        assertEquals(original.timeConstant, copy.timeConstant)
        assertEquals(original.useConvolution, copy.useConvolution)
        assertEquals(original.spikeProbability, copy.spikeProbability)
    }

    @Test
    fun `jump and decay description and name`() {
        val jd = JumpAndDecay()
        assertEquals("Jump and Decay", jd.description)
        assertEquals("Jump and Decay", jd.name)
    }

    @Test
    fun `jump and decay with probability always fires`() {
        val jd = JumpAndDecay()
        jd.spikeProbability = 1.0
        s2.spikeResponder = jd
        s2.strength = 0.5
        
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.5, n3.activation)
    }

    @Test
    fun `jump and decay with probability never fires`() {
        val jd = JumpAndDecay()
        jd.spikeProbability = 0.0
        s2.spikeResponder = jd
        s2.strength = 0.5
        
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.0, n3.activation)
    }

    @Test
    fun `jump and decay with multiple spikes`() {
        val jd = JumpAndDecay()
        s2.strength = 0.6
        jd.baseLine = 0.0
        jd.timeConstant = 1.0
        s2.spikeResponder = jd
        
        // First spike
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.6, n3.activation)
        
        // Wait for partial decay
        repeat(2) { net.update() }
        val partialDecay = n3.activation
        assert(partialDecay < 0.6 && partialDecay > 0.0)
        
        // Second spike - should jump back to full strength
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.6, n3.activation)
    }

    @Test
    fun `jump and decay with convolution accumulates spikes`() {
        val jd = JumpAndDecay()
        jd.useConvolution = true
        jd.baseLine = 0.0
        jd.timeConstant = 1.0
        s2.strength = 0.3
        s2.spikeResponder = jd
        
        // First spike
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.3, n3.activation)
        
        // Second spike while first is still decaying
        n1.activation = 1.0
        net.update()
        net.update()
        assert(n3.activation > 0.3) // Should be accumulated
    }
} 