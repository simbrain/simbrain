/*
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
package org.simbrain.network.groups;

import java.util.List;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;

/**
 * Extend neuron group with layer information used in a layered network.
 *
 * @author jyoshimi
 */
public class NeuronLayer extends NeuronGroup {

    /** Enumeration of layer types. */
    public enum LayerType {
        Input, Hidden, Output, Context, Reservoir
    }

    /** The type of this layer: input, hidden, or outout. */
    private LayerType type;

    /**
     * Construct a neuron layer.
     *
     * @param net parent network
     * @param neurons set of neurons
     * @param type the type of this layer
     */
    public NeuronLayer(Network net, List<Neuron> neurons, LayerType type) {
        super(net, neurons);
        this.type = type;
        setLabel(type.name() + " Layer");
    }

    /**
     * @return the type
     */
    public LayerType getType() {
        return type;
    }

}
