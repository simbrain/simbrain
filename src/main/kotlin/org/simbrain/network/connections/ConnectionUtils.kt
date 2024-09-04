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

import org.simbrain.network.core.Synapse
import org.simbrain.util.SimbrainConstants.Polarity
import kotlin.random.Random

/**
 * Utility functions/interfaces/etc for manipulating synapses.
 * Usually, for manipulating loose synapses since most changes to Synapses in a
 * synapse group should be done through the synapse group, but there are
 * counter-examples.
 *
 * @author Zoë Tosi
 */

const val DEFAULT_EXCITATORY_STRENGTH = 1.0

const val DEFAULT_INHIBITORY_STRENGTH = -1.0

/**
 * Changes the strengths of the provided synapses so that [percentExcitatory] of them
 * are excitatory.
 *
 * This method will attempt to maintain the requested percentage even
 * if some or all of the source neurons are themselves polarized. In such
 * cases the polarity of the Neurons' outgoing synapses will not be
 * overridden. Though it may not be possible to obtain the desired
 * percentage in this case, this method will get as close as possible.
 *
 * @param synapses the synapses to polarize
 * @param percentExcitatory the percent of the synapses to make excitatory
 */
@JvmOverloads
fun polarizeSynapses(synapses: Collection<Synapse>, percentExcitatory: Double, random: Random = Random) {
    // Computations are done using ratios
    val excitatoryRatio = percentExcitatory / 100
    if (excitatoryRatio > 1 || excitatoryRatio < 0) {
        throw IllegalArgumentException("Randomization had failed." + " The ratio of excitatory synapses " + " cannot be greater than 1 or less than 0.")
    }
    val synapsesByPolarity = synapses.groupBy { it.source.polarity }
    val excitatory = synapsesByPolarity[Polarity.EXCITATORY] ?: emptyList()
    val inhibitory = synapsesByPolarity[Polarity.INHIBITORY] ?: emptyList()
    val both = synapsesByPolarity[Polarity.BOTH] ?: emptyList()

    if (both.isEmpty()) return // Skip if there are no synapses to polarize

    val excitatoryNeed = (synapses.size * excitatoryRatio).toInt() - excitatory.size
    val inhibitoryNeed = (synapses.size * (1 - excitatoryRatio)).toInt() - inhibitory.size

    if (excitatoryNeed < 0 || inhibitoryNeed < 0) {
        throw IllegalArgumentException("""
            Insufficient free synapses to meet the requested excitatory ratio.
            Existing excitatory synapses: ${excitatory.size}
            Existing inhibitory synapses: ${inhibitory.size}
            Existing both synapses: ${both.size}
            Requested excitatory size: $excitatoryNeed
            Requested inhibitory size: $inhibitoryNeed
        """.trimIndent())
    }

    val (toExcite, toInhibit) = both.shuffled(random).let {
        it.take(excitatoryNeed) + excitatory to it.drop(excitatoryNeed) + inhibitory
    }

    toExcite.forEach { it.strength = DEFAULT_EXCITATORY_STRENGTH }
    toInhibit.forEach { it.strength = DEFAULT_INHIBITORY_STRENGTH }

}
