package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron

class IntegrateAndFireTest {

    val net = Network()
    val intFire = IntegrateAndFireRule()
    val n = Neuron(intFire)
    init {
        net.addNetworkModelAsync(n)
    }

    // TODO: Test threshold, time constant, resistance

    @Test
    fun `stays at resting potential when resistance is 0`() {
        intFire.resistance = 0.0
        // Start it off at resting potential
        n.activation = intFire.restingPotential
        repeat(10) {
            net.update()
            // println("t = ${net.time}: act=${n.activation}")
            assertEquals(intFire.restingPotential, n.activation)
        }
    }

    @Test
    fun `large current triggers one spike followed by no spike because of refractory period`() {
        intFire.backgroundCurrent = 1000.0
        net.update()
        assertEquals(true, n.isSpike)
        net.update()
        assertEquals(false, n.isSpike)
    }

    @Test
    fun `goes to reset potential after a spike`() {
        intFire.backgroundCurrent = 1000.0
        net.update()
        assertEquals(intFire.resetPotential, n.activation)
    }

    @Test
    fun `decays to resting potential`() {
        intFire.backgroundCurrent = 0.0
        intFire.timeConstant = 1.0 // Small time constant so it decays quickly
        repeat(100) {
            println("t = ${net.time}time: act=${n.activation}")
            net.update()
        }
        assertEquals(intFire.restingPotential, n.activation, .001)
    }

    @Test
    fun `neuron only spikes between refractory periods`() {
        intFire.refractoryPeriod = 5.0
        intFire.backgroundCurrent = 1000.0
        net.timeStep = 1.0
        repeat(100) {
            net.update()
            // println("t = $it: act=${n.activation}")
            if (it % (intFire.refractoryPeriod + 1) == 0.0) {
                assertEquals(true, n.isSpike)
            } else {
                assertEquals(false, n.isSpike)
            }
        }
    }

}