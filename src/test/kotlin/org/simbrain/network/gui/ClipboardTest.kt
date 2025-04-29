package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.core.*
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.util.point
import org.simbrain.workspace.gui.SimbrainDesktop

class ClipboardTest {

    private lateinit var network: Network
    private lateinit var networkComponent: NetworkComponent

    @BeforeEach
    fun setUp() {
        SimbrainDesktop.workspace.clearWorkspace()
        // Clear the clipboard before each test
        Clipboard.clear()

        // Create a fresh network and panel for each test
        network = Network()
        networkComponent = NetworkComponent("Test", network)
        SimbrainDesktop.workspace.addWorkspaceComponent(networkComponent)
    }

    @Test
    fun `test is empty initially`() {
        // Verify that the clipboard is initially empty
        assertTrue(Clipboard.isEmpty, "Clipboard should be empty initially")
    }

    @Test
    fun `test clear clipboard`() {
        // Add a neuron to the network
        val neuron = Neuron()
        network.addNetworkModel(neuron)

        // Add the neuron to the clipboard
        Clipboard.add(listOf(neuron))

        // Verify that the clipboard is not empty
        assertFalse(Clipboard.isEmpty, "Clipboard should not be empty after adding an object")

        // Clear the clipboard
        Clipboard.clear()

        // Verify that the clipboard is empty again
        assertTrue(Clipboard.isEmpty, "Clipboard should be empty after clearing")
    }

    @Test
    fun `test add to clipboard`() {
        // Add a neuron to the network
        val neuron = Neuron()
        network.addNetworkModel(neuron)

        // Add the neuron to the clipboard
        Clipboard.add(listOf(neuron))

        // Verify that the clipboard is not empty
        assertFalse(Clipboard.isEmpty, "Clipboard should not be empty after adding an object")
    }

    @Test
    fun `test add multiple objects to clipboard`() {
        // Add multiple objects to the network
        val neuron1 = Neuron()
        val neuron2 = Neuron()
        val textObject = NetworkTextObject("Test Text")
        network.addNetworkModel(neuron1)
        network.addNetworkModel(neuron2)
        network.addNetworkModel(textObject)

        // Add the objects to the clipboard
        Clipboard.add(listOf(neuron1, neuron2, textObject))

        // Verify that the clipboard is not empty
        assertFalse(Clipboard.isEmpty, "Clipboard should not be empty after adding objects")
    }

    @Test
    fun `test clipboard listener`() {
        // Create a test listener
        var listenerCalled = false
        val testListener = object : ClipboardListener {
            override fun clipboardChanged() {
                listenerCalled = true
            }
        }

        // Add the listener to the clipboard
        Clipboard.addClipboardListener(testListener)

        // Add a neuron to the clipboard
        val neuron = Neuron()
        network.addNetworkModel(neuron)
        Clipboard.add(listOf(neuron))

        // Verify that the listener was called
        assertTrue(listenerCalled, "Clipboard listener should be called when clipboard changes")

        // Reset the flag and test clear
        listenerCalled = false
        Clipboard.clear()

        // Verify that the listener was called again
        assertTrue(listenerCalled, "Clipboard listener should be called when clipboard is cleared")
    }

    @Test
    fun `test paste empty clipboard`() = runBlocking {

        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel

        // Try to paste when clipboard is empty
        Clipboard.paste(networkPanel)

        // Verify that nothing was added to the network
        assertEquals(0, network.flatNeuronList.size, "No neurons should be added when pasting an empty clipboard")
        assertEquals(0, network.getModels<NetworkTextObject>().size, "No text objects should be added when pasting an empty clipboard")
    }

    @Test
    fun `test paste neuron`() = runBlocking {

        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel

        // Add a neuron to the network with specific properties
        val originalNeuron = Neuron().apply {
            label = "Test Neuron"
            activation = 0.5
            x = 100.0
            y = 100.0
        }
        network.addNetworkModel(originalNeuron)

        // Add the neuron to the clipboard
        Clipboard.add(listOf(originalNeuron))

        // Get the initial count of neurons
        val initialNeuronCount = network.flatNeuronList.size

        // Paste the clipboard contents
        Clipboard.paste(networkPanel)

        // Verify that a neuron was added
        assertEquals(initialNeuronCount + 1, network.flatNeuronList.size, "A neuron should be added after pasting")

        // Get the pasted neuron
        val pastedNeuron = network.flatNeuronList.last()

        // Verify that the pasted neuron has the same properties as the original
        assertEquals(originalNeuron.label, pastedNeuron.label, "Pasted neuron should have the same label")
        assertEquals(originalNeuron.activation, pastedNeuron.activation, "Pasted neuron should have the same activation")

        // The pasted neuron should have a different ID
        assertNotEquals(originalNeuron.id, pastedNeuron.id, "Pasted neuron should have a different ID")
    }

    @Test
    fun `test paste text object`() = runBlocking {

        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel

        // Add a text object to the network
        val originalText = "Test Text"
        val textObject = NetworkTextObject(originalText).apply {
            location = java.awt.geom.Point2D.Double(100.0, 100.0)
        }
        network.addNetworkModel(textObject)

        // Add the text object to the clipboard
        Clipboard.add(listOf(textObject))

        // Get the initial count of text objects
        val initialTextCount = network.getModels<NetworkTextObject>().size

        // Paste the clipboard contents
        Clipboard.paste(networkPanel)

        // Verify that a text object was added
        assertEquals(initialTextCount + 1, network.getModels<NetworkTextObject>().size, "A text object should be added after pasting")

        // Get the pasted text object
        val pastedText = network.getModels<NetworkTextObject>().last()

        // Verify that the pasted text object has the same text as the original
        assertEquals(originalText, pastedText.text, "Pasted text object should have the same text")

        // The pasted text object should have a different ID
        assertNotEquals(textObject.id, pastedText.id, "Pasted text object should have a different ID")
    }

    @Test
    fun `test paste neuron with synapse`() = runBlocking {

        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel

        // Add two neurons to the network
        val sourceNeuron = Neuron().apply {
            label = "Source"
            x = 100.0
            y = 100.0
        }
        val targetNeuron = Neuron().apply {
            label = "Target"
            x = 200.0
            y = 100.0
        }
        network.addNetworkModel(sourceNeuron)?.await()
        network.addNetworkModel(targetNeuron)?.await()

        // Create a synapse between the neurons
        val synapse = Synapse(sourceNeuron, targetNeuron).apply {
            strength = 0.75
        }
        network.addNetworkModel(synapse)?.await()

        // Add both neurons and the synapse to the clipboard
        Clipboard.add(listOf(sourceNeuron, targetNeuron, synapse))

        // Get the initial counts
        val initialNeuronCount = network.flatNeuronList.size
        val initialSynapseCount = network.flatSynapseList.size

        // Paste the clipboard contents
        Clipboard.paste(networkPanel)

        // Verify that neurons and synapse were added
        assertEquals(initialNeuronCount + 2, network.flatNeuronList.size, "Two neurons should be added after pasting")
        assertEquals(initialSynapseCount + 1, network.flatSynapseList.size, "A synapse should be added after pasting")

        // Get the pasted synapse
        val pastedSynapse = network.flatSynapseList.last()

        // Verify that the pasted synapse has the same strength as the original
        assertEquals(synapse.strength, pastedSynapse.strength, "Pasted synapse should have the same strength")

        // The pasted synapse should connect the pasted neurons
        val pastedSourceNeuron = network.flatNeuronList[initialNeuronCount]
        val pastedTargetNeuron = network.flatNeuronList[initialNeuronCount + 1]

        assertEquals(pastedSourceNeuron, pastedSynapse.source, "Pasted synapse should connect to the pasted source neuron")
        assertEquals(pastedTargetNeuron, pastedSynapse.target, "Pasted synapse should connect to the pasted target neuron")
    }

    @Test
    fun `test undo redo paste`() = runBlocking {

        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel

        // Add a neuron to the network
        val neuron = Neuron().apply {
            label = "Test Neuron"
            activation = 0.5
        }
        network.addNetworkModel(neuron)

        // Add the neuron to the clipboard
        Clipboard.add(listOf(neuron))

        // Get the initial count of neurons
        val initialNeuronCount = network.flatNeuronList.size

        // Paste the clipboard contents
        Clipboard.paste(networkPanel)

        // Verify that a neuron was added
        assertEquals(initialNeuronCount + 1, network.flatNeuronList.size, "A neuron should be added after pasting")

        // Get the ID of the pasted neuron
        val pastedNeuronId = network.flatNeuronList.last().id

        // Undo the paste action
        networkPanel.undoManager.undo()

        // Verify that the pasted neuron was removed
        assertEquals(initialNeuronCount, network.flatNeuronList.size, "The pasted neuron should be removed after undo")
        assertFalse(
            network.flatNeuronList.any { it.id == pastedNeuronId },
            "The pasted neuron should not be in the network after undo"
        )

        // Redo the paste action
        networkPanel.undoManager.redo()

        // Verify that the neuron was added back
        assertEquals(initialNeuronCount + 1, network.flatNeuronList.size, "The neuron should be added back after redo")
        assertTrue(
            network.flatNeuronList.any { it.id == pastedNeuronId },
            "A neuron with the same ID should be in the network after redo"
        )
    }

    @Test
    fun `copy neuron collection should copy both the collection and make new neurons`() = runBlocking {
        // Create a neuron collection
        val neuron1 = Neuron().apply {
            label = "Neuron 1"
            activation = 0.5
            x = 100.0
            y = 100.0
        }
        val neuron2 = Neuron().apply {
            label = "Neuron 2"
            activation = 0.8
            x = 150.0
            y = 100.0
        }
        network.addNetworkModel(neuron1)
        network.addNetworkModel(neuron2)
        
        // Create a neuron collection
        val originalCollection = NeuronCollection(listOf(neuron1, neuron2))
        network.addNetworkModel(originalCollection)
        
        // Add the collection to the clipboard
        Clipboard.add(listOf(originalCollection))
        
        // Verify the clipboard is not empty
        assertFalse(Clipboard.isEmpty, "Clipboard should not be empty after adding a neuron collection")
        
        // Get the network panel
        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel
        
        // Initial counts
        val initialNeuronCount = network.flatNeuronList.size
        val initialCollectionCount = network.getModels<NeuronCollection>().size
        
        // Paste the clipboard contents
        runBlocking {
            Clipboard.paste(networkPanel)
        }
        
        // Verify a new collection was created
        assertEquals(initialCollectionCount + 1, network.getModels<NeuronCollection>().size, 
            "A new neuron collection should be created after pasting")
        
        // Verify new neurons were created (not just references to existing ones)
        assertEquals(initialNeuronCount + 2, network.flatNeuronList.size, 
            "New neurons should be created after pasting a neuron collection")
        
        // Get the pasted collection
        val pastedCollection = network.getModels<NeuronCollection>().last()
        
        // Verify the collection is not the same as the original
        assertNotEquals(originalCollection.id, pastedCollection.id, 
            "The pasted collection should have a different ID than the original")
        
        // Verify the neurons in the pasted collection are new (not the same as the original)
        val pastedNeurons = pastedCollection.neuronList
        assertEquals(2, pastedNeurons.size, "The pasted collection should have the same number of neurons")
        
        pastedNeurons.forEach { pastedNeuron ->
            assertFalse(listOf(neuron1.id, neuron2.id).contains(pastedNeuron.id),
                "The pasted neurons should have different IDs than the original neurons")
        }
        
        // Verify the neurons have the same properties
        val sortedOriginalNeurons = listOf(neuron1, neuron2).sortedBy { it.label }
        val sortedPastedNeurons = pastedNeurons.sortedBy { it.label }
        
        for (i in sortedOriginalNeurons.indices) {
            assertEquals(sortedOriginalNeurons[i].label, sortedPastedNeurons[i].label,
                "The pasted neurons should have the same labels as the original neurons")
            assertEquals(sortedOriginalNeurons[i].activation, sortedPastedNeurons[i].activation,
                "The pasted neurons should have the same activation as the original neurons")
        }
    }

    @Test
    fun `test neuron collection copy paste undo redo`() = runBlocking {
        // Create neurons
        val neuron1 = Neuron().apply {
            label = "Neuron 1"
            activation = 0.5
            x = 100.0
            y = 100.0
        }
        val neuron2 = Neuron().apply {
            label = "Neuron 2"
            activation = 0.8
            x = 150.0
            y = 100.0
        }
        network.addNetworkModel(neuron1)
        network.addNetworkModel(neuron2)
        
        // Create a neuron collection
        val originalCollection = NeuronCollection(listOf(neuron1, neuron2))
        network.addNetworkModel(originalCollection)
        
        // Add the collection to the clipboard
        Clipboard.add(listOf(originalCollection))
        
        // Get the network panel
        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel
        
        // Get initial counts
        val initialNeuronCount = network.flatNeuronList.size
        val initialCollectionCount = network.getModels<NeuronCollection>().size
        
        // Paste the clipboard contents
        Clipboard.paste(networkPanel)
        
        // Verify new items were added
        assertEquals(initialCollectionCount + 1, network.getModels<NeuronCollection>().size, 
            "A new neuron collection should be created after pasting")
        assertEquals(initialNeuronCount + 2, network.flatNeuronList.size, 
            "New neurons should be created after pasting")
        
        // Get the IDs of the pasted collection and neurons
        val pastedCollectionId = network.getModels<NeuronCollection>().last().id
        val pastedNeuronIds = network.getModels<NeuronCollection>().last().neuronList.map { it.id }
        
        // Undo the paste action
        networkPanel.undoManager.undo()
        
        // Verify that pasted items were removed
        assertEquals(initialCollectionCount, network.getModels<NeuronCollection>().size, 
            "The pasted collection should be removed after undo")
        assertEquals(initialNeuronCount, network.flatNeuronList.size, 
            "The pasted neurons should be removed after undo")
        
        // Check that the pasted collection is no longer in the network
        assertFalse(
            network.getModels<NeuronCollection>().any { it.id == pastedCollectionId },
            "The pasted collection should not be in the network after undo"
        )
        
        // Check that the pasted neurons are no longer in the network
        pastedNeuronIds.forEach { neuronId ->
            assertFalse(
                network.flatNeuronList.any { it.id == neuronId },
                "The pasted neuron should not be in the network after undo"
            )
        }
        
        // Redo the paste action
        networkPanel.undoManager.redo()
        
        // Verify that items were added back
        assertEquals(initialCollectionCount + 1, network.getModels<NeuronCollection>().size, 
            "The collection should be added back after redo")
        assertEquals(initialNeuronCount + 2, network.flatNeuronList.size, 
            "The neurons should be added back after redo")
        
        // Check that a collection with the same ID is back in the network
        assertTrue(
            network.getModels<NeuronCollection>().any { it.id == pastedCollectionId },
            "A collection with the same ID should be in the network after redo"
        )
        
        // Check that neurons with the same IDs are back in the network
        pastedNeuronIds.forEach { neuronId ->
            assertTrue(
                network.flatNeuronList.any { it.id == neuronId },
                "A neuron with the same ID should be in the network after redo"
            )
        }
    }

    @Test
    fun `copy synapse group should copy both the group and make new synapses`() = runBlocking {
        // Create source and target neuron collections
        val sourceNeurons = listOf(
            Neuron().apply {
                label = "Source 1"
                activation = 0.5
                x = 100.0
                y = 100.0
            },
            Neuron().apply {
                label = "Source 2"
                activation = 0.7
                x = 150.0
                y = 100.0
            }
        )
        
        val targetNeurons = listOf(
            Neuron().apply {
                label = "Target 1"
                activation = 0.2
                x = 300.0
                y = 100.0
            },
            Neuron().apply {
                label = "Target 2"
                activation = 0.3
                x = 350.0
                y = 100.0
            }
        )
        
        // Add neurons to network
        sourceNeurons.forEach { network.addNetworkModel(it) }
        targetNeurons.forEach { network.addNetworkModel(it) }
        
        // Create neuron collections
        val sourceCollection = NeuronCollection(sourceNeurons)
        val targetCollection = NeuronCollection(targetNeurons)
        
        network.addNetworkModel(sourceCollection)
        network.addNetworkModel(targetCollection)
        
        // Create a synapse group connecting the collections using a connection strategy
        val connectionStrategy = AllToAll()
        val synapseGroup = SynapseGroup(sourceCollection, targetCollection, connectionStrategy)
        network.addNetworkModel(synapseGroup)
        
        // Verify synapse group has the expected number of synapses (2 source * 2 target = 4 synapses)
        assertEquals(4, synapseGroup.synapses.size, "Synapse group should have 4 synapses")
        
        // Add both collections and the synapse group to clipboard
        Clipboard.add(listOf(sourceCollection, targetCollection, synapseGroup))
        
        // Verify clipboard is not empty
        assertFalse(Clipboard.isEmpty, "Clipboard should not be empty")
        
        // Get the network panel
        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel
        
        // Initial counts
        val initialNeuronCount = network.flatNeuronList.size
        val initialCollectionCount = network.getModels<NeuronCollection>().size
        val initialSynapseCount = network.flatSynapseList.size
        val initialSynapseGroupCount = network.getModels<SynapseGroup>().size
        
        // Paste the clipboard contents
        Clipboard.paste(networkPanel)
        
        // Verify new items were created
        assertEquals(initialNeuronCount + 4, network.flatNeuronList.size,
            "Four new neurons should be created after pasting")
        assertEquals(initialCollectionCount + 2, network.getModels<NeuronCollection>().size, 
            "Two new neuron collections should be created after pasting")
        assertEquals(initialSynapseCount + 4, network.flatSynapseList.size, 
            "Four new synapses should be created after pasting")
        assertEquals(initialSynapseGroupCount + 1, network.getModels<SynapseGroup>().size,
            "One new synapse group should be created after pasting")
        
        // Get the pasted synapse group
        val pastedSynapseGroup = network.getModels<SynapseGroup>().last()
        
        // Verify the pasted synapse group is not the same as the original
        assertNotEquals(synapseGroup.id, pastedSynapseGroup.id, 
            "The pasted synapse group should have a different ID than the original")
        
        // Verify the pasted synapse group has the correct number of synapses
        assertEquals(4, pastedSynapseGroup.synapses.size, 
            "The pasted synapse group should have the same number of synapses")
        
        // Verify the pasted synapse group has the same connection strategy type
        assertEquals(synapseGroup.connectionStrategy.javaClass, pastedSynapseGroup.connectionStrategy.javaClass,
            "The pasted synapse group should have the same connection strategy type")
        
        // Verify the pasted synapse group connects the pasted collections (not the original ones)
        assertNotEquals(sourceCollection.id, pastedSynapseGroup.source.id,
            "The pasted synapse group should connect the new source collection, not the original")
        assertNotEquals(targetCollection.id, pastedSynapseGroup.target.id,
            "The pasted synapse group should connect the new target collection, not the original")
        
        // Verify that pasted synapses connect pasted neurons (not the original ones)
        pastedSynapseGroup.synapses.forEach { synapse ->
            // Source check
            assertFalse(sourceNeurons.map { it.id }.contains(synapse.source.id),
                "The pasted synapse should not connect to an original source neuron")
            
            // Target check
            assertFalse(targetNeurons.map { it.id }.contains(synapse.target.id),
                "The pasted synapse should not connect to an original target neuron")
            
            // Should connect to neurons in the pasted source collection
            assertTrue(pastedSynapseGroup.source.neuronList.map { it.id }.contains(synapse.source.id),
                "The pasted synapse should connect to a neuron in the pasted source collection")
            
            // Should connect to neurons in the pasted target collection
            assertTrue(pastedSynapseGroup.target.neuronList.map { it.id }.contains(synapse.target.id),
                "The pasted synapse should connect to a neuron in the pasted target collection")
        }
    }

    @Test
    fun `test synapse group copy paste undo redo`() = runBlocking {
        // Create source and target neuron collections
        val sourceNeurons = listOf(
            Neuron().apply {
                label = "Source 1"
                activation = 0.5
                x = 100.0
                y = 100.0
            },
            Neuron().apply {
                label = "Source 2"
                activation = 0.7
                x = 150.0
                y = 100.0
            }
        )
        
        val targetNeurons = listOf(
            Neuron().apply {
                label = "Target 1"
                activation = 0.2
                x = 300.0
                y = 100.0
            },
            Neuron().apply {
                label = "Target 2"
                activation = 0.3
                x = 350.0
                y = 100.0
            }
        )
        
        // Add neurons to network
        sourceNeurons.forEach { network.addNetworkModel(it) }
        targetNeurons.forEach { network.addNetworkModel(it) }
        
        // Create neuron collections
        val sourceCollection = NeuronCollection(sourceNeurons)
        val targetCollection = NeuronCollection(targetNeurons)
        
        network.addNetworkModel(sourceCollection)
        network.addNetworkModel(targetCollection)
        
        // Create a synapse group connecting the collections using a connection strategy
        val connectionStrategy = AllToAll()
        val synapseGroup = SynapseGroup(sourceCollection, targetCollection, connectionStrategy)
        network.addNetworkModel(synapseGroup)
        
        // Add both collections and the synapse group to clipboard
        Clipboard.add(listOf(sourceCollection, targetCollection, synapseGroup))
        
        // Get the network panel
        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel
        
        // Initial counts
        val initialNeuronCount = network.flatNeuronList.size
        val initialCollectionCount = network.getModels<NeuronCollection>().size
        val initialSynapseCount = network.flatSynapseList.size
        val initialSynapseGroupCount = network.getModels<SynapseGroup>().size
        
        // Paste the clipboard contents
        Clipboard.paste(networkPanel)
        
        // Verify items were added
        assertEquals(initialNeuronCount + 4, network.flatNeuronList.size, 
            "Four new neurons should be created after pasting")
        assertEquals(initialCollectionCount + 2, network.getModels<NeuronCollection>().size, 
            "Two new neuron collections should be created after pasting")
        assertEquals(initialSynapseCount + 4, network.flatSynapseList.size, 
            "Four new synapses should be created after pasting")
        assertEquals(initialSynapseGroupCount + 1, network.getModels<SynapseGroup>().size, 
            "One new synapse group should be created after pasting")
        
        // Get the pasted items' IDs
        val pastedSynapseGroup = network.getModels<SynapseGroup>().last()
        val pastedSynapseGroupId = pastedSynapseGroup.id
        val pastedSourceCollectionId = pastedSynapseGroup.source.id
        val pastedTargetCollectionId = pastedSynapseGroup.target.id
        val pastedSynapseIds = pastedSynapseGroup.synapses.map { it.id }
        val pastedSourceNeuronIds = pastedSynapseGroup.source.neuronList.map { it.id }
        val pastedTargetNeuronIds = pastedSynapseGroup.target.neuronList.map { it.id }
        
        // Undo the paste action
        networkPanel.undoManager.undo()
        
        // Verify that all pasted items were removed
        assertEquals(initialNeuronCount, network.flatNeuronList.size, 
            "All pasted neurons should be removed after undo")
        assertEquals(initialCollectionCount, network.getModels<NeuronCollection>().size, 
            "All pasted neuron collections should be removed after undo")
        assertEquals(initialSynapseCount, network.flatSynapseList.size, 
            "All pasted synapses should be removed after undo")
        assertEquals(initialSynapseGroupCount, network.getModels<SynapseGroup>().size, 
            "All pasted synapse groups should be removed after undo")
        
        // Check that pasted items are no longer in the network
        assertFalse(network.getModels<SynapseGroup>().any { it.id == pastedSynapseGroupId },
            "The pasted synapse group should not be in the network after undo")
        assertFalse(network.getModels<NeuronCollection>().any { it.id == pastedSourceCollectionId },
            "The pasted source collection should not be in the network after undo")
        assertFalse(network.getModels<NeuronCollection>().any { it.id == pastedTargetCollectionId },
            "The pasted target collection should not be in the network after undo")
        
        pastedSynapseIds.forEach { synapseId ->
            assertFalse(network.flatSynapseList.any { it.id == synapseId },
                "The pasted synapse should not be in the network after undo")
        }
        
        pastedSourceNeuronIds.forEach { neuronId ->
            assertFalse(network.flatNeuronList.any { it.id == neuronId },
                "The pasted source neuron should not be in the network after undo")
        }
        
        pastedTargetNeuronIds.forEach { neuronId ->
            assertFalse(network.flatNeuronList.any { it.id == neuronId },
                "The pasted target neuron should not be in the network after undo")
        }
        
        // Redo the paste action
        networkPanel.undoManager.redo()
        
        // Verify that all items were added back
        assertEquals(initialNeuronCount + 4, network.flatNeuronList.size, 
            "All neurons should be added back after redo")
        assertEquals(initialCollectionCount + 2, network.getModels<NeuronCollection>().size, 
            "All neuron collections should be added back after redo")
        assertEquals(initialSynapseCount + 4, network.flatSynapseList.size, 
            "All synapses should be added back after redo")
        assertEquals(initialSynapseGroupCount + 1, network.getModels<SynapseGroup>().size, 
            "All synapse groups should be added back after redo")
        
        // Check that items with the same IDs are back in the network
        assertTrue(network.getModels<SynapseGroup>().any { it.id == pastedSynapseGroupId },
            "A synapse group with the same ID should be in the network after redo")
        assertTrue(network.getModels<NeuronCollection>().any { it.id == pastedSourceCollectionId },
            "A source collection with the same ID should be in the network after redo")
        assertTrue(network.getModels<NeuronCollection>().any { it.id == pastedTargetCollectionId },
            "A target collection with the same ID should be in the network after redo")
        
        pastedSynapseIds.forEach { synapseId ->
            assertTrue(network.flatSynapseList.any { it.id == synapseId },
                "A synapse with the same ID should be in the network after redo")
        }
        
        pastedSourceNeuronIds.forEach { neuronId ->
            assertTrue(network.flatNeuronList.any { it.id == neuronId },
                "A source neuron with the same ID should be in the network after redo")
        }
        
        pastedTargetNeuronIds.forEach { neuronId ->
            assertTrue(network.flatNeuronList.any { it.id == neuronId },
                "A target neuron with the same ID should be in the network after redo")
        }
        
        // Verify connections are maintained after redo
        val redoSynapseGroup = network.getModels<SynapseGroup>().first { it.id == pastedSynapseGroupId }
        
        // Check that the synapse group still connects the proper collections
        assertEquals(pastedSourceCollectionId, redoSynapseGroup.source.id,
            "The redo synapse group should connect to the proper source collection")
        assertEquals(pastedTargetCollectionId, redoSynapseGroup.target.id,
            "The redo synapse group should connect to the proper target collection")
        
        // Check that the synapse group has the same number of synapses
        assertEquals(pastedSynapseIds.size, redoSynapseGroup.synapses.size,
            "The redo synapse group should have the same number of synapses")
        
        // Check that all the synapses have the same IDs
        val redoSynapseIds = redoSynapseGroup.synapses.map { it.id }
        assertTrue(redoSynapseIds.containsAll(pastedSynapseIds) && pastedSynapseIds.containsAll(redoSynapseIds),
            "The redo synapse group should contain the same synapses")
    }

    @Test
    fun `test neuron arrays and weight matrix copy paste`() = runBlocking {
        // Create two neuron arrays
        val sourceArray = NeuronArray(3).apply {
            label = "Source Array"
            location = point(100.0, 100.0)
        }
        val targetArray = NeuronArray(2).apply {
            label = "Target Array"
            location = point(100.0, 300.0)
        }
        
        // Add the arrays to the network
        network.addNetworkModel(sourceArray)
        network.addNetworkModel(targetArray)
        
        // Create a weight matrix connecting the arrays
        val weightMatrix = WeightMatrix(sourceArray, targetArray)
        
        // Set specific weights in the matrix
        val weights = weightMatrix.weights
        weights[0, 0] = 1.0  // target row 0, source column 0
        weights[0, 1] = 0.5  // target row 0, source column 1
        weights[0, 2] = 0.0  // target row 0, source column 2
        weights[1, 0] = 0.0  // target row 1, source column 0
        weights[1, 1] = 0.0  // target row 1, source column 1
        weights[1, 2] = 1.0  // target row 1, source column 2
        
        network.addNetworkModel(weightMatrix)
        
        // Add all components to the clipboard
        Clipboard.add(listOf(sourceArray, targetArray, weightMatrix))
        
        // Verify the clipboard is not empty
        assertFalse(Clipboard.isEmpty, "Clipboard should not be empty")
        
        // Get the network panel
        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel
        
        // Get initial counts
        val initialArrayCount = network.getModels<NeuronArray>().size
        val initialWeightMatrixCount = network.getModels<WeightMatrix>().size
        
        // Paste the clipboard contents
        Clipboard.paste(networkPanel)
        
        // Verify new items were created
        assertEquals(initialArrayCount + 2, network.getModels<NeuronArray>().size, 
            "Two new neuron arrays should be created after pasting")
        assertEquals(initialWeightMatrixCount + 1, network.getModels<WeightMatrix>().size, 
            "One new weight matrix should be created after pasting")
        
        // Get the pasted components
        val pastedSourceArray = network.getModels<NeuronArray>().firstOrNull { 
            it.id != sourceArray.id && it.label == sourceArray.label 
        }
        val pastedTargetArray = network.getModels<NeuronArray>().firstOrNull { 
            it.id != targetArray.id && it.label == targetArray.label 
        }
        val pastedWeightMatrix = network.getModels<WeightMatrix>().first { it.id != weightMatrix.id }
        
        // Verify the arrays were copied
        assertNotNull(pastedSourceArray, "Pasted source array should exist")
        assertNotNull(pastedTargetArray, "Pasted target array should exist")
        
        // Verify the arrays have the same dimensions
        assertEquals(sourceArray.size, pastedSourceArray!!.size, 
            "Pasted source array should have the same size")
        assertEquals(targetArray.size, pastedTargetArray!!.size, 
            "Pasted target array should have the same size")
        
        // Verify the weight matrix connects the pasted arrays
        assertEquals(pastedSourceArray.id, pastedWeightMatrix.source.id, 
            "Pasted weight matrix should connect to the pasted source array")
        assertEquals(pastedTargetArray.id, pastedWeightMatrix.target.id, 
            "Pasted weight matrix should connect to the pasted target array")
        
        // Verify the weight matrix has the same dimensions
        assertEquals(weightMatrix.weights.nrow(), pastedWeightMatrix.weights.nrow(), 
            "Pasted weight matrix should have the same number of rows")
        assertEquals(weightMatrix.weights.ncol(), pastedWeightMatrix.weights.ncol(), 
            "Pasted weight matrix should have the same number of columns")
        
        // Verify the weight matrix has the same values
        for (row in 0 until weightMatrix.weights.nrow()) {
            for (col in 0 until weightMatrix.weights.ncol()) {
                assertEquals(weightMatrix.weights[row, col], pastedWeightMatrix.weights[row, col], 0.001, 
                    "Weight at position [$row, $col] should be copied correctly")
            }
        }
        
        // Test that the connectivity works by simulating input and output
        // Set activations in the pasted source array
        val sourceActivations = doubleArrayOf(1.0, 0.5, 0.2)
        pastedSourceArray.setActivations(sourceActivations)
        
        // Update the network to propagate the values
        network.update()
        
        // Calculate the expected output manually
        // Note: When a network updates, inputs are transmitted to activations
        // and then the network update occurs which transfers those activations through connections
        val expectedOutput = doubleArrayOf(
            sourceActivations[0] * weights[0, 0] + sourceActivations[1] * weights[0, 1] + sourceActivations[2] * weights[0, 2],
            sourceActivations[0] * weights[1, 0] + sourceActivations[1] * weights[1, 1] + sourceActivations[2] * weights[1, 2]
        )
        
        // Expected: source[0]*1.0 + source[1]*0.5 + source[2]*0.0 = 1.0*1.0 + 0.5*0.5 + 0.2*0.0 = 1.0 + 0.25 = 1.25
        // Expected: source[0]*0.0 + source[1]*0.0 + source[2]*1.0 = 1.0*0.0 + 0.5*0.0 + 0.2*1.0 = 0.0 + 0.0 + 0.2 = 0.2
        
        // Verify the target array received the expected activations
        val targetActivations = pastedTargetArray.activationArray
        assertArrayEquals(expectedOutput, targetActivations, 0.001, 
            "Pasted weight matrix should correctly propagate activations")
    }

    @Test
    fun `test neuron arrays and weight matrix copy paste undo redo`() = runBlocking {
        // Create two neuron arrays
        val sourceArray = NeuronArray(3).apply {
            label = "Source Array"
            location = point(100.0, 100.0)
        }
        val targetArray = NeuronArray(2).apply {
            label = "Target Array"
            location = point(100.0, 300.0)
        }
        
        // Add the arrays to the network
        network.addNetworkModel(sourceArray)
        network.addNetworkModel(targetArray)
        
        // Create a weight matrix connecting the arrays
        val weightMatrix = WeightMatrix(sourceArray, targetArray)
        
        // Set specific weights in the matrix
        val weights = weightMatrix.weights
        weights[0, 0] = 1.0  // target row 0, source column 0
        weights[0, 1] = 0.5  // target row 0, source column 1
        weights[0, 2] = 0.0  // target row 0, source column 2
        weights[1, 0] = 0.0  // target row 1, source column 0
        weights[1, 1] = 0.0  // target row 1, source column 1
        weights[1, 2] = 1.0  // target row 1, source column 2
        
        network.addNetworkModel(weightMatrix)
        
        // Add all components to the clipboard
        Clipboard.add(listOf(sourceArray, targetArray, weightMatrix))
        
        // Get the network panel
        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel
        
        // Get initial counts
        val initialArrayCount = network.getModels<NeuronArray>().size
        val initialWeightMatrixCount = network.getModels<WeightMatrix>().size
        
        // Paste the clipboard contents
        Clipboard.paste(networkPanel)
        
        // Verify new items were created
        assertEquals(initialArrayCount + 2, network.getModels<NeuronArray>().size, 
            "Two new neuron arrays should be created after pasting")
        assertEquals(initialWeightMatrixCount + 1, network.getModels<WeightMatrix>().size, 
            "One new weight matrix should be created after pasting")
        
        // Get the pasted items' IDs
        val pastedArrays = network.getModels<NeuronArray>().filter { it.id != sourceArray.id && it.id != targetArray.id }
        val pastedSourceArray = pastedArrays.first { it.label == sourceArray.label }
        val pastedTargetArray = pastedArrays.first { it.label == targetArray.label }
        val pastedWeightMatrix = network.getModels<WeightMatrix>().first { it.id != weightMatrix.id }
        
        val pastedSourceArrayId = pastedSourceArray.id
        val pastedTargetArrayId = pastedTargetArray.id
        val pastedWeightMatrixId = pastedWeightMatrix.id
        
        // Undo the paste action
        networkPanel.undoManager.undo()
        
        // Verify that all pasted items were removed
        assertEquals(initialArrayCount, network.getModels<NeuronArray>().size, 
            "All pasted neuron arrays should be removed after undo")
        assertEquals(initialWeightMatrixCount, network.getModels<WeightMatrix>().size, 
            "All pasted weight matrices should be removed after undo")
        
        // Check that pasted items are no longer in the network
        assertFalse(network.getModels<NeuronArray>().any { it.id == pastedSourceArrayId },
            "The pasted source array should not be in the network after undo")
        assertFalse(network.getModels<NeuronArray>().any { it.id == pastedTargetArrayId },
            "The pasted target array should not be in the network after undo")
        assertFalse(network.getModels<WeightMatrix>().any { it.id == pastedWeightMatrixId },
            "The pasted weight matrix should not be in the network after undo")
        
        // Redo the paste action
        networkPanel.undoManager.redo()
        
        // Verify that all items were added back
        assertEquals(initialArrayCount + 2, network.getModels<NeuronArray>().size, 
            "All neuron arrays should be added back after redo")
        assertEquals(initialWeightMatrixCount + 1, network.getModels<WeightMatrix>().size, 
            "All weight matrices should be added back after redo")
        
        // Check that items with the same IDs are back in the network
        assertTrue(network.getModels<NeuronArray>().any { it.id == pastedSourceArrayId },
            "A source array with the same ID should be in the network after redo")
        assertTrue(network.getModels<NeuronArray>().any { it.id == pastedTargetArrayId },
            "A target array with the same ID should be in the network after redo")
        assertTrue(network.getModels<WeightMatrix>().any { it.id == pastedWeightMatrixId },
            "A weight matrix with the same ID should be in the network after redo")
        
        // Get the restored components
        val redoSourceArray = network.getModels<NeuronArray>().first { it.id == pastedSourceArrayId }
        val redoTargetArray = network.getModels<NeuronArray>().first { it.id == pastedTargetArrayId }
        val redoWeightMatrix = network.getModels<WeightMatrix>().first { it.id == pastedWeightMatrixId }
        
        // Verify the weight matrix still connects the proper arrays
        assertEquals(redoSourceArray.id, redoWeightMatrix.source.id,
            "The redo weight matrix should connect to the proper source array")
        assertEquals(redoTargetArray.id, redoWeightMatrix.target.id,
            "The redo weight matrix should connect to the proper target array")
        
        // Verify the weight matrix still has the same values
        for (row in 0 until weightMatrix.weights.nrow()) {
            for (col in 0 until weightMatrix.weights.ncol()) {
                assertEquals(weightMatrix.weights[row, col], redoWeightMatrix.weights[row, col], 0.001, 
                    "Weight at position [$row, $col] should be preserved after redo")
            }
        }
        
        // Test that the connectivity still works by simulating input and output
        // Set activations in the restored source array
        val sourceActivations = doubleArrayOf(1.0, 0.5, 0.2)
        redoSourceArray.setActivations(sourceActivations)
        redoSourceArray.isClamped = true
        
        // Update the network to propagate the values
        network.update()
        network.update()
        network.update()
        network.update()

        // Calculate the expected output manually
        // Note: When a network updates, inputs are transmitted to activations
        // and then the network update occurs which transfers those activations through connections
        val expectedOutput = doubleArrayOf(
            sourceActivations[0] * weights[0, 0] + sourceActivations[1] * weights[0, 1] + sourceActivations[2] * weights[0, 2],
            sourceActivations[0] * weights[1, 0] + sourceActivations[1] * weights[1, 1] + sourceActivations[2] * weights[1, 2]
        )
        
        // Expected: source[0]*1.0 + source[1]*0.5 + source[2]*0.0 = 1.0*1.0 + 0.5*0.5 + 0.2*0.0 = 1.0 + 0.25 = 1.25
        // Expected: source[0]*0.0 + source[1]*0.0 + source[2]*1.0 = 1.0*0.0 + 0.5*0.0 + 0.2*1.0 = 0.0 + 0.0 + 0.2 = 0.2
        
        // Verify the target array received the expected activations
        val targetActivations = redoTargetArray.activationArray
        assertArrayEquals(expectedOutput, targetActivations, 0.001, 
            "The restored weight matrix should correctly propagate activations")
    }

    @Test
    fun `test SupervisedModel copy paste`() = runBlocking {
        // Create two neuron arrays
        val sourceArray = NeuronArray(3).apply {
            label = "Source Array"
            location = point(100.0, 100.0)
        }
        val targetArray = NeuronArray(2).apply {
            label = "Target Array"
            location = point(100.0, 300.0)
        }
        
        // Add the arrays to the network
        network.addNetworkModel(sourceArray)
        network.addNetworkModel(targetArray)
        
        // Create a weight matrix connecting the arrays
        val weightMatrix = WeightMatrix(sourceArray, targetArray)
        
        // Set simple weight values
        val weights = weightMatrix.weights
        weights[0, 0] = 1.0
        weights[0, 1] = 0.5
        weights[0, 2] = 0.0
        weights[1, 0] = 0.0
        weights[1, 1] = 0.0
        weights[1, 2] = 1.0
        
        network.addNetworkModel(weightMatrix)
        
        // Create a supervised model using the arrays
        val supervisedModel = org.simbrain.network.trainers.SupervisedModel(sourceArray, targetArray)
        network.addNetworkModel(supervisedModel)
        
        // Add the supervised model to the clipboard
        Clipboard.add(listOf(supervisedModel))
        
        // Verify the clipboard is not empty
        assertFalse(Clipboard.isEmpty, "Clipboard should not be empty")
        
        // Get the network panel
        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel
        
        // Get initial counts
        val initialArrayCount = network.getModels<NeuronArray>().size
        val initialWeightMatrixCount = network.getModels<WeightMatrix>().size
        val initialSupervisedModelCount = network.getModels<org.simbrain.network.trainers.SupervisedModel>().size
        
        // Paste the clipboard contents
        Clipboard.paste(networkPanel)
        
        // Verify new items were created
        assertEquals(initialArrayCount + 2, network.getModels<NeuronArray>().size, 
            "Two new neuron arrays should be created after pasting")
        assertEquals(initialWeightMatrixCount + 1, network.getModels<WeightMatrix>().size, 
            "One new weight matrix should be created after pasting")
        assertEquals(initialSupervisedModelCount + 1, network.getModels<org.simbrain.network.trainers.SupervisedModel>().size, 
            "One new supervised model should be created after pasting")
        
        // Get the pasted supervised model
        val pastedSupervisedModel = network.getModels<org.simbrain.network.trainers.SupervisedModel>().last()
        
        // Verify the supervised model is not the same as the original
        assertNotEquals(supervisedModel.id, pastedSupervisedModel.id, 
            "The pasted supervised model should have a different ID than the original")
        
        // Verify the pasted supervised model has different layer IDs than the original
        assertNotEquals(sourceArray.id, pastedSupervisedModel.inputLayer.id, 
            "The pasted supervised model should have a different input layer than the original")
        assertNotEquals(targetArray.id, pastedSupervisedModel.outputLayer.id, 
            "The pasted supervised model should have a different output layer than the original")
        
        // Get the pasted arrays and weight matrix
        val pastedSourceArray = pastedSupervisedModel.inputLayer as NeuronArray
        val pastedTargetArray = pastedSupervisedModel.outputLayer as NeuronArray
        val pastedWeightMatrix = pastedSupervisedModel.weightMatrices.first() as WeightMatrix
        
        // Verify the arrays have the same dimensions
        assertEquals(sourceArray.size, pastedSourceArray.size, 
            "Pasted source array should have the same size")
        assertEquals(targetArray.size, pastedTargetArray.size, 
            "Pasted target array should have the same size")
        
        // Verify the weight matrix has the same dimensions
        assertEquals(weightMatrix.weights.nrow(), pastedWeightMatrix.weights.nrow(), 
            "Pasted weight matrix should have the same number of rows")
        assertEquals(weightMatrix.weights.ncol(), pastedWeightMatrix.weights.ncol(), 
            "Pasted weight matrix should have the same number of columns")
        
        // Verify the weight matrix has the same values
        for (row in 0 until weightMatrix.weights.nrow()) {
            for (col in 0 until weightMatrix.weights.ncol()) {
                assertEquals(weightMatrix.weights[row, col], pastedWeightMatrix.weights[row, col], 0.001, 
                    "Weight at position [$row, $col] should be copied correctly")
            }
        }
        
        // Test that the connectivity works by simulating input and output
        // Set activations in the pasted source array
        val sourceActivations = doubleArrayOf(1.0, 0.5, 0.2)
        pastedSourceArray.setActivations(sourceActivations)
        pastedSourceArray.isClamped = true
        
        // Update the network to propagate the values
        with(network) {
            update()
            update()
            update()
            update()
        }
        
        // Calculate the expected output manually
        val expectedOutput = doubleArrayOf(
            sourceActivations[0] * weights[0, 0] + sourceActivations[1] * weights[0, 1] + sourceActivations[2] * weights[0, 2],
            sourceActivations[0] * weights[1, 0] + sourceActivations[1] * weights[1, 1] + sourceActivations[2] * weights[1, 2]
        )
        
        // Expected: source[0]*1.0 + source[1]*0.5 + source[2]*0.0 = 1.0*1.0 + 0.5*0.5 + 0.2*0.0 = 1.0 + 0.25 = 1.25
        // Expected: source[0]*0.0 + source[1]*0.0 + source[2]*1.0 = 1.0*0.0 + 0.5*0.0 + 0.2*1.0 = 0.0 + 0.0 + 0.2 = 0.2
        
        // Verify the target array received the expected activations
        val targetActivations = pastedTargetArray.activationArray
        assertArrayEquals(expectedOutput, targetActivations, 0.001, 
            "Pasted supervised model should correctly propagate activations")
        
        // Test undo/redo functionality
        val pastedSupervisedModelId = pastedSupervisedModel.id
        val pastedSourceArrayId = pastedSourceArray.id
        val pastedTargetArrayId = pastedTargetArray.id
        val pastedWeightMatrixId = pastedWeightMatrix.id
        
        // Undo the paste action
        networkPanel.undoManager.undo()
        
        // Verify that all pasted items were removed
        assertEquals(initialArrayCount, network.getModels<NeuronArray>().size, 
            "All pasted neuron arrays should be removed after undo")
        assertEquals(initialWeightMatrixCount, network.getModels<WeightMatrix>().size, 
            "All pasted weight matrices should be removed after undo")
        assertEquals(initialSupervisedModelCount, network.getModels<org.simbrain.network.trainers.SupervisedModel>().size, 
            "All pasted supervised models should be removed after undo")
        
        // Check that pasted items are no longer in the network
        assertFalse(network.getModels<org.simbrain.network.trainers.SupervisedModel>().any { it.id == pastedSupervisedModelId },
            "The pasted supervised model should not be in the network after undo")
        assertFalse(network.getModels<NeuronArray>().any { it.id == pastedSourceArrayId },
            "The pasted source array should not be in the network after undo")
        assertFalse(network.getModels<NeuronArray>().any { it.id == pastedTargetArrayId },
            "The pasted target array should not be in the network after undo")
        assertFalse(network.getModels<WeightMatrix>().any { it.id == pastedWeightMatrixId },
            "The pasted weight matrix should not be in the network after undo")
        
        // Redo the paste action
        networkPanel.undoManager.redo()
        
        // Verify that all items were added back
        assertEquals(initialArrayCount + 2, network.getModels<NeuronArray>().size, 
            "All neuron arrays should be added back after redo")
        assertEquals(initialWeightMatrixCount + 1, network.getModels<WeightMatrix>().size, 
            "All weight matrices should be added back after redo")
        assertEquals(initialSupervisedModelCount + 1, network.getModels<org.simbrain.network.trainers.SupervisedModel>().size, 
            "All supervised models should be added back after redo")
        
        // Check that items with the same IDs are back in the network
        assertTrue(network.getModels<org.simbrain.network.trainers.SupervisedModel>().any { it.id == pastedSupervisedModelId },
            "A supervised model with the same ID should be in the network after redo")
        assertTrue(network.getModels<NeuronArray>().any { it.id == pastedSourceArrayId },
            "A source array with the same ID should be in the network after redo")
        assertTrue(network.getModels<NeuronArray>().any { it.id == pastedTargetArrayId },
            "A target array with the same ID should be in the network after redo")
        assertTrue(network.getModels<WeightMatrix>().any { it.id == pastedWeightMatrixId },
            "A weight matrix with the same ID should be in the network after redo")
    }

}
