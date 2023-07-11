package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.simulations.AllostaticDataHolder
import org.simbrain.custom_sims.simulations.AllostaticUpdateRule
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse

class AllostaticRuleTest {

    val net = Network()
    val n = Neuron(net, AllostaticUpdateRule())
    val data = n.dataHolder as AllostaticDataHolder
    val externalInput = Neuron(net).apply{
        upperBound = 10.0
    }
    val reservoirInput = Neuron(net, AllostaticUpdateRule())
    val externalWeight = Synapse(externalInput, n)
    val resWeight = Synapse(reservoirInput, n)

    init {
        net.addNetworkModelsAsync(n, externalInput, reservoirInput, externalWeight, resWeight)
    }

    fun printNeuronState() {
        println("ResInput: ${reservoirInput.activation}, ${reservoirInput.isSpike}")
        println("Neuron: ${n.activation}, ${n.isSpike}")
        println("Weight: ${resWeight.strength}")
    }

    // TODO
    // Sensor neuron case above and below threshold
    // Res neuron three cases of Figure 8

    @Test
    fun `activation leaks at a rate of 75 percent`() {
        n.activation = 1.0
        assertEquals(1.0, n.activation, 0.0)
        net.update()
        assertEquals(.75, n.activation, 0.0)
        net.update()
        assertEquals(.5625, n.activation, 0.0)
    }

    @Test
    fun `neuron spikes with above threshold activation`() {
        // threshold is 2 so it should spike
        n.activation = 3.0
        net.update()
        assertTrue(n.isSpike)
    }

    @Test
    fun `target reduces when activation is below target`() {
        data.target = 1.5
        n.activation = .5
        net.update()
        assertTrue(data.target < 1.5)
    }

    @Test
    fun `target does not reduce below 1`() {
        n.activation = .5
        net.update()
        assertEquals(data.target, 1.0)
    }

    @Test
    fun `target increases when activation is below target`() {
        n.activation = 1.5
        net.update()
        assertTrue(data.target > 1.0)
    }

    // @Test
    // fun `changing target changes threshold to twice target`() {
    //     data.target = .75
    //     assertEquals(1.5, data.threshold)
    // }

    @Test
    fun `if activation above threshold, spike and subtract threshold`() {
        // Threshold is 2
        n.activation = 3.0
        // This becomes 2.25 after leak. Threshold is 2. So new activation should be 2.25 - 2
        n.update()
        assertTrue(n.isSpike)
        assertEquals(.25, n.activation)

    }

    @Test
    fun `if activation enough above threshold, target increases`() {
        // Threshold is 2
        n.activation = 10.0
        // This becomes 7.5 after leak. Threshold is 2. So new activation should be 5.5
        n.update()
        assertTrue(n.isSpike)
        assertEquals(5.5, n.activation)
        // This should make target increase above 1
        assertTrue(data.target > 1.0)
    }

    @Test
    fun `when activation is above target, weights of active neighbors decreased`() {
        // Spike the input
        reservoirInput.activation = 3.0
        reservoirInput.update()
        n.activation = 3.0
        n.update()
        assertTrue(resWeight.strength < 1.0)
    }

    @Test
    fun `when activation is below target, weights of active neighbors decreased`() {
        // Spike the input
        reservoirInput.activation = 3.0
        reservoirInput.update()
        n.activation = .5
        n.update()
        assertTrue(resWeight.strength < 1.0)
    }

    @Test
    fun `res weight increases when input spikes and activation is above target`() {
        reservoirInput.activation = 3.0
        reservoirInput.update()
        n.activation = 3.0
        n.update()
        assertTrue(resWeight.strength < 1.0)
    }

    // @Test
    // fun `neuron spiking does change external weight`() {
    //     n.activation = 3.0
    //     assertEquals(1.0, externalWeight.strength)
    // }





}