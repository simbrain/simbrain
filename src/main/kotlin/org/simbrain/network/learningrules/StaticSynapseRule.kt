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
package org.simbrain.network.learningrules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData

/**
 * **ClampedSynapse**.
 */
open class StaticSynapseRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>() {

    var isClipped: Boolean = false

    override fun init(synapse: Synapse) {
        // TODO Auto-generated method stub
    }

    override fun copy(): SynapseUpdateRule<*, *> {
        val cs = StaticSynapseRule()
        return cs
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        // if (clipped) {
        // super.setStrength(Synapse(synapse.getStrength()));
        // }
    }

    override val name: String
        get() = "Static"
}
