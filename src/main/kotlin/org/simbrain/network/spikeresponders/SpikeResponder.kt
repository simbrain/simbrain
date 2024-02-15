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
package org.simbrain.network.spikeresponders

import org.simbrain.network.core.Connector
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.spikeresponders.*
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.propertyeditor.CopyableObject
import java.util.*

/**
 * **SpikeResponder** is a superclass for objects that respond to pre-synaptic spikes with a response that is sent
 * as input to the post-synaptic neuron.
 */
abstract class SpikeResponder : CopyableObject {
    override fun getTypeList(): List<Class<out CopyableObject?>>? {
        return responderList
    }

    /**
     * Defines a spike responder for scalar data.
     *
     * @param synapse           a reference to a parent synapse whose spikes we respond to. Contains reference to source
     * and target neuron, weight, strength, spike time, etc which can be used to defined the
     * response rule.
     * @param responderData data holder for spike responder
     */
    context(Network)
    abstract fun apply(synapse: Synapse, responderData: ScalarDataHolder)

    /**
     * Override to define a spike responder for matrix data.
     */
    context(Network)
    open fun apply(connector: Connector, responderData: MatrixDataHolder) {}

    /**
     * Override to return an appropriate data holder for a given responder.
     */
    open fun createResponderData(): ScalarDataHolder {
        return EmptyScalarData
    }

    /**
     * Override to return an appropriate data holder for a given responder.
     */
    open fun createMatrixData(rows: Int, cols: Int): MatrixDataHolder {
        return EmptyMatrixData
    }

    /**
     * @return Spike responder to duplicate.
     */
    abstract override fun copy(): SpikeResponder

    /**
     * @return the name of the spike responder
     */
    abstract val description: String?

    val type: String
        /**
         * @return the name of the class of this synapse
         */
        get() = this.javaClass.name.substring(this.javaClass.name.lastIndexOf('.') + 1)

}

/**
 * Spike responders for drop-down list used by
 * [org.simbrain.util.propertyeditor.ObjectTypeEditor]
 * to set the spike responder on a synapse.
 */
var responderList: List<Class<out CopyableObject?>> = listOf<Class<out CopyableObject?>>(
    NonResponder::class.java, JumpAndDecay::class.java,
    ConvolvedJumpAndDecay::class.java, ProbabilisticResponder::class.java,
    RiseAndDecay::class.java, StepResponder::class.java, UDF::class.java
)