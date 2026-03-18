package org.simbrain.workspace

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
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

        net.addNetworkModelsAsync(listOf(n1, n2, n3, s1, s2))
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
        // NeuronCollection.accumulateInputs() distributes WeightMatrix inputs to neurons.
        // NeuronCollection must run BEFORE its individual neurons at each priority level.
        // Since updatingOrder is Neuron(10) < NeuronCollection(20), we set neurons to a
        // higher priority number so the collection runs first.
        val inputNeurons = List(2) { Neuron().apply { priority = 2 } }
        val inputGroup = NeuronCollection(inputNeurons).apply {
            priority = 1
            neuronList[0].activation = 1.0
            neuronList[1].activation = -1.0
            isClamped = true
        }

        val hiddenNeurons = List(2) { Neuron().apply { priority = 4 } }
        val hiddenGroup = NeuronCollection(hiddenNeurons).apply {
            priority = 3
        }

        val outputNeurons = List(2) { Neuron().apply { priority = 6 } }
        val outputGroup = NeuronCollection(outputNeurons).apply {
            label = "Output"
            priority = 5
        }

        // identity matrices
        val wm1 = WeightMatrix(inputGroup, hiddenGroup)
        val wm2 = WeightMatrix(hiddenGroup, outputGroup)

        net.addNetworkModelsAsync(inputNeurons + hiddenNeurons + outputNeurons)
        net.addNetworkModelsAsync(listOf(inputGroup, hiddenGroup, outputGroup, wm1, wm2))

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

        val hiddenNeurons = List(2) { Neuron().apply { priority = 3 } }
        val hidden = NeuronCollection(hiddenNeurons).apply {
            priority = 2
        }

        val outputNeurons = List(2) { Neuron().apply { priority = 5 } }
        val output = NeuronCollection(outputNeurons).apply {
            label = "Output"
            priority = 4
        }

        // identity matrices
        val wm1 = WeightMatrix(input, hidden)
        val wm2 = WeightMatrix(hidden, output)

        net.addNetworkModelsAsync(hiddenNeurons + outputNeurons)
        net.addNetworkModelsAsync(listOf(input, hidden, output, wm1, wm2))

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
