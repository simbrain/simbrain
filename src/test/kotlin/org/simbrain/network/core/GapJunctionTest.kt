/**
 * Model-level coverage for [GapJunction]: symmetric currents pulled during buffered update, inert and
 * disabled behavior, duplicate rejection, endpoint-delete cascade, and XML round-tripping.
 */
package org.simbrain.network.core

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.updaterules.ContinuousSigmoidalData
import org.simbrain.network.updaterules.ContinuousSigmoidalRule

class GapJunctionTest {

    private fun voltageNeuron(initialState: Double = 0.0) = Neuron(
        ContinuousSigmoidalRule().apply {
            timeConstant = 1.0
            leakConstant = 1.0
            slope = 0.25
        }
    ).also { (it.dataHolder as ContinuousSigmoidalData).netActivation = initialState }

    private fun netActivation(neuron: Neuron) = (neuron.dataHolder as ContinuousSigmoidalData).netActivation

    @Test
    fun `currents into the two endpoints are equal and opposite`() {
        val net = Network()
        val a = voltageNeuron(2.0)
        val b = voltageNeuron(-1.0)
        val junction = GapJunction(a, b, 2.5)
        net.addNetworkModelsAsync(a, b, junction)

        assertEquals(2.5 * (-1.0 - 2.0), junction.currentInto(a), 1e-12)
        assertEquals(-junction.currentInto(a), junction.currentInto(b), 0.0)
    }

    @Test
    fun `gap currents integrate into both endpoints in the same buffered step`() {
        val net = Network()
        net.timeStep = 0.1
        val a = voltageNeuron(2.0)
        val b = voltageNeuron(-1.0)
        val junction = GapJunction(a, b, 2.5)
        net.addNetworkModelsAsync(a, b, junction)

        net.update()

        assertEquals(2.0 * 0.9 + 0.1 * 2.5 * (-1.0 - 2.0), netActivation(a), 1e-12)
        assertEquals(-1.0 * 0.9 + 0.1 * 2.5 * (2.0 - (-1.0)), netActivation(b), 1e-12)
    }

    @Test
    fun `a clamped endpoint still exports its frozen state`() {
        val net = Network()
        net.timeStep = 0.1
        val a = voltageNeuron(2.0)
        val b = voltageNeuron(0.0)
        a.clamped = true
        val junction = GapJunction(a, b, 1.0)
        net.addNetworkModelsAsync(a, b, junction)

        net.update()

        assertEquals(2.0, netActivation(a), 0.0, "clamped endpoint state must not change")
        assertEquals(0.1 * (2.0 - 0.0), netActivation(b), 1e-12)
    }

    @Test
    fun `a junction with a non voltage endpoint is inert`() {
        val net = Network()
        val a = voltageNeuron(3.0)
        val b = Neuron()
        val junction = GapJunction(a, b, 2.0)
        net.addNetworkModelsAsync(a, b, junction)

        assertFalse(junction.isActive)
        assertEquals(0.0, junction.currentInto(a), 0.0)
        assertEquals(0.0, junction.currentInto(b), 0.0)
    }

    @Test
    fun `a disabled junction passes no current`() {
        val net = Network()
        val a = voltageNeuron(3.0)
        val b = voltageNeuron(0.0)
        val junction = GapJunction(a, b, 2.0)
        junction.isEnabled = false
        net.addNetworkModelsAsync(a, b, junction)

        assertEquals(0.0, junction.currentInto(a), 0.0)
        assertEquals(0.0, junction.currentInto(b), 0.0)
    }

    @Test
    fun `nudging increments and decrements the conductance without going negative`() {
        val a = voltageNeuron()
        val b = voltageNeuron()
        val junction = GapJunction(a, b, 0.15)

        junction.increment()
        assertEquals(0.25, junction.conductance, 1e-12)

        junction.decrement()
        junction.decrement()
        junction.decrement()
        assertEquals(0.0, junction.conductance, 0.0, "conductance must clamp at zero")
    }

    @Test
    fun `self junctions and duplicate pairs are rejected`() {
        val net = Network()
        val a = voltageNeuron()
        val b = voltageNeuron()
        net.addNetworkModelsAsync(a, b)
        val junction = GapJunction(a, b, 1.0)
        net.addNetworkModelsAsync(junction)

        val selfJunction = GapJunction(a, a)
        val duplicate = GapJunction(a, b)
        val reversedDuplicate = GapJunction(b, a)
        net.addNetworkModelsAsync(selfJunction, duplicate, reversedDuplicate)

        with(net) {
            assertFalse(selfJunction.shouldAdd())
            assertFalse(duplicate.shouldAdd())
            assertFalse(reversedDuplicate.shouldAdd())
        }
        assertEquals(listOf(junction), net.getModels<GapJunction>().toList())
        assertEquals(setOf(junction), a.gapJunctions)
    }

    @Test
    fun `deleting an endpoint neuron cascades to the junction and returns it`() = runBlocking {
        val net = Network()
        val a = voltageNeuron()
        val b = voltageNeuron()
        val junction = GapJunction(a, b, 1.0)
        net.addNetworkModelsAsync(a, b, junction)

        val deleted = a.delete()

        assertTrue(junction in deleted, "the cascade must report the junction for undo")
        assertTrue(net.getModels<GapJunction>().isEmpty())
        assertTrue(b.gapJunctions.isEmpty())
    }

    @Test
    fun `xml round trip preserves endpoints registration and conductance`() {
        val net = Network()
        net.timeStep = 0.1
        val a = voltageNeuron(1.5).apply { label = "A" }
        val b = voltageNeuron(0.0).apply { label = "B" }
        val junction = GapJunction(a, b, 2.62)
        net.addNetworkModelsAsync(a, b, junction)

        val restored = getNetworkXStream().fromXML(getNetworkXStream().toXML(net)) as Network
        val restoredJunction = restored.getModels<GapJunction>().single()
        val restoredNeurons = restored.freeNeurons.associateBy { it.label }

        assertEquals(2.62, restoredJunction.conductance, 0.0)
        assertTrue(restoredJunction.neuron1 === restoredNeurons["A"] && restoredJunction.neuron2 === restoredNeurons["B"])
        assertTrue(restoredJunction in restoredNeurons["A"]!!.gapJunctions)
        assertTrue(restoredJunction in restoredNeurons["B"]!!.gapJunctions)

        restored.update()
        assertEquals(
            1.5 * 0.9 + 0.1 * 2.62 * (0.0 - 1.5),
            netActivation(restoredNeurons["A"]!!),
            1e-12,
            "the restored junction must conduct"
        )
    }
}
