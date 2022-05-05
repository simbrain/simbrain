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
package org.simbrain.network.connections

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution

/**
 * Maintains a specific strategy for creating connections between two groups of neurons. Subclasses correspond to
 * specific types of connection strategy. Methods for creating free synapses are generally distinct from those for
 * creating them in synapse groups. Another distinction is between strategies that use polarity and those that do not.
 *
 * Note that connections are generally made in the following order.
 * 1) Weights are made using this class
 * 2) Their excitatory / inhibitory ratio is set using [ConnectionUtilities.polarizeSynapses]
 * for free synapses, or [SynapseGroup.setExcitatoryRatio] in synapse groups
 * 3) The two sets of weights are then randomized using probability distributions.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
abstract class ConnectionStrategy : EditableObject {

    /**
     * Whether excitatory connection should be randomized.
     */
    var isUseExcitatoryRandomization = true

    /**
     * Whether inhibitory connection should be randomized.
     */
    var isUseInhibitoryRandomization = true

    /**
     * The normalized ratio of excitatory to inhibitory neurons.
     * A value between 0 (all inhibitory) and 1 (all excitatory).
     */
    var excitatoryRatio = .5

    /**
     * The randomizer for excitatory synapses.
     */
    var exRandomizer: ProbabilityDistribution = NormalDistribution();

    /**
     * The randomizer for inhibitory synapses.
     */
    var inRandomizer: ProbabilityDistribution = NormalDistribution();

    /**
     * Subclasses should set to true if the strategy itself produces inhibitory and excitatory weights and thus
     * overrides the need to explicitly set weight polarity (excitatory/inhibitory ratio).
     */
    open val overridesPolarity = false

    /**
     * Apply connection to a set of loose neurons.
     *
     * @param network parent network loose neuron
     * @param source  source neurons
     * @param target  target neurons
     * @return the resulting list of synapses, which are sometimes needed for
     * other operations
     */
    abstract fun connectNeurons(network: Network, source: List<Neuron>, target: List<Neuron>): List<Synapse>

    val stringDescription: String
        get() = "" + this.javaClass.simpleName

}