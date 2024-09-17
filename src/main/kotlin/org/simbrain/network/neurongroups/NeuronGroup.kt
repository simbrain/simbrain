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
package org.simbrain.network.neurongroups

import org.simbrain.network.core.AbstractNeuronCollection
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.sortTopBottom
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.CustomTypeName
import java.util.*

/**
 * A group of neurons using a common [NeuronUpdateRule]. After creation the update rule may be changed but
 * neurons should not be added. Intermediate between a [NeuronCollection] which is just an
 * assemblage of potentially heterogeneous neurons that can be treated as a group, and a
 * [org.simbrain.network.matrix.NeuronArray] which is an array that can be updated using static update methods.
 *
 * A primary abstraction for larger network structures. Layers in feed-forward networks are neuron
 * groups. Self-organizing-maps subclass this class. Etc. Since all update rules are the same groups can be characterized
 * as spiking vs. non-spiking.
 */
open class NeuronGroup() : AbstractNeuronCollection() {

    constructor(neurons: List<Neuron>) : this() {
        addNeurons(neurons.sortTopBottom())
    }

    constructor(numNeurons: Int) : this(List(numNeurons) { Neuron() })

    fun setUpdateRule(base: NeuronUpdateRule<*, *>) {
        neuronList.forEach { it.updateRule = base.copy() }
    }

    override suspend fun delete() {
        neuronList.toList().forEach { it.delete() }
        super.delete()
    }

    context(Network)
    override fun update() {
        neuronList.forEach { it.accumulateInputs() }
        neuronList.forEach { it.update() }
        neuronList.forEach { it.clearInput() }
        super.update()
    }

    override fun clear() {
        super.clear()
        neuronList.forEach { it.clear() }
    }

    override fun copy() = NeuronGroup().also {
        it.addNeurons(neuronList.map(Neuron::copy))
        it.label = label
    }
}

@CustomTypeName("Bare Neuron Group")
class BasicNeuronGroupParams: NeuronGroupParams() {

    override fun create(): NeuronGroup {
        return NeuronGroup(List(numNeurons) { Neuron() })
    }

    override fun copy(): CopyableObject {
        return BasicNeuronGroupParams().also {
            commonCopy(it)
        }
    }
}