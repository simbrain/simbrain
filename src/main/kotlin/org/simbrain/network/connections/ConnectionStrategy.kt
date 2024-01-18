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
import org.simbrain.network.gui.ConnectionStrategyPanel
import org.simbrain.util.displayInDialog
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution

/**
 * Maintains a specific strategy for creating connections between two groups of neurons. Subclasses correspond to
 * specific types of connection strategy. Methods for creating free synapses are generally distinct from those for
 * creating them in synapse groups. Another distinction is between strategies that use polarity and those that do not.
 *
 * Note that connections are generally made in the following order.
 * 1) Weights are made using this class
 * 2) Their excitatory / inhibitory ratio is set using [percentExcitatory]
 * 3) The two sets of weights are then randomized using probability distributions.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
abstract class ConnectionStrategy : CopyableObject {

    /**
     * Whether excitatory connection should be randomized.
     */
    var isUseExcitatoryRandomization = true

    /**
     * Whether inhibitory connection should be randomized.
     */
    var isUseInhibitoryRandomization = true

    /**
     * The randomizer for excitatory synapses.
     */
    var exRandomizer: ProbabilityDistribution = NormalDistribution();

    /**
     * The randomizer for inhibitory synapses.
     */
    var inRandomizer: ProbabilityDistribution = NormalDistribution();

    /**
     * If true, then separately store [percentExcitatory]. If false, the connection strategy itself determines how
     * many excitatory vs. inhibitory weights there are.
     */
    open val usesPolarity = true

    /**
     * If uses polarity, store the percent excitatory. Otherwise ignore.
     */
    var percentExcitatory: Double = 50.0

    fun commonCopy(toCopy: ConnectionStrategy) {
        toCopy.exRandomizer = exRandomizer.copy()
        toCopy.inRandomizer = inRandomizer.copy()
        toCopy.isUseExcitatoryRandomization = isUseExcitatoryRandomization
        toCopy.isUseInhibitoryRandomization = isUseInhibitoryRandomization
        toCopy.percentExcitatory = percentExcitatory
    }

    abstract override fun copy(): ConnectionStrategy

    /**
     * Apply connection to a set of loose neurons.
     *
     * @param network parent network loose neuron
     * @param source  source neurons
     * @param target  target neurons
     * @param addToNetwork if true, add the synapses to the network
     * @return the resulting list of synapses, which are sometimes needed for
     * other operations
     */
    abstract fun connectNeurons(
        network: Network,
        source: List<Neuron>,
        target: List<Neuron>,
        addToNetwork: Boolean = true
    ): List<Synapse>

    override fun getTypeList() = connectionTypes

}

val connectionTypes = listOf(
    AllToAll::class.java,
    DistanceBased::class.java,
    OneToOne::class.java,
    FixedDegree::class.java,
    RadialGaussian::class.java,
    RadialProbabilistic::class.java,
    Sparse::class.java
)

fun main() {
    ConnectionStrategyPanel(Sparse()).displayInDialog()
}
