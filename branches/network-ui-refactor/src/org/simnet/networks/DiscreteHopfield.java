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
    public static final int randomUpdate = 0;
    public static final int sequentialUpdate = 1;
    private int updateOrder = sequentialUpdate;

    /**
     * Default constructor.
     */
    public DiscreteHopfield() {
        super();
    }

    /**
     * Creates a new descrete hopfield network.
     * @param numNeurons Number of neurons in new network
     */
    public DiscreteHopfield(final int numNeurons) {
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
     * Update nodes randomly or sequentially.
     */
    public void update() {
        int nCount = getNeuronCount();
        int j;
        Neuron n;

        for (int i = 0; i < nCount; i++) {
            j = (int) (Math.random() * nCount);

            if (updateOrder == randomUpdate) {
                n = (Neuron) neuronList.get(j);
            } else {
                n = (Neuron) neuronList.get(i);
            }

            n.update();
            n.setActivation(n.getBuffer());
        }
    }
}
