/*
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
package org.simbrain.network.core

import org.simbrain.network.NetworkModel
import org.simbrain.network.layouts.Layout
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.util.propertyeditor.CopyableObject
import java.util.*
import java.util.function.Consumer

/**
 * A collection of free neurons (neurons in a [NeuronGroup] can be added to a collection). Allows them to be
 * labelled, moved around as a unit, coupled to, etc. However no special processing occurs in neuron collections. They
 * are a convenience. NeuronCollections can overlap each other in the sense of having neurons in common.
 */
class NeuronCollection : AbstractNeuronCollection {

    constructor(): super()

    constructor(neurons: List<Neuron>): super() {
        addNeurons(neurons.sortTopBottom())

        neurons.forEach { n: Neuron ->
            n.events.locationChanged.on { events.locationChanged }
            n.events.activationChanged.on(wait = true) { _, _ ->
                invalidateCachedActivations()
            }
            n.events.deleted.on(null, true, Consumer { toDelete: NetworkModel? ->
                removeNeuron(toDelete as Neuron?)
                events.locationChanged.fireAndForget()
                if (isEmpty) {
                    delete()
                }
            })
        }
    }

    /**
     * Translate all neurons (the only objects with position information).
     *
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    override fun offset(offsetX: Double, offsetY: Double) {
        for (neuron in neuronList) {
            neuron.offset(offsetX, offsetY, false)
        }
        events.locationChanged.fireAndForget()
    }

    /**
     * Call after deleting neuron collection from parent network.
     */
    override fun delete() {
        events.deleted.fireAndForget(this)
    }

    /**
     * Set the update rule for the neurons in this group.
     *
     * @param base the neuron update rule to set.
     */
    fun setNeuronType(base: NeuronUpdateRule<*, *>) {
        neuronList.forEach(Consumer { n: Neuron -> n.updateRule = base.copy() })
    }

    /**
     * Set the string update rule for the neurons in this group.
     *
     * @param rule the neuron update rule to set.
     */
    fun setNeuronType(rule: String) {
        try {
            val newRule =
                Class.forName("org.simbrain.network.neuron_update_rules.$rule").newInstance() as NeuronUpdateRule<*, *>
            setNeuronType(newRule)
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    val summedNeuronHash: Int
        /**
         * Returns the summed hash codes of contained neurons.  Used to prevent creation of neuron collections from
         * identical sets of neurons.
         *
         * @return summed hash
         */
        get() = neuronList.stream().mapToInt { obj: Neuron -> obj.hashCode() }.sum()

    context(Network)
    override fun shouldAdd(): Boolean {
        val hashCode = summedNeuronHash
        for (other in getModels(NeuronCollection::class.java)) {
            if (hashCode == other.summedNeuronHash) {
                return false
            }
        }
        return true
    }

    override fun clear() {
        for (n in neuronList) {
            n.clear()
        }
    }

    public override fun addNeuron(neuron: Neuron) {
        // These neurons already have ids and listeners
        neuronList.add(neuron)
        addListener(neuron)
    }

    override fun postOpenInit() {
        super.postOpenInit()
        neuronList.forEach { addListener(it) }
    }

    /**
     * Convenience method for applying a [Layout] to a neuron collection.
     */
    fun layout(layout: Layout) {
        layout.layoutNeurons(neuronList)
    }

    override fun copy(): CopyableObject {
        return NeuronCollection(neuronList)
    }
}
