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
import org.simnet.interfaces.Neuron;
import org.simnet.neurons.LinearNeuron;


/**
 * <b>WinnerTakeAll</b>
 */
public class WinnerTakeAll extends Network {
    /** Winning value. */
    private double winValue = 1;
    /** Losing value. */
    private double loseValue = 0;

    /**
     * Default constructor.
     */
    public WinnerTakeAll() {
        super();
    }

    /**
     * Creates a new winner take all network.
     * @param numNeurons Number of neurons in new network
     */
    public WinnerTakeAll(final int numNeurons) {
        super();

        for (int i = 0; i < numNeurons; i++) {
            this.addNeuron(new LinearNeuron());
        }
    }

    /**
     * Update network.
     */
    public void update() {
        updateAllNeurons();

        double max = 0;
        int winner = 0;

        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = (Neuron) neuronList.get(i);

            if (n.getActivation() > max) {
                max = n.getActivation();
                winner = i;
            }
        }

        for (int i = 0; i < neuronList.size(); i++) {
            if (i == winner) {
                ((Neuron) neuronList.get(i)).setActivation(winValue);
            } else {
                ((Neuron) neuronList.get(i)).setActivation(loseValue);
            }
        }
    }
}
