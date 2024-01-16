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
package org.simbrain.network.groups

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.network.topLeftLocation
import java.awt.geom.Point2D

/**
 * A group of neurons using a common [NeuronUpdateRule]. After creation the update rule may be changed but
 * neurons should not be added. Intermediate between a [NeuronCollection] which is just an
 * assemblage of potentially heterogeneous neurons that can be treated as a group, and a
 * [org.simbrain.network.matrix.NeuronArray] which is an array that can be updated using static update methods.
 *
 *
 * A primary abstraction for larger network structures. Layers in feed-forward networks are neuron
 * groups. Self-organizing-maps subclass this class. Etc. Since all update rules are the same groups can be characterized
 * as spiking vs. non-spiking.
 */
open class NeuronGroup(net: Network?) : AbstractNeuronCollection(net) {

    constructor(net: Network?, neurons: List<Neuron?>) : this(net) {
        addNeurons(neurons)
    }

    constructor(net: Network?, numNeurons: Int) : this(net, List(numNeurons) { Neuron(net) })

    fun setUpdateRule(base: NeuronUpdateRule<*, *>) {
        neuronList.forEach { it.updateRule = base.copy() }
    }

    fun copyTo(newParent: Network): NeuronGroup {
        return NeuronGroup(newParent).also {
            it.addNeurons(neuronList.map(Neuron::deepCopy))
            it.label = label
        }
    }

    override fun delete() {
        super.delete()
        events.deleted.fireAndBlock(this)
        neuronList.forEach { it.delete() }
    }

    override fun update() {
        neuronList.forEach { it.updateInputs() }
        neuronList.forEach { it.clearInput() }
        super.update()
    }

    override fun clear() {
        super.clear()
        neuronList.forEach { it.clear() }
    }

    override fun copy(): NeuronGroup {
        return copyTo(parentNetwork)
    }

    override fun postOpenInit() {
        super.postOpenInit()
        getNeuronList().forEach { it.postOpenInit() }
        getNeuronList().forEach { addListener(it) }
    }

    override fun getTopLeftLocation(): Point2D.Double {
        return neuronList.topLeftLocation
    }
}
