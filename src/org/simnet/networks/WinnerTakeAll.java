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
import org.simnet.layouts.Layout;
import org.simnet.neurons.LinearNeuron;


/**
 * <b>WinnerTakeAll</b>.
 */
public class WinnerTakeAll extends Network {
    /** Number of neurons. */
    private int numUnits = 3;
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
     *
     * @param numNeurons Number of neurons in new network
     * @param layout the way to layout the network
     */
    public WinnerTakeAll(final int numNeurons, final Layout layout) {
        super();
        for (int i = 0; i < numNeurons; i++) {
            this.addNeuron(new LinearNeuron());
        }
        layout.layoutNeurons(this);
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

    /**
     * @return Returns the loseValue.
     */
    public double getLoseValue() {
        return loseValue;
    }

    /**
     * @param loseValue The loseValue to set.
     */
    public void setLoseValue(final double loseValue) {
        this.loseValue = loseValue;
    }

    /**
     * @return Returns the winValue.
     */
    public double getWinValue() {
        return winValue;
    }

    /**
     * @param winValue The winValue to set.
     */
    public void setWinValue(final double winValue) {
        this.winValue = winValue;
    }

    /**
     * @return Number of neurons.
     */
    public int getNumUnits() {
        return numUnits;
    }
}
