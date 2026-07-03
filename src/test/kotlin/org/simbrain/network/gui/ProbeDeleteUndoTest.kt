package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.Probe
import org.simbrain.network.trainers.createProbe

/**
 * Deleting the host of a [Probe] through the GUI delete path: the probe must be deleted with its host
 * (not left dangling), the cascade must be captured for undo, and undo must restore both the host and
 * the probe without corrupting the host subnetwork's structure.
 */
class ProbeDeleteUndoTest : NetworkPanelDeleteUndoTestBase() {

    @Test
    fun `deleting a host subnetwork deletes a probe on its hidden layer and undo restores both`() = runBlocking {
        val bp = BackpropNetwork(intArrayOf(2, 3, 2), null)
        network.addNetworkModel(bp)
        val hidden = bp.hiddenLayers().first()
        val probe = with(network) { createProbe(hidden, readoutSize = 2, label = "Loop probe") }
        assertEquals(5, bp.modelList.size)

        selectOnly(bp)
        panel.deleteSelectedObjects()
        awaitUntil { network.getModels<Probe>().isEmpty() }
        assertEquals(0, network.getModels<Probe>().size, "deleting the host must delete the probe overlay")
        assertEquals(0, network.getModels<Subnetwork>().size)
        awaitUntil { network.getModels<NeuronArray>().none { it === probe.outputLayer } }
        assertTrue(network.getModels<NeuronArray>().none { it === probe.outputLayer },
            "deleting the host must also delete the probe's readout array")

        panel.undoManager.undo()
        assertEquals(1, network.getModels<Subnetwork>().size, "undo must restore the host")
        assertEquals(5, bp.modelList.size, "undo must restore the host's internal models")
        assertTrue(hidden in bp.modelList.all, "the probed layer must be restored INTO the host subnetwork")
        assertTrue(network.getModels<NeuronArray>().none { it === hidden },
            "the probed layer must not leak as a free top-level model")
        val restored = network.getModels<Probe>().firstOrNull()
        assertNotNull(restored, "undo must restore the probe")
        assertSame(probe, restored)
        assertSame(hidden, restored!!.probedModel)
        assertTrue(restored.weightMatrices.first() in hidden.outgoingConnectors,
            "the restored probe's weight matrix must be re-registered with the probed layer")

        panel.undoManager.redo()
        awaitUntil { network.getModels<Probe>().isEmpty() }
        assertEquals(0, network.getModels<Probe>().size, "redo must delete the probe again")
        assertEquals(0, network.getModels<Subnetwork>().size)
    }

    @Test
    fun `deleting a probed free layer deletes the probe and undo restores it`() = runBlocking {
        val hostInput = NeuronArray(4).apply { isClamped = true }
        val hostHidden = NeuronArray(3)
        val hostWm = WeightMatrix(hostInput, hostHidden)
        network.addNetworkModel(hostInput)
        network.addNetworkModel(hostHidden)
        network.addNetworkModel(hostWm)
        val probe = with(network) { createProbe(hostHidden, readoutSize = 2) }

        selectOnly(hostHidden)
        panel.deleteSelectedObjects()
        awaitUntil { network.getModels<Probe>().isEmpty() }
        assertEquals(0, network.getModels<Probe>().size, "deleting the probed layer must delete the probe")
        awaitUntil { network.getModels<NeuronArray>().none { it === probe.outputLayer } }
        assertTrue(network.getModels<NeuronArray>().none { it === probe.outputLayer },
            "deleting the probed layer must also delete the probe's readout array")

        panel.undoManager.undo()
        val restored = network.getModels<Probe>().firstOrNull()
        assertNotNull(restored, "undo must restore the probe")
        assertSame(probe, restored)
        assertSame(hostHidden, restored!!.probedModel)
        assertTrue(network.getModels<NeuronArray>().any { it === probe.outputLayer },
            "undo must restore the probe's readout array")

        panel.undoManager.redo()
        awaitUntil { network.getModels<Probe>().isEmpty() }
        assertEquals(0, network.getModels<Probe>().size, "redo must delete the probe again")
    }

    @Test
    fun `deleting a probe deletes its readout path and undo restores it`() = runBlocking {
        val hostInput = NeuronArray(4).apply { isClamped = true }
        val hostHidden = NeuronArray(3)
        val hostWm = WeightMatrix(hostInput, hostHidden)
        network.addNetworkModel(hostInput)
        network.addNetworkModel(hostHidden)
        network.addNetworkModel(hostWm)
        val probe = with(network) { createProbe(hostHidden, readoutSize = 2) }
        val readout = probe.outputLayer
        val probeWm = probe.weightMatrices.first()

        selectOnly(probe)
        panel.deleteSelectedObjects()
        awaitUntil { network.getModels<Probe>().isEmpty() }
        assertEquals(0, network.getModels<Probe>().size)
        awaitUntil { network.getModels<NeuronArray>().none { it === readout } }
        assertTrue(network.getModels<NeuronArray>().none { it === readout },
            "deleting the probe must delete its readout array")
        assertTrue(network.getModels<WeightMatrix>().none { it === probeWm },
            "deleting the probe must delete its weight matrix")
        assertTrue(probeWm !in hostHidden.outgoingConnectors,
            "the deleted weight matrix must be detached from the probed layer")
        assertTrue(network.getModels<NeuronArray>().any { it === hostHidden },
            "the probed host layer must survive probe deletion")

        panel.undoManager.undo()
        val restored = network.getModels<Probe>().firstOrNull()
        assertNotNull(restored, "undo must restore the probe")
        assertSame(probe, restored)
        assertTrue(network.getModels<NeuronArray>().any { it === readout },
            "undo must restore the readout array")
        assertTrue(probeWm in hostHidden.outgoingConnectors,
            "undo must re-register the probe's weight matrix with the probed layer")

        panel.undoManager.redo()
        awaitUntil { network.getModels<Probe>().isEmpty() }
        assertEquals(0, network.getModels<Probe>().size, "redo must delete the probe again")
        awaitUntil { network.getModels<NeuronArray>().none { it === readout } }
        assertTrue(network.getModels<NeuronArray>().none { it === readout },
            "redo must delete the readout array again")
        assertTrue(network.getModels<NeuronArray>().any { it === hostHidden },
            "the probed host layer must survive redo")
    }

    @Test
    fun `deleting a CNN host deletes a tensor probe and undo restores it`() = runBlocking {
        val inputTensorLayer = TensorLayer(TensorShape(3, 3, 1)).apply { isClamped = true }
        val convOutShape = TensorShape(3, 3, 1).convOutputShape(3, 1, Padding.SAME, 2)
        val convOut = TensorLayer(convOutShape)
        ConvolutionConnector(inputTensorLayer, convOut, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)
        val flatArray = NeuronArray(convOutShape.size)
        FlattenConnector(convOut, flatArray)
        val outputArray = NeuronArray(2)
        WeightMatrix(flatArray, outputArray)
        val cnn = network.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray)
        val probe = with(network) { createProbe(convOut, readoutSize = 2) }

        selectOnly(cnn)
        panel.deleteSelectedObjects()
        awaitUntil { network.getModels<Probe>().isEmpty() }
        assertEquals(0, network.getModels<Probe>().size,
            "deleting the CNN host must delete the probe on its tensor stage")
        awaitUntil { network.getModels<NeuronArray>().none { it === probe.inputLayer || it === probe.outputLayer } }
        assertTrue(network.getModels<NeuronArray>().none { it === probe.inputLayer || it === probe.outputLayer },
            "deleting the CNN host must also delete the probe's flatten and readout arrays")

        panel.undoManager.undo()
        assertEquals(1, network.getModels<Subnetwork>().size, "undo must restore the CNN")
        val restored = network.getModels<Probe>().firstOrNull()
        assertNotNull(restored, "undo must restore the probe")
        assertSame(probe, restored)
        assertSame(convOut, restored!!.probedModel)

        panel.undoManager.redo()
        awaitUntil { network.getModels<Probe>().isEmpty() }
        assertEquals(0, network.getModels<Probe>().size, "redo must delete the probe again")
    }
}
