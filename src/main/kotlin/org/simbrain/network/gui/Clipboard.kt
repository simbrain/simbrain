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

import kotlinx.coroutines.awaitAll
import org.simbrain.network.core.*
import org.simbrain.network.neurongroups.NeuronGroup
import java.util.*

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
        copiedObjects = objects.filter { (it as? Neuron) !in collectionNeurons }
        // System.out.println("add-->"+ Arrays.asList(objects));
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

        fun createCopies(destinationNetwork: Network, sourceModels: List<NetworkModel>): MutableList<NetworkModel> {
            val ret: MutableList<NetworkModel> = ArrayList()

            // Match new to old neurons for synapse adding
            val neuronMappings = Hashtable<Neuron, Neuron>()
            val synapses = ArrayList<Synapse>()

            fun Synapse.isStranded(): Boolean {
                val allNeurons = sourceModels.filterIsInstance<Neuron>()
                return !(allNeurons.contains(this.source) && (allNeurons.contains(this.target)))
            }

            for (item in sourceModels) {
                when (item) {
                    is Neuron -> {
                        val newNeuron = Neuron(item)
                        ret.add(newNeuron)
                        neuronMappings[item] = newNeuron
                    }
                    is Synapse -> {
                        if (!item.isStranded()) {
                            synapses.add(item)
                        }
                    }
                    is NetworkTextObject -> {
                        val newText = NetworkTextObject(item)
                        ret.add(newText)
                    }
                    is NeuronGroup -> {
                        ret.add(item.copy())
                    }
                    is NeuronArray -> {
                        val copy: LocatableModel = item.copy()
                        ret.add(copy)
                    }
                    is ActivationStack -> {
                        val copy: LocatableModel = item.copy()
                        ret.add(copy)
                    }
                    is TransformerBlock -> {
                        val copy: LocatableModel = item.copy()
                        ret.add(copy)
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
                ret.add(newSynapse)
            }

            return ret
        }

        // Create a copy of the clipboard objects.
        val copy = createCopies(net.network, copiedObjects)
        copy.filterIsInstance<LocatableModel>()
            .forEach { it.shouldBePlaced = false }

        // Add the copied object
        net.network.addNetworkModels(copy).awaitAll()

        // Unselect "old" copied objects
        net.selectionManager.clear()

        // Paste objects intelligently using placement manager
        net.network.placementManager.placeObjects(
            copy.filterIsInstance<LocatableModel>()
                .onEach { it.shouldBePlaced = true }
        )

        // Select copied objects after pasting them
        copy.forEach { it.select() }
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