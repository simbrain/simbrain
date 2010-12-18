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
package org.simbrain.network.neurons;

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.NeuronUpdateRule;


/**
 * <b>ClampedNeuron</b> is a simple neuron that does nothing! 
 */
public class ClampedNeuron implements NeuronUpdateRule {

    /**
     * TODO: Not really true...
     * @return time type.
     */
    public int getTimeType() {
        return org.simbrain.network.interfaces.RootNetwork.DISCRETE;
    }

//    /**
//     * Returns a duplicate ClampedNeuron (used, e.g., in copy/paste).
//     * @return Duplicated neuron
//     */
//    public ClampedNeuron duplicate() {
//        ClampedNeuron cn = new ClampedNeuron();
//        cn = (ClampedNeuron) super.duplicate(cn);
//
//        return cn;
//    }

    /**
     * Update neuron.
     */
    public void update(Neuron neuron) {
     }

    /**
     * @{inheritDoc}
     */
    public String getName() {
        return "Clamped";
    }

    /**
     * @{inheritDoc}
     */
    public void init(Neuron neuron) {
        // No implementation
    }
}
