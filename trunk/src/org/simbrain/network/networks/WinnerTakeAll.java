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
package org.simbrain.network.networks;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neurons.LinearNeuron;


/**
 * <b>WinnerTakeAll</b>.The neuron with the highest weighted input in a
 * winner-take-all network takes on an upper value, all other neurons take on
 * the lower value. In case of a tie the node which wins is arbitrary (the first
 * in an internally maintained list).
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
    public WinnerTakeAll(final RootNetwork root, final int numNeurons, final Layout layout) {
        super();
        setRootNetwork(root);
        for (int i = 0; i < numNeurons; i++) {
            this.addNeuron(new Neuron(new LinearNeuron()));
        }
        layout.layoutNeurons(this);
    }

    /**
     * Update network.
     */
    public void update() {
        if (getRootNetwork().getClampNeurons()) {
            return;
        }

        updateAllNeurons();

        double max = 0;
        int winner = 0;

        for (int i = 0; i < getNeuronList().size(); i++) {
            Neuron n = (Neuron) getNeuronList().get(i);

            if (n.getActivation() > max) {
                max = n.getActivation();
                winner = i;
            }
        }

        for (int i = 0; i < getNeuronList().size(); i++) {
            if (i == winner) {
                ((Neuron) getNeuronList().get(i)).setActivation(winValue);
            } else {
                ((Neuron) getNeuronList().get(i)).setActivation(loseValue);
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

    @Override
    public Network duplicate() {
        WinnerTakeAll net = new WinnerTakeAll();
        net = (WinnerTakeAll) super.duplicate(net);
        return net;
    }
}
