package org.simbrain.network.gui

import org.simbrain.network.core.*
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.SupervisedModel

/**
 * Buffer which holds network objects for cutting and pasting.
 */
object Clipboard {
    // To add new copy-pastable items, must update:
    // 1) SimnetUtils.getCopy()
    // 2) Network.addObjects
    // 3) NetworkPanel.getSelectedModels()
    /**
     * Static list of cut or copied objects.
     */
    private var copiedObjects: List<NetworkModel> = ArrayList()

    /**
     * List of components which listen for changes to this clipboard.
     */
    private val listenerList = HashSet<ClipboardListener>()

    /**
     * Clear the clipboard.
     */
    fun clear() {
        copiedObjects = ArrayList()
        fireClipboardChanged()
    }

    /**
     * Add objects to the clipboard.  This happens with cut and copy.
     *
     * @param objects objects to add
     */
    fun add(objects: List<NetworkModel>) {

        // when copying models that are part of a collection, we don't want to copy those models again
        val collectionNeurons = objects.filterIsInstance<AbstractNeuronCollection>().flatMap { it.neuronList }.toMutableSet()
        val collectionSynapses = objects.filterIsInstance<SynapseGroup>().flatMap { it.synapses }.toMutableSet()
        val supervisedModelWeightMatrices = objects.filterIsInstance<SupervisedModel>().flatMap { it.weightMatrices }.toMutableSet()
        val supervisedModelSynapseGroups = objects.filterIsInstance<SupervisedModel>().flatMap { it.synapseGroups }.toMutableSet()
        val supervisedModelLayers = objects.filterIsInstance<SupervisedModel>().flatMap { it.layers }.toMutableSet()
        val supervisedModelNeurons = supervisedModelLayers.filterIsInstance<NeuronCollection>().flatMap { it.neuronList }.toMutableSet()
        val supervisedModelSynapses = supervisedModelSynapseGroups.flatMap { it.synapses }.toMutableSet()

        val collectionObjects = collectionNeurons + collectionSynapses +
                supervisedModelWeightMatrices + supervisedModelSynapseGroups +
                supervisedModelLayers + supervisedModelNeurons + supervisedModelSynapses

        copiedObjects = objects
            .filter { it !in collectionObjects }

        fireClipboardChanged()
    }

    /**
     * Paste objects into the netPanel.
     *
     * @param net the network to paste into
     */
    suspend fun paste(net: NetworkPanel) {
        if (isEmpty) {
            return
        }

        // Match new to old neurons for synapse adding
        val neuronMappings = LinkedHashMap<Neuron, Neuron>()
        // Match new to old layers for connector adding
        val layerMappings = LinkedHashMap<Layer, Layer>()

        fun createCopies(destinationNetwork: Network, sourceModels: List<NetworkModel>): List<NetworkModel> {

            fun Synapse.isStranded(): Boolean {
                val allNeurons = sourceModels.filterIsInstance<Neuron>().toMutableSet()
                // Also include neurons from collections that are being copied
                allNeurons.addAll(sourceModels.filterIsInstance<AbstractNeuronCollection>().flatMap { it.neuronList })
                return !(allNeurons.contains(this.source) && (allNeurons.contains(this.target)))
            }

            fun Connector.isStranded(): Boolean {
                val allLayer = sourceModels.filterIsInstance<Layer>()
                return !(allLayer.contains(this.source) && (allLayer.contains(this.target)))
            }

            fun SynapseGroup.isStranded(): Boolean {
                val allLayer = sourceModels.filterIsInstance<Layer>()
                return !(allLayer.contains(this.source) && (allLayer.contains(this.target)))
            }

            return buildList {
                for (item in sourceModels.sortedBy { updatingOrder(it) }) {
                    when (item) {
                        is Neuron -> {
                            val newNeuron = Neuron(item)
                            add(newNeuron)
                            neuronMappings[item] = newNeuron
                        }
                        is Synapse -> {
                            if (!item.isStranded()) {
                                val newSynapse = Synapse(
                                    neuronMappings[item.source]!!,
                                    neuronMappings[item.target]!!,
                                    item
                                )
                                add(newSynapse)
                            }
                        }
                        is NetworkTextObject -> {
                            val newText = NetworkTextObject(item)
                            add(newText)
                        }
                        is NeuronGroup -> {
                            val copy = item.copy()
                            neuronMappings.putAll(item.neuronList.zip(copy.neuronList))
                            add(copy)
                            layerMappings[item] = copy
                        }
                        is NeuronCollection -> {
                            val copy = item.copy()
                            neuronMappings.putAll(item.neuronList.zip(copy.neuronList))
                            add(copy)
                            addAll(copy.neuronList)
                            layerMappings[item] = copy
                        }
                        is SupervisedModel -> {
                            addAll(
                                createCopies(
                                    destinationNetwork,
                                    buildList {
                                        addAll(item.layers)
                                        addAll(item.weightMatrices)
                                        addAll(item.synapseGroups)
                                    }
                                )
                            )
                            add(SupervisedModel(layerMappings[item.inputLayer]!!, layerMappings[item.outputLayer]!!))
                        }
                        is NeuronArray -> {
                            val copy = item.copy()
                            layerMappings[item] = copy
                            add(copy)
                        }
                        is ActivationSequence -> {
                            val copy = item.copy()
                            layerMappings[item] = copy
                            add(copy)
                        }
                        is TransformerBlock -> {
                            val copy = item.copy()
                            layerMappings[item] = copy
                            add(copy)
                        }
                        is WeightMatrix -> {
                            if (!item.isStranded()) {
                                val weightMatrix = WeightMatrix(
                                    layerMappings[item.source]!!,
                                    layerMappings[item.target]!!
                                )
                                weightMatrix.copyFrom(item)
                                add(weightMatrix)
                            }
                        }
                        is SynapseGroup -> {
                            if (!item.isStranded()) {
                                val newGroup = SynapseGroup(
                                    layerMappings[item.source] as AbstractNeuronCollection,
                                    layerMappings[item.target] as AbstractNeuronCollection,
                                    item.connectionStrategy.copy(),
                                    item.synapses.map { Synapse(neuronMappings[it.source]!!, neuronMappings[it.target]!!, it) }.toMutableList()
                                )
                                add(newGroup)
                            }
                        }
                        is Subnetwork -> {
                            add(item.copy())
                        }
                    }
                }

            }

        }

        // Create a copy of the clipboard objects.
        val copy = createCopies(net.network, copiedObjects)
        copy.filterIsInstance<LocatableModel>()
            .forEach { it.shouldBePlaced = false }

        // Add the copied object
        net.network.addNetworkModelsAsync(copy)

        val undeleteContext = UndeleteContext(net, copy)

        var deletedModels: LinkedHashSet<NetworkModel>? = null

        net.undoManager.addUndoableAction(
            description = "Paste objects",
            undo = { deletedModels = LinkedHashSet( net.network.deleteModels(copy)) },
            redo = { with(net) { undeleteContext.restore(deletedModels!!.toList()) } }
        )

        // Unselect "old" copied objects
        net.selectionManager.clear()

        // Paste objects intelligently using placement manager
        net.network.placementManager.placeObjects(
            copy.filterIsInstance<LocatableModel>()
                .onEach { it.shouldBePlaced = true }
        )

        // Select copied objects after pasting them
        net.selectionManager.add(copy.map { net.modelNodeMap.get(it) })
    }

    @JvmStatic
    val isEmpty: Boolean
        /**
         * @return true if there's nothing in the clipboard, false otherwise
         */
        get() = copiedObjects.isEmpty()

    /**
     * Add the specified clipboard listener.
     *
     * @param l listener to add
     */
    @JvmStatic
    fun addClipboardListener(l: ClipboardListener) {
        listenerList.add(l)
    }

    /**
     * Fire a clipboard changed event to all registered model listeners.
     */
    fun fireClipboardChanged() {
        for (listener in listenerList) {
            listener.clipboardChanged()
        }
    }
}