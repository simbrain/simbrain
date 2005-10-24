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

import org.simnet.neurons.BinaryNeuron;


/**
 * <b>DiscreteHopfield</b>
 */
public class DiscreteHopfield extends Hopfield {
    public static final int RANDOM_UPDATE = 0;
    public static final int SEQUENTIAL_UPDATE = 1;
    private int update_order = SEQUENTIAL_UPDATE;

    public DiscreteHopfield() {
        super();
    }

    public DiscreteHopfield(int numNeurons) {
        super();

        //Create the neurons
        for (int i = 0; i < numNeurons; i++) {
            BinaryNeuron n = new BinaryNeuron();
            n.setUpperBound(1);
            n.setLowerBound(-1);
            n.setThreshold(0);
            n.setIncrement(1);
            addNeuron(n);
        }

        this.createConnections();
    }

    /**
     * Update nodes randomly or sequentially
     */
    public void update() {
        int n_count = getNeuronCount();
        int j;
        Neuron n;

        for (int i = 0; i < n_count; i++) {
            j = (int) (Math.random() * n_count);

            if (update_order == RANDOM_UPDATE) {
                n = (Neuron) neuronList.get(j);
            } else {
                n = (Neuron) neuronList.get(i);
            }

            n.update();
            n.setActivation(n.getBuffer());
        }
    }
}
