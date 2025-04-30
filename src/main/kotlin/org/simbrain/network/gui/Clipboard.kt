/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
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

        // when copying neuron collections/neuron groups, we don't want to copy the neurons again
        val collectionNeurons = objects.filterIsInstance<AbstractNeuronCollection>().flatMap { it.neuronList }.toSet()
        val collectionSynapses = objects.filterIsInstance<SynapseGroup>().flatMap { it.synapses }.toSet()

        copiedObjects = objects
            .filter { (it as? Neuron) !in collectionNeurons }
            .filter { (it as? Synapse) !in collectionSynapses }

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

        fun createCopies(destinationNetwork: Network, sourceModels: List<NetworkModel>): List<NetworkModel> {

            // Match new to old neurons for synapse adding
            val neuronMappings = HashMap<Neuron, Neuron>()
            val synapses = ArrayList<Synapse>()

            fun Synapse.isStranded(): Boolean {
                val allNeurons = sourceModels.filterIsInstance<Neuron>()
                return !(allNeurons.contains(this.source) && (allNeurons.contains(this.target)))
            }

            val layerMappings = HashMap<Layer, Layer>()
            val weightMatrices = ArrayList<WeightMatrix>()
            val synapseGroups = ArrayList<SynapseGroup>()

            fun Connector.isStranded(): Boolean {
                val allLayer = sourceModels.filterIsInstance<Layer>()
                return !(allLayer.contains(this.source) && (allLayer.contains(this.target)))
            }

            fun SynapseGroup.isStranded(): Boolean {
                val allLayer = sourceModels.filterIsInstance<Layer>()
                return !(allLayer.contains(this.source) && (allLayer.contains(this.target)))
            }

            return buildList {
                for (item in sourceModels) {
                    when (item) {
                        is Neuron -> {
                            val newNeuron = Neuron(item)
                            add(newNeuron)
                            neuronMappings[item] = newNeuron
                        }
                        is Synapse -> {
                            if (!item.isStranded()) {
                                synapses.add(item)
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
                            val layers = item.layers.map { existing ->
                                existing.copy().also { copy ->
                                    layerMappings[existing] = copy
                                    if (copy is AbstractNeuronCollection) {
                                        neuronMappings.putAll((existing as AbstractNeuronCollection).neuronList.zip(copy.neuronList))
                                        (copy as? NeuronCollection)?.let { addAll(it.neuronList) }
                                    }
                                }
                            }
                            addAll(layers)
                            val weightMatrices = item.weightMatrices.map { existing ->
                                WeightMatrix(layerMappings[existing.source]!!, layerMappings[existing.target]!!).also { copy ->
                                   copy.copyFrom(existing as WeightMatrix)
                                }
                            }
                            addAll(weightMatrices)
                            val copy = SupervisedModel(layerMappings[item.inputLayer]!!, layerMappings[item.outputLayer]!!)
                            add(copy)
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
                                weightMatrices.add(item)
                            }
                        }
                        is SynapseGroup -> {
                            if (!item.isStranded()) {
                                synapseGroups.add(item)
                            }
                        }
                        is Subnetwork -> {
                            add(item.copy())
                        }
                    }
                }


                // Copy synapses
                for (synapse in synapses) {
                    val newSynapse = Synapse(
                        neuronMappings[synapse.source]!!,
                        neuronMappings[synapse.target]!!,
                        synapse
                    )
                    add(newSynapse)
                }

                for (connector in weightMatrices) {
                    val weightMatrix = WeightMatrix(
                        layerMappings[connector.source]!!,
                        layerMappings[connector.target]!!
                    )
                    weightMatrix.copyFrom(connector)
                    add(weightMatrix)
                }

                for (oldGroup in synapseGroups) {
                    val newGroup = SynapseGroup(
                        layerMappings[oldGroup.source] as AbstractNeuronCollection,
                        layerMappings[oldGroup.target] as AbstractNeuronCollection,
                        oldGroup.connectionStrategy.copy(),
                        oldGroup.synapses.map { Synapse(neuronMappings[it.source]!!, neuronMappings[it.target]!!, it) }.toMutableList()
                    )
                    add(newGroup)
                }
            }

        }

        // Create a copy of the clipboard objects.
        val copy = createCopies(net.network, copiedObjects)
        copy.filterIsInstance<LocatableModel>()
            .forEach { it.shouldBePlaced = false }

        // Add the copied object
        net.network.addNetworkModels(copy)

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