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
package org.simbrain.network.core

import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.network.util.SpikingScalarData

/**
 * **SpikingNeuron** is the superclass for spiking neuron types (e.g.
 * integrate and fire) with functions common to spiking neurons. For example a
 * boolean hasSpiked field is used in the gui to indicate that this neuron has
 * spiked.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
abstract class SpikingNeuronUpdateRule<DS : SpikingScalarData, DM : SpikingMatrixData> : NeuronUpdateRule<DS, DM>() {
    override fun clear(neuron: Neuron) {
        super.clear(neuron)
        neuron.fanIn.forEach { it.clear() }
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.CONTINUOUS

    /**
     * A helper method which identifies this and all subclasses as variations of
     * spiking neurons. While instanceof is often bad practice this is a faster
     * way of determining if a neuron is spiking without using instanceof.
     * While normally this would still be bad practice, this is often used by
     * GUI components which are separate from the logical code.
     *
     * @return TRUE: Any subclass of SpikingNeuronUpdate rule, must by
     * definition be a spiking neuron.
     */
    override val isSpikingRule: Boolean = true

    /**
     * Override to provide subclasses of SpikingMatrixData if needed.
     */
    override fun createMatrixData(size: Int): DM {
        return SpikingMatrixData(size) as DM
    }

    /**
     * Override to provide subclasses of SpikingScalarData if needed.
     */
    override fun createScalarData(): DS {
        return SpikingScalarData() as DS
    }
}
