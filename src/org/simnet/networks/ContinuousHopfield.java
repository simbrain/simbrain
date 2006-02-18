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

import org.simnet.interfaces.Neuron;
import org.simnet.layouts.Layout;
import org.simnet.neurons.AdditiveNeuron;


/**
 * <b>ContinuousHopfield</b>.
 */
public class ContinuousHopfield extends Hopfield {

    /** Number of neurons. */
    private int numUnits = 3;

    /**
     * Default constructor.
     */
    public ContinuousHopfield() {
        super();
    }

    /**
     * Creates a new continuous hopfield network.
     * @param numNeurons Number of neurons in network
     */
    public ContinuousHopfield(final int numNeurons, final Layout layout) {
        super();

        //Create the neurons
        for (int i = 0; i < numNeurons; i++) {
            AdditiveNeuron n = new AdditiveNeuron();
            addNeuron(n);
        }
        layout.layoutNeurons(this);
        this.createConnections();
    }

    /**
     * Update nodes using a buffer.
     */
    public void update() {
        for (int i = 0; i < getNeuronCount(); i++) {
            Neuron n = (Neuron) neuronList.get(i);
            n.update();
        }

        for (int i = 0; i < getNeuronCount(); i++) {
            Neuron n = (Neuron) neuronList.get(i);
            n.setActivation(n.getBuffer());
        }
    }

    /**
     * @return Number of neurons.
     */
    public int getNumUnits() {
        return numUnits;
    }
}
