package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import kotlin.math.atan

class AdditiveRuleTest {

    fun g(x: Double, lambda: Double) = 2 / Math.PI * atan((Math.PI * lambda * x) / 2)

    val network = Network()

    val n1 = Neuron().also { network.addNetworkModel(it) }

    val n2 = Neuron().apply {
        updateRule = AdditiveRule()
    }.also { network.addNetworkModel(it) }

    @Test
    fun `test single synapse update`() {

        val synapse = Synapse(n1, n2).also {
            it.strength = 1.0
            network.addNetworkModel(it)
        }

        n1.activation = 1.0
        n1.clamped = true
        network.update()
        println(n2.activation)
        println(g(n1.activation, (n2.updateRule as AdditiveRule).lambda) * network.timeStep)
        assertEquals(g(n1.activation, (n2.updateRule as AdditiveRule).lambda) * network.timeStep, n2.activation, 1e-6)
    }

    @Test
    fun `test synapse group update`() {
        val nc1 = NeuronCollection(listOf(n1)).also { network.addNetworkModel(it) }
        val nc2 = NeuronCollection(listOf(n2)).also { network.addNetworkModel(it) }
        val synapseGroup = SynapseGroup(nc1, nc2).also { network.addNetworkModel(it) }
        synapseGroup.synapses.first().strength = 1.0
        n1.activation = 1.0
        n1.clamped = true
        network.update()

        println(n2.activation)
        println(g(n1.activation, (n2.updateRule as AdditiveRule).lambda) * network.timeStep)
        assertEquals(g(n1.activation, (n2.updateRule as AdditiveRule).lambda) * network.timeStep, n2.activation, 1e-6)
    }

    @Test
    fun `test weight matrix update`() {
        val nc1 = NeuronCollection(listOf(n1)).also { network.addNetworkModel(it) }
        val nc2 = NeuronCollection(listOf(n2)).also { network.addNetworkModel(it) }
        val weightMatrix = WeightMatrix(nc1, nc2).also { network.addNetworkModel(it) }
        weightMatrix.weights[0, 0] = 1.0
        n1.activation = 1.0
        n1.clamped = true
        network.update()

        println(n2.activation)
        println(g(n1.activation, (n2.updateRule as AdditiveRule).lambda) * network.timeStep)
        assertEquals(g(n1.activation, (n2.updateRule as AdditiveRule).lambda) * network.timeStep, n2.activation, 1e-6)
    }

}