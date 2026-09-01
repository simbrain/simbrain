package org.simbrain.network.gui

import org.simbrain.network.core.*
import org.simbrain.network.llm.LanguageModel
import org.simbrain.network.llm.TinyLanguageModel
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.SupervisedModel

/**
 * Buffer which holds network objects for cutting and pasting. To make a new model type
 * copy-pastable, add it to [canCopy] and give it a branch in the `createCopies` switch inside
 * [paste] — [add] admits only [canCopy] types, so a type missing its branch fails visibly
 * (it never enters the clipboard) instead of silently vanishing at paste time.
 */
object Clipboard {
    /**
     * Static list of cut or copied objects.
     */
    private var copiedObjects: List<NetworkModel> = ArrayList()

    /** The model types [paste]'s `createCopies` knows how to copy. */
    fun canCopy(model: NetworkModel): Boolean = when (model) {
        is Neuron, is Synapse, is GapJunction, is NetworkTextObject, is NeuronCollection, is SupervisedModel,
        is NeuronArray, is ActivationSequence, is WeightMatrix, is SynapseGroup, is Subnetwork,
        is LanguageModel, is TinyLanguageModel -> true
        else -> false
    }

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
        val collectionNeurons = objects.filterIsInstance<NeuronCollection>().flatMap { it.neuronList }.toMutableSet()
        val collectionSynapses = objects.filterIsInstance<SynapseGroup>().flatMap { it.synapses }.toMutableSet()
        val supervisedModelWeightMatrices = objects.filterIsInstance<SupervisedModel>().flatMap { it.weightMatrices }.toMutableSet()
        val supervisedModelSynapseGroups = objects.filterIsInstance<SupervisedModel>().flatMap { it.synapseGroups }.toMutableSet()
        val supervisedModelLayers = objects.filterIsInstance<SupervisedModel>().flatMap { it.layers }.toMutableSet()
        val supervisedModelNeurons = supervisedModelLayers.filterIsInstance<NeuronCollection>().flatMap { it.neuronList }.toMutableSet()
        val supervisedModelSynapses = supervisedModelSynapseGroups.flatMap { it.synapses }.toMutableSet()

        // a subnetwork copies its own children, so don't copy them again as free models
        val subnetworkChildren = objects.filterIsInstance<Subnetwork>().flatMap { it.modelList.all }.toMutableSet()
        val subnetworkNeurons = subnetworkChildren.filterIsInstance<NeuronCollection>().flatMap { it.neuronList }.toMutableSet()
        val subnetworkSynapses = subnetworkChildren.filterIsInstance<SynapseGroup>().flatMap { it.synapses }.toMutableSet()

        val collectionObjects = collectionNeurons + collectionSynapses +
                supervisedModelWeightMatrices + supervisedModelSynapseGroups +
                supervisedModelLayers + supervisedModelNeurons + supervisedModelSynapses +
                subnetworkChildren + subnetworkNeurons + subnetworkSynapses

        // InfoText (e.g. a subnetwork's energy readout) is owned by its parent model and is
        // recreated when that parent is copied. It must never be copied as a standalone free
        // text object, or duplicating leaves a stray overlapping readout near the original.
        copiedObjects = objects
            .filter { it !in collectionObjects && it !is InfoText && canCopy(it) }

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
                allNeurons.addAll(sourceModels.filterIsInstance<NeuronCollection>().flatMap { it.neuronList })
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
                        is GapJunction -> {
                            val endpoint1 = neuronMappings[item.neuron1]
                            val endpoint2 = neuronMappings[item.neuron2]
                            if (endpoint1 != null && endpoint2 != null) {
                                add(GapJunction(endpoint1, endpoint2, item))
                            }
                        }
                        is InfoText -> {
                            // customInfo is recreated by its owning model's copy(); never copy it as free text
                        }
                        is NetworkTextObject -> {
                            val newText = NetworkTextObject(item)
                            add(newText)
                        }
                        is NeuronCollection -> {
                            val copy = item.copy()
                            neuronMappings.putAll(item.neuronList.zip(copy.neuronList))
                            addAll(copy.neuronList) // Add neurons as free models first
                            add(copy) // Then add the collection wrapper
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
                                    layerMappings[item.source] as NeuronCollection,
                                    layerMappings[item.target] as NeuronCollection,
                                    item.connectionStrategy.copy(),
                                    item.synapses.map { Synapse(neuronMappings[it.source]!!, neuronMappings[it.target]!!, it) }.toMutableList()
                                )
                                add(newGroup)
                            }
                        }
                        is Subnetwork -> {
                            add(item.copy())
                        }
                        is LanguageModel -> {
                            add(item.copy())
                        }
                        is TinyLanguageModel -> {
                            add(item.copy())
                        }
                        else -> error(
                            "No copy branch for ${item::class.simpleName}; " +
                                "Clipboard.add should not have admitted it"
                        )
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