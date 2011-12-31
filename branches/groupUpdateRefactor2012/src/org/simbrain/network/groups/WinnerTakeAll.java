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
package org.simbrain.network.groups;

import java.util.Random;

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
public class WinnerTakeAll extends NeuronGroup implements UpdatableGroup {

    /** Default initial number of units. */
    private static final int DEFAULT_NUM_UNITS = 5;

    /** Number of neurons. */
    private int numUnits = DEFAULT_NUM_UNITS;

    /** Winning value. */
    private double winValue = 1;

    /** Losing value. */
    private double loseValue = 0;

    /** If true, sometimes set the winner randomly. */
    private boolean useRandom;

    /** Probability of setting the winner randomly, when useRandom is true. */
    private double randomProb = .1;


    /**
     * Copy constructor.
     *
     * @param newRoot new root net
     * @param oldNet old network
     */
    public WinnerTakeAll(RootNetwork newRoot,  WinnerTakeAll oldNet) {
        super(null,null);
        setLoseValue(oldNet.getLoseValue());
        setWinValue(oldNet.getWinValue());
        setUseRandom(oldNet.isUseRandom());
        setRandomProb(oldNet.getRandomProb());
    }

    /**
     * Creates a new winner take all network.
     *
     * @param numNeurons Number of neurons in new network
     * @param layout the way to layout the network
     */
    public WinnerTakeAll(final RootNetwork root, final int numNeurons, final Layout layout) {
        super(root);
        for (int i = 0; i < numNeurons; i++) {
          //TODO: Prevent invalid states like this?
          this.addNeuron(new Neuron(root, new LinearNeuron()));
        }
        layout.layoutNeurons(this.getNeuronList());
    }

    /**
     * {@inheritDoc}
     */
    public void update() {
        
        if (getParentNetwork().getClampNeurons()) {
            return;
        }

        // Determine the winning neuron
        int winnerIndex;
        if (useRandom) {
            if (Math.random() < randomProb) {
                winnerIndex = getRandomWinnerIndex();
            } else {
                winnerIndex = getWinningIndex();
            }
        } else {
            winnerIndex = getWinningIndex();
        }

        // Set neuron values
        for (int i = 0; i < getNeuronList().size(); i++) {
            if (i == winnerIndex) {
                ((Neuron) getNeuronList().get(i)).setActivation(winValue);
            } else {
                ((Neuron) getNeuronList().get(i)).setActivation(loseValue);
            }
        }
    }

    /**
     *
     * Returns index of random winning neuron.
     *
     * @return index of random winner
     */
    private int getRandomWinnerIndex() {
        return new Random().nextInt(getNeuronList().size());
    }

    /**
     * Returns the index of the input node with the greatest net input.
     *
     * @return winning node's index
     */
    private int getWinningIndex() {
        int winnerIndex = 0;
        double max = Double.NEGATIVE_INFINITY;
        double lastVal  =  getNeuronList().get(0).getWeightedInputs();
        boolean tie = true;
        for (int i = 0; i < getNeuronList().size(); i++) {
            Neuron n = getNeuronList().get(i);
            double val = n.getWeightedInputs();
            if (val != lastVal) {
                tie = false;
            }
            lastVal = val;
            if (val > max) {
                winnerIndex = i;
                max = n.getWeightedInputs();
            }
        }
        // Break ties randomly
        // (TODO: Add a field so use can decide if they want this)
        if (tie) {
            winnerIndex = getRandomWinnerIndex();
        }
        return winnerIndex;
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

    /**
     * @return the useRandom
     */
    public boolean isUseRandom() {
        return useRandom;
    }

    /**
     * @param useRandom the useRandom to set
     */
    public void setUseRandom(boolean useRandom) {
        this.useRandom = useRandom;
    }

    /**
     * @return the randomProb
     */
    public double getRandomProb() {
        return randomProb;
    }

    /**
     * @param randomProb the randomProb to set
     */
    public void setRandomProb(double randomProb) {
        this.randomProb = randomProb;
    }

    public boolean getEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setEnabled(boolean enabled) {
        // TODO Auto-generated method stub
        
    }
}
