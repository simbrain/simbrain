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
package org.simbrain.network.subnetworks

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.updaterules.PointNeuronRule

/**
 * **KwtaNetwork** implements a k Winner Take All network. The k neurons
 * receiving the most excitatory input will become active. The network
 * determines what level of inhibition across all network neurons will result in
 * those k neurons being active about threshold. From O'Reilley and Munakata,
 * Computational Explorations in Cognitive Neuroscience, p. 110. All page
 * references below are are to this book.
 *
 *
 * TODO: This has been temporarilty disabled. When re-enabled, it's name should
 * reflect its' connection to the Leabra framework, since generic kwta is
 * possible and is slated to be implemented in a regular WTA network.
 */
class KWTA(k: Int) : NeuronGroup() {
    // TODO: Make q settable
    // Add average based version
    /**
     * k, that is, number of neurons to win a competition.
     */
    private var k = 1

    /**
     * Determines the relative contribution of the k and k+1 node to the
     * threshold conductance.
     */
    private val q = 0.25

    /**
     * Current inhibitory conductance to be applied to all neurons in the
     * subnetwork.
     */
    private var inhibitoryConductance = 0.0

    /**
     * Default constructor.
     *
     * @param k for the number of Neurons in the Kwta Network.
     */
    init {
        for (i in 0 until k) {
            val neuron = Neuron()
            neuron.updateRule = PointNeuronRule()
            addNeuron(neuron)
        }
        label = "K-Winner Take All"
    }

    context(Network)
    override fun update() {
        sortNeurons()
        super.update()
    }

    /**
     * See p. 101, equation 3.3. TODO: Unused, use or delete
     */
    private fun setCurrentThresholdCurrent() {
        val highest = (neuronList[k].updateRule as PointNeuronRule).inhibitoryThresholdConductance
        val secondHighest = (neuronList[k - 1].updateRule as PointNeuronRule).inhibitoryThresholdConductance

        inhibitoryConductance = secondHighest + q * (highest - secondHighest)

        // System.out.println("highest " + highest + "  secondHighest "
        // + secondHighest + " inhibitoryCondctance" + inhibitoryConductance);

        // Set inhibitory conductances in the layer
        for (neuron in neuronList) {
            (neuron.updateRule as PointNeuronRule).inhibitoryConductance = inhibitoryConductance
        }
    }

    /**
     * Sort neurons by their excitatory conductance. See p. 101.
     */
    private fun sortNeurons() {
        // REDO
        // Collections.sort(this.getNeuronList(), new PointNeuronComparator());
    }

    /**
     * Used to sort PointNeurons by excitatory conductance.
     */
    internal inner class PointNeuronComparator : Comparator<Neuron> {
        /**
         * {@inheritDoc}
         */
        override fun compare(neuron1: Neuron, neuron2: Neuron): Int {
            return (neuron1.updateRule as PointNeuronRule).excitatoryConductance.toInt() - (neuron1.updateRule as PointNeuronRule).excitatoryConductance.toInt()
        }
    }

    /**
     * Returns the initial number of neurons.
     *
     * @return the initial number of neurons
     */
    fun getK(): Int {
        return k
    }

    /**
     * @param k The k to set.
     */
    fun setK(k: Int) {
        if (k < 1) {
            this.k = 1
        } else if (k >= neuronList.size) {
            this.k = neuronList.size - 1
        } else {
            this.k = k
        }
    }
}
