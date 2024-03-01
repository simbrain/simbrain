package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.activity_generators.SinusoidalRule

class SinusoidalRuleTest {

    // Default update rule is sinusoidal
    val net = Network()
    var rule1 = SinusoidalRule()
    var rule2 = SinusoidalRule()
    val n1 = Neuron(rule1)
    val n2 = Neuron(rule2)

    init {
        net.addNetworkModelsAsync(n1, n2)
        n1.activation = 0.0
        rule1.upperBound = 1.0
        rule1.lowerBound = -1.0
        rule1.phase = 1.0
        rule1.frequency = 1.0

        n2.activation = 0.0
        rule2.upperBound = 1.0
        rule2.lowerBound = -1.0
        rule2.phase = 1.0
        rule2.frequency = 1.0
    }

    @Test
    fun `Activation Stays Within the Bounds`() {
        repeat(10) {
            net.update()
            assertTrue(n1.activation < 1.0)
            assertTrue(n1.activation > -1.0)
        }

        net.resetTime()
        rule1.upperBound = 2.0
        repeat(10) {
            net.update()
            assertTrue(n1.activation < 2.0)
        }
    }

    @Test
    fun `Activation of Equal Phases are Equal`() {
        repeat(10) {
            net.update()
            assertEquals(n1.activation, n2.activation)
        }
    }

    @Test
    fun `Testing Time Function to get Activation Approximately 1`() {
        rule1.phase = 0.0
        rule1.frequency = 1.0
        net.timeType = Network.TimeType.CONTINUOUS
        net.timeStep = 1.57 // pi/2
        net.updateTime()
        net.update()
        // sin(pi/2) = 1
        assertEquals(1.0, n1.activation, .01)
    }

    @Test
    fun `Testing Phase Function to get Activation Approximately 1`(){
        net.resetTime()
        rule1.phase = 1.57
        rule1.frequency = 1.0
        net.update()
        assertEquals(1.0, n1.activation, 0.01)

    }

    @Test
    fun `Phase Function at 1_57 Equals TimeStep Function at 1_57`() {
        assertEquals(`Testing Time Function to get Activation Approximately 1`(),`Testing Phase Function to get Activation Approximately 1`() )
    }

    @Test
    fun `Activation Stays within the Bounds Still if Frequency Increases or Decreases`(){
        rule1.frequency = 2.0
        rule1.phase = 0.0
        repeat(10) {
            net.update()
        }
        assertTrue(n1.activation < 1.0)
        assertTrue(n1.activation > -1.0)

        rule2.frequency = -2.0
        rule2.phase = 0.0
        repeat(10) {
            net.update()
        }
        assertTrue(n2.activation < 1.0)
        assertTrue(n2.activation > -1.0)
    }

    @Test
    fun `Activation Should Remain Constant at Frequency 0`() {
        rule1.frequency = 0.0
        rule2.frequency = 0.0
        repeat(10) {
            net.update()
        }
    assertTrue(n1.activation == n2.activation)
    }
}