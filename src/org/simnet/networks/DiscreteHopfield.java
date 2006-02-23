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

import java.util.Collections;

import org.simnet.interfaces.Neuron;
import org.simnet.layouts.Layout;
import org.simnet.neurons.BinaryNeuron;


/**
 * <b>DiscreteHopfield</b>.
 */
public class DiscreteHopfield extends Hopfield {

    /** Random update. */
    public static final int RANDOM_UPDATE = 1;

    /** Sequential update. */
    public static final int SEQUENTIAL_UPDATE = 0;

    /** Update order. */
    private int updateOrder = SEQUENTIAL_UPDATE;

    /** Number of neurons. */
    private int numUnits = 9;

    /**
     * Default constructor.
     */
    public DiscreteHopfield() {
        super();
    }

    /**
     * Creates a new descrete hopfield network.
     * @param numNeurons Number of neurons in new network
     * @param layout Neuron layout patern
     */
    public DiscreteHopfield(final int numNeurons, final Layout layout) {
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
        layout.layoutNeurons(this);
        this.createConnections();
    }

    /**
     * Update nodes randomly or sequentially.
     */
    public void update() {
        int nCount = getNeuronCount();
        Neuron n;

        if (updateOrder == RANDOM_UPDATE) {
            Collections.shuffle(neuronList);
        }

        for (int i = 0; i < nCount; i++) {
            n = (Neuron) neuronList.get(i);
            n.update();
            n.setActivation(n.getBuffer());
        }
    }

    /**
     * @return The number of neurons.
     */
    public int getNumUnits() {
        return numUnits;
    }

    /**
     * @return The update order.
     */
    public int getUpdateOrder() {
        return updateOrder;
    }

    /**
     * Sets the update order.
     *
     * @param updateOrder The value to set
     */
    public void setUpdateOrder(final int updateOrder) {
        this.updateOrder = updateOrder;
    }
}
