package org.simbrain.custom_sims.simulations.leabra

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.NewSimulation
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.updaterules.LeabraRule
import org.simbrain.network.updaterules.PointNeuronRule
import org.simbrain.network.updaterules.PointNeuronScalarData

class LeabraBasicSimsTest {

    private suspend fun network(sim: NewSimulation): Network {
        val scope = SimulationScope()
        sim.task.invoke(scope, null)
        return scope.workspace.componentList.filterIsInstance<NetworkComponent>().first().network
    }

    @Test
    fun `point neuron activates only above threshold and is shut off by inhibition and leak`() = runBlocking {
        val network = network(pointNeuronSim)
        val excitatory = network.freeNeurons.first { it.label == "Excitatory" }
        val inhibitory = network.freeNeurons.first { it.label == "Inhibitory" }
        val neuron = network.freeNeurons.first { it.label == "Point neuron" }
        val rule = neuron.updateRule as PointNeuronRule

        suspend fun settle(exc: Double, inh: Double = 0.0, leak: Double = 0.3): Pair<Double, Double> {
            excitatory.activation = exc
            inhibitory.activation = inh
            rule.leakConductance = leak
            repeat(300) { network.update() }
            return (neuron.dataHolder as PointNeuronScalarData).membranePotential to neuron.activation
        }

        val (vmLow, actLow) = settle(0.3)
        assertEquals(0.0, actLow)
        assertTrue(vmLow > 0.3, "membrane potential rises with subthreshold input: $vmLow")
        assertTrue(settle(0.5).second > 0.5)
        assertTrue(settle(0.6).second > 0.5)
        assertEquals(0.0, settle(0.6, inh = 0.3).second)
        assertEquals(0.0, settle(0.6, leak = 0.6).second)

        assertEquals(0.3, settle(0.0).first, 0.01)
        assertEquals(0.0, settle(0.1, leak = 0.1).second)
        assertTrue(settle(0.2, leak = 0.1).second > 0.5)
    }

    @Test
    fun `layer inhibition sliders behave as the activities describe`() = runBlocking {
        val network = network(leabraInhibition)
        val layer = network.getModels<NeuronArray>().first { it.label == "Layer" }
        val rule = layer.updateRule as LeabraRule

        suspend fun percentActive(): Double {
            repeat(60) { network.update() }
            return 100.0 * layer.activationArray.count { it > 0.5 } / layer.size
        }

        suspend fun assertActive(range: ClosedFloatingPointRange<Double>) {
            val active = percentActive()
            assertTrue(active in range, "$active% active with gain ${rule.inhibitionGain}, " +
                    "feedforward ${rule.feedforward}, feedback ${rule.feedback}; expected $range")
        }

        assertActive(8.0..25.0)

        val input = network.getModels<NeuronArray>().first { it.label == "Input" }
        val pattern = input.activationArray.map { if (it > 0) 1.0 else 0.0 }
        for (strength in listOf(0.5, 0.6, 0.8, 1.0)) {
            input.activationArray = pattern.map { it * strength }.toDoubleArray()
            val active = percentActive()
            assertTrue(active <= 20.0, "strength $strength gives $active% active")
        }

        rule.inhibitionGain = 0.0
        assertActive(90.0..100.0)
        rule.inhibitionGain = 1.2
        assertActive(25.0..60.0)
        rule.inhibitionGain = 3.0
        assertActive(0.0..2.0)
        rule.inhibitionGain = 1.8
        assertActive(8.0..25.0)
        rule.feedback = 0.0
        assertActive(85.0..100.0)
        rule.feedback = 1.0
        rule.feedforward = 0.0
        assertActive(35.0..75.0)
        rule.feedforward = 1.0
        assertActive(8.0..25.0)
    }
}
