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
fun polarizeSynapses(synapses: Collection<Synapse>, percentExcitatory: Double) {
    // Computations are done using ratios
    var excitatoryRatio = percentExcitatory / 100
    if (excitatoryRatio > 1 || excitatoryRatio < 0) {
        throw IllegalArgumentException("Randomization had failed." + " The ratio of excitatory synapses " + " cannot be greater than 1 or less than 0.")
    } else {
        var exciteCount = (excitatoryRatio * synapses.size).toInt()
        var inhibCount = synapses.size - exciteCount
        var remaining = synapses.size
        var excitatory = false
        for (s in synapses) {
            excitatory = shouldBeExcitatory(excitatoryRatio, exciteCount, inhibCount, s)
            // Set the strength based on the polarity.
            if (excitatory) {
                s.strength = DEFAULT_EXCITATORY_STRENGTH
                exciteCount--
                // Change the excitatoryRatio to maintain balance
                excitatoryRatio = exciteCount / remaining.toDouble()
            } else {
                s.strength = DEFAULT_INHIBITORY_STRENGTH
                inhibCount--
                // Change the excitatoryRatio to maintain balance.
                excitatoryRatio = (remaining - inhibCount) / remaining.toDouble()
            }
            remaining--
        }
    }
}

/**
 * Should the provided synapse be excitatory.
 */
private fun shouldBeExcitatory(excitatoryRatio: Double, exciteCount: Int, inhibCount: Int, s: Synapse): Boolean {
    var excitatory = false
    if (s.source.isPolarized) {
        excitatory = Polarity.EXCITATORY === s.source.polarity
    } else {
        if (exciteCount <= 0 || inhibCount <= 0) {
            if (exciteCount <= 0) {
                excitatory = false
            }
            if (inhibCount <= 0) {
                excitatory = true
            }
        } else {
            val exciteOrInhib = Math.random()
            excitatory = exciteOrInhib < excitatoryRatio
        }
    }
    return excitatory
}