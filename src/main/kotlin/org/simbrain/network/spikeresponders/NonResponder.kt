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
import org.simbrain.network.core.Synapse
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder

/**
 * A "null" spike responder which produces "connectionist" dynamics where the post-synaptic response is the weight
 * times the source activation. See [Synapse.updateOutput] and [WeightMatrix.getOutput]
 */
class NonResponder : SpikeResponder() {

    override fun apply(conn: Connector, responderData: MatrixDataHolder) {
        // No implementation. The responder is bypassed.
    }

    override fun apply(s: Synapse, responderData: ScalarDataHolder) {
        // No implementation. The responder is bypassed.
    }

    override fun deepCopy(): SpikeResponder {
        return NonResponder()
    }

    override fun getDescription(): String {
        return "None (No spike response)"
    }

    override val name: String
        get() = "None"
}