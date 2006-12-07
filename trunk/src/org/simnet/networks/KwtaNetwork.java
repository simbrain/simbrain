/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet.networks;

import org.simnet.interfaces.Network;
import org.simnet.layouts.Layout;
import org.simnet.neurons.ClampedNeuron;


/**
 * <b>KwtaNetwork</b> implements a k Winner Take All network.
 *
 */
public class KwtaNetwork extends Network {

    /** k Field. */
    private int k = 3;

    /**
     * Default connstructor.
     */
    public KwtaNetwork() {

    }


    /**
     * Initializes K Winner Take All network.
     */
    public void init() {
        super.init();
    }

    /**
     * Default connstructor.
     * @param layout for layout of Neurons.
     * @param k for the number of Neurons in the Kwta Network.
     */
    public KwtaNetwork(final int k, final Layout layout) {
        super();
        for (int i = 0; i < k; i++) {
            this.addNeuron(new ClampedNeuron());
        }
        layout.layoutNeurons(this);
    }

    /**
     * The core update function of the neural network.  Calls the current update function on each neuron, decays all
     * the neurons, and checks their bounds.
     */
    public void update() {
        updateAllNeurons();
        updateAllWeights();
    }


    /**
     * Returns the initial number of neurons.
     *
     * @return the initial number of neurons
     */
    public int getk() {
        return k;
    }
}
