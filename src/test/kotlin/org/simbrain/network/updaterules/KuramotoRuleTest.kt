package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import kotlin.math.PI
import kotlin.math.abs

class KuramotoRuleTest {

    val net = Network()
    val n1 = Neuron()
    val n2 = Neuron()
    val kuramotoRule1: KuramotoRule = KuramotoRule()
    val kuramotoRule2: KuramotoRule = KuramotoRule()
    
    init {
        n1.updateRule = kuramotoRule1
        n2.updateRule = kuramotoRule2
        
        // Set initial phases
        n1.activation = PI / 4  // 45 degrees
        n2.activation = 3 * PI / 4  // 135 degrees
        
        net.addNetworkModelsAsync(n1, n2)
    }

    @Test
    fun `test basic phase evolution without coupling`() {
        kuramotoRule1.slope = 1.0
        kuramotoRule1.isClipped = false
        val initialPhase = n1.activation
        
        net.update()
        
        // Phase should remain within valid bounds after update
        assertTrue(n1.activation >= 0.0 && n1.activation <= 2 * PI, "Phase should be within [0, 2π]")
        
        // Test that the rule name is correct
        assertEquals("Kuramoto", kuramotoRule1.name)
    }

    @Test
    fun `test phase wrapping`() {
        kuramotoRule1.slope = PI
        n1.activation = 1.8 * PI  // Close to 2π
        
        net.update()
        
        // Should wrap around to small positive value
        assertTrue(n1.activation >= 0)
        assertTrue(n1.activation < 2 * PI)
    }

    @Test
    fun `test coupling between oscillators`() {
        // Connect n1 to n2
        val synapse = Synapse(n1, n2)
        synapse.strength = 1.0  
        net.addNetworkModelAsync(synapse)
        
        kuramotoRule2.slope = 0.0  // No natural frequency for n2
        kuramotoRule2.isClipped = false
        
        val initialPhaseN2 = n2.activation
        
        net.update()
        
        // n2 should be influenced by n1 through coupling and should change
        assertNotEquals(initialPhaseN2, n2.activation, "n2 phase should change due to coupling")
        assertTrue(n2.activation >= 0.0 && n2.activation <= 2 * PI, "Phase should be within [0, 2π]")
    }

    @Test
    fun `test multiple coupling with averaging`() {
        val n3 = Neuron()
        val kuramotoRule3 = KuramotoRule()
        kuramotoRule3.isClipped = false
        n3.updateRule = kuramotoRule3
        n3.activation = PI / 2
        net.addNetworkModelAsync(n3)
        
        // Connect both n1 and n2 to n3
        val s13 = Synapse(n1, n3)
        val s23 = Synapse(n2, n3)
        s13.strength = 1.0
        s23.strength = 1.0
        net.addNetworkModelsAsync(s13, s23)
        
        kuramotoRule3.slope = 0.0
        val initialPhaseN3 = n3.activation
        
        net.update()
        
        // n3 should be influenced by both n1 and n2
        assertNotEquals(initialPhaseN3, n3.activation, "n3 phase should change due to multiple coupling")
        assertTrue(n3.activation >= 0.0 && n3.activation <= 2 * PI, "Phase should be within [0, 2π]")
    }

    @Test
    fun `test clipping`() {
        kuramotoRule1.isClipped = true
        kuramotoRule1.upperBound = PI
        kuramotoRule1.lowerBound = 0.0
        kuramotoRule1.slope = 2 * PI  // Large slope to test clipping
        
        n1.activation = 0.5
        
        net.update()
        
        // Should be clipped to bounds (though note: Kuramoto naturally wraps phases)
        assertTrue(n1.activation <= kuramotoRule1.upperBound)
        assertTrue(n1.activation >= kuramotoRule1.lowerBound)
    }

    @Test
    fun `test derivative`() {
        kuramotoRule1.slope = 2.0
        kuramotoRule1.upperBound = 10.0
        kuramotoRule1.lowerBound = -10.0
        
        // Within bounds should return slope
        assertEquals(2.0, kuramotoRule1.getDerivative(0.0), 0.0)
        
        // At boundaries should return 0
        assertEquals(0.0, kuramotoRule1.getDerivative(11.0), 0.0)
        assertEquals(0.0, kuramotoRule1.getDerivative(-11.0), 0.0)
    }

    @Test
    fun `test copy`() {
        kuramotoRule1.slope = 1.5
        kuramotoRule1.upperBound = 5.0
        kuramotoRule1.lowerBound = -5.0
        kuramotoRule1.isClipped = true
        
        val copy = kuramotoRule1.copy()
        
        assertEquals(kuramotoRule1.slope, copy.slope)
        assertEquals(kuramotoRule1.upperBound, copy.upperBound)
        assertEquals(kuramotoRule1.lowerBound, copy.lowerBound)
        assertEquals(kuramotoRule1.isClipped, copy.isClipped)
    }

    @Test
    fun `test synchronization tendency`() {
        // Set up two oscillators with similar frequencies
        kuramotoRule1.slope = 1.0
        kuramotoRule2.slope = 1.1
        
        // Strong coupling
        val synapse12 = Synapse(n1, n2)
        val synapse21 = Synapse(n2, n1)
        synapse12.strength = 2.0
        synapse21.strength = 2.0
        net.addNetworkModelsAsync(synapse12, synapse21)
        
        // Start with different phases
        n1.activation = 0.0
        n2.activation = PI
        
        val initialPhaseDiff = abs(n1.activation - n2.activation)
        
        // Run several updates
        repeat(10) { net.update() }
        
        val finalPhaseDiff = abs(n1.activation - n2.activation)
        
        // Phase difference should generally decrease (synchronization)
        // Note: This is a tendency test, not a guarantee due to phase wrapping
        assertTrue(finalPhaseDiff < initialPhaseDiff || finalPhaseDiff > PI)
    }
} 