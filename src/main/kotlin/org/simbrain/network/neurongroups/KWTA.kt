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
package org.simbrain.network.neurongroups

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.updaterules.PointNeuronRule
import org.simbrain.util.propertyeditor.CustomTypeName
import org.simbrain.util.propertyeditor.GuiEditable

/**
 * A k Winner Take All network. The k neurons receiving the most excitatory input will become active. The network
 * determines what level of inhibition across all network neurons will result in
 * those k neurons being active about threshold.
 *
 * From O'Reilly and Munakata, Computational Explorations in Cognitive Neuroscience, p. 110.
 * All page references below are  to this book.
 */
class KWTA @JvmOverloads constructor(
    neurons: List<Neuron>,
    params: KWTAParams = KWTAParams()
) : NeuronGroup() {

    var params by GuiEditable(
        label = "KWTA Parameters",
        description = "Parameters for the K Winner-Take-All Group",
        initValue = params.apply { creationMode = false },
        order = 50
    )

    constructor(network: Network, numNeurons: Int) : this(List(numNeurons) { Neuron(PointNeuronRule()) })

    @XStreamConstructor
    private constructor(): this(listOf())

    init {
        label = "K-Winner Take All"
        neurons.filterNot { it.updateRule is PointNeuronRule }.forEach { it.updateRule = PointNeuronRule() }
        addNeurons(neurons)
    }

    override fun copy() = KWTA(neuronList.map { it.copy() }, params.copy())

    context(Network)
    override fun update() {
        // TODO Implement rule
        println("KWTA Update not yet implemented")
        // sortNeurons()
        super.update()
    }

    // /**
    //  * Sort neurons by their excitatory conductance. See p. 101.
    //  */
    // private fun sortNeurons() {
    //     Collections.sort(neuronList, PointNeuronComparator());
    // }
    //
    // class PointNeuronComparator : Comparator<Neuron> {
    //     override fun compare(neuron1: Neuron, neuron2: Neuron): Int {
    //         return (neuron1.updateRule as PointNeuronRule).excitatoryConductance.toInt() - (neuron1.updateRule as PointNeuronRule).excitatoryConductance.toInt()
    //     }
    // }

}

@CustomTypeName("KWTA")
class KWTAParams() : NeuronGroupParams() {

    var k by GuiEditable(
        label = "K",
        description = "Number of nodes that should win a competition",
        min = 0,
        initValue = 1,
        order = 10,
    )

    var q by GuiEditable(
        label = "q",
        description = "Determines the relative contribution of the k and k+1 node to the threshold conductance.",
        min = 0.0,
        initValue = 0.25,
        order = 20,
    )

    var inhibitoryConductance by GuiEditable(
        label = "q",
        description = " Current inhibitory conductance to be applied to all neurons in the subnetwork.",
        min = 0.0,
        initValue = 0.0,
        order = 30,
    )

    override fun create(): KWTA {
        return KWTA(List(numNeurons) { Neuron() }, this)
    }

    override fun copy(): KWTAParams {
        return KWTAParams().also {
            commonCopy(it)
            it.k = k
            it.q = q
            it.inhibitoryConductance = inhibitoryConductance
        }
    }
}
