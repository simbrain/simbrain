package org.simbrain.workspace

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.update_actions.BufferedUpdate
import org.simbrain.network.update_actions.PriorityUpdate

class PriorityUpdateTest {

    lateinit var net: Network
    lateinit var n1: Neuron
    lateinit var n2: Neuron
    lateinit var n3: Neuron
    lateinit var s1: Synapse
    lateinit var s2: Synapse

    @Test
    fun `test update for neurons and synapses`() {
        net = Network()
        net.updateManager.clear()
        net.updateManager.addAction(PriorityUpdate(net))

        n1 = Neuron().apply {
            priority = 1
            clamped = true
            activation = 0.5
        }

        n2 = Neuron().apply { priority = 2 }
        n3 = Neuron().apply { priority = 3 }

        s1 = Synapse(n1, n2)
        s2 = Synapse(n2, n3)

        net.addNetworkModels(listOf(n1, n2, n3, s1, s2))
        net.update()

        // With priority based updating, .5 makes it all the way in one update
        assertEquals(0.5, n3.activation, 0.001)

        // Compare Buffered Update, where it takes two updates to get there
        net.clearActivations()
        net.updateManager.clear()
        net.updateManager.addAction(BufferedUpdate(net))
        net.update()

        assertNotEquals(0.5, n3.activation, 0.001)
    }

    @Test
    fun `test priority updating with neuron groups`() {
        val net = Network()
        net.updateManager.clear()
        net.updateManager.addAction(PriorityUpdate(net))

        // Input group: two neurons with initial activations
        val inputGroup = NeuronGroup(2).apply {
            priority = 1
            neuronList[0].activation = 1.0
            neuronList[1].activation = -1.0
            isClamped = true
        }

        val hiddenGroup = NeuronGroup(2).apply {
            priority = 2
        }

        val outputGroup = NeuronGroup(2).apply {
            label = "Output"
            priority = 3
        }

        // identity matrices
        val wm1 = WeightMatrix(inputGroup, hiddenGroup)
        val wm2 = WeightMatrix(hiddenGroup, outputGroup)

        net.addNetworkModels(listOf(inputGroup, hiddenGroup, outputGroup, wm1, wm2))

        net.update()

        // Priority based makes it all the way in one shot
        assertEquals(1.0,outputGroup.neuronList[0].activation )
        assertEquals(-1.0,outputGroup.neuronList[1].activation )

        // Now reset and run BufferedUpdate to confirm no full propagation in one pass
        net.clearActivations()
        inputGroup.neuronList[0].activation = 1.0
        inputGroup.neuronList[1].activation = -1.0

        net.updateManager.clear()
        net.updateManager.addAction(BufferedUpdate(net))
        net.update()

        // Expect output activations not yet updated (still zero)
        assertTrue(outputGroup.neuronList.all { it.activation == 0.0 })
    }

    @Test
    fun `test priority updating with neuron arrays`() {
        val net = Network()
        net.updateManager.clear()
        net.updateManager.addAction(PriorityUpdate(net))

        val input = NeuronArray(2).apply {
            priority = 1
            activationArray = doubleArrayOf(1.0, -1.0)
            isClamped = true
        }

        val hidden = NeuronGroup(2).apply {
            priority = 2
        }

        val output = NeuronGroup(2).apply {
            label = "Output"
            priority = 3
        }

        // identity matrices
        val wm1 = WeightMatrix(input, hidden)
        val wm2 = WeightMatrix(hidden, output)

        net.addNetworkModels(listOf(input, hidden, output, wm1, wm2))

        net.update()

        // Priority based makes it all the way in one shot
        assertArrayEquals(doubleArrayOf(1.0, -1.0),output.activationArray)

        // Now reset and run BufferedUpdate to confirm no full propagation in one pass
        net.clearActivations()
        input.activationArray = doubleArrayOf(1.0, -1.0)


        net.updateManager.clear()
        net.updateManager.addAction(BufferedUpdate(net))
        net.update()

        // Expect output activations not yet updated (still zero)
        assertTrue(output.activationArray.sum() == 0.0)
    }
}
