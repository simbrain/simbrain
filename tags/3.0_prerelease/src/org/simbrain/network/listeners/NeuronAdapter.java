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
package org.simbrain.network.listeners;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;

/**
 * Adapter class for NeuronListener.
 *
 * @author Jeff Yoshimi
 */
public class NeuronAdapter implements NeuronListener {

    @Override
    public void neuronChanged(NetworkEvent<Neuron> networkEvent) {
    }

    @Override
    public void neuronTypeChanged(NetworkEvent<NeuronUpdateRule> networkEvent) {
    }

    @Override
    public void labelChanged(NetworkEvent<Neuron> networkEvent) {
    }

    @Override
    public void neuronAdded(NetworkEvent<Neuron> networkEvent) {
    }

    @Override
    public void neuronMoved(NetworkEvent<Neuron> networkEvent) {
    }

    @Override
    public void neuronRemoved(NetworkEvent<Neuron> networkEvent) {
    }

}
