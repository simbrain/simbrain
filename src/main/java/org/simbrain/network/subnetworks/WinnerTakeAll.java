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
package org.simbrain.network.subnetworks;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.UserParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <b>WinnerTakeAll</b>.The neuron with the highest weighted input in a
 * winner-take-all network takes on an upper value, all other neurons take on
 * the lower value. In case of a tie a randomly chosen member of the "winners"
 * is returned.
 */
public class WinnerTakeAll extends NeuronGroup {

    /**
     * Winning value.
     */
    @UserParameter(label = "Win value", order = 50)
    private double winValue = 1;

    /**
     * Losing value.
     */
    @UserParameter(label = "Lose value", order = 60)
    private double loseValue = 0;

    /**
     * If true, sometimes set the winner randomly.
     */
    @UserParameter(label = "Random winner", order = 70)
    private boolean useRandom;

    /**
     * Probability of setting the winner randomly, when useRandom is true.
     */
    @UserParameter(label = "Random prob", widgetForConditionalEnabling = "Random winner", order = 80)
    private double randomProb = .1;

    /**
     * Random number generator.
     */
    private static Random rand = new Random();

    /**
     * Copy constructor.
     *
     * @param newRoot new root net
     * @param oldNet  old network
     */
    public WinnerTakeAll(Network newRoot, WinnerTakeAll oldNet) {
        super(newRoot, oldNet);
        setLoseValue(oldNet.getLoseValue());
        setWinValue(oldNet.getWinValue());
        setUseRandom(oldNet.isUseRandom());
        setRandomProb(oldNet.getRandomProb());
        setLabel("WTA Group (copy)");
    }

    /**
     * Creates a new winner take all network.
     *
     * @param root       the network containing this subnetwork
     * @param numNeurons Number of neurons in new network
     */
    public WinnerTakeAll(final Network root, final int numNeurons) {
        super(root);
        for (int i = 0; i < numNeurons; i++) {
            // TODO: Prevent invalid states like this?
            this.addNeuron(new Neuron(root, new LinearRule()));
        }
        setLabel("Winner take all network");
    }

    @Override
    public WinnerTakeAll deepCopy(Network newNetwork) {
        return new WinnerTakeAll(newNetwork, this);
    }

    @Override
    public String getTypeDescription() {
        return "Winner Take All Group";
    }

    @Override
    public void update() {
        Neuron winner = getWinner();
        if (useRandom) {
            if (Math.random() < randomProb) {
                winner = getNeuronList().get(rand.nextInt(getNeuronList().size()));
            }
        }
        for (Neuron neuron : getNeuronList()) {
            if (neuron == winner) {
                neuron.setActivation(winValue);
            } else {
                neuron.setActivation(loseValue);
            }
        }
    }

    /**
     * Returns the neuron with the greatest net input.
     *
     * @return winning neuron
     */
    public Neuron getWinner() {
        return getWinner(getNeuronList());
    }

    /**
     * Returns the neuron in the provided list with the greatest net input (or a
     * randomly chosen neuron among those that "win").
     *
     * @param neuronList the list to check
     * @return the neuron with the highest net input
     */
    public static Neuron getWinner(List<Neuron> neuronList) {
        return getWinner(neuronList, false);
    }

    /**
     * Returns the neuron in the provided list with the greatest net input or
     * activation (or a randomly chosen neuron among those that "win").
     *
     * @param neuronList     the list to check
     * @param useActivations if true, use activations instead of net input to
     *                       determine winner
     * @return the neuron with the highest net input
     */
    public static Neuron getWinner(List<Neuron> neuronList, boolean useActivations) {

        if (neuronList.isEmpty()) {
            return null;
        }

        List<Neuron> winners = new ArrayList<Neuron>();
        Neuron winner = neuronList.get(0);
        winners.add(winner);
        for (Neuron n : neuronList) {
            double winnerVal = useActivations ? winner.getActivation() : winner.getWeightedInputs();
            double val = useActivations ? n.getActivation() : n.getWeightedInputs();
            if (val == winnerVal) {
                winners.add(n);
            } else if (val > winnerVal) {
                winners.clear();
                winner = n;
                winners.add(n);
            }
        }
        if (winners.size() == 1) {
            return winner;
        } else {
            return winners.get(rand.nextInt(winners.size()));
        }

    }

    public double getLoseValue() {
        return loseValue;
    }

    public void setLoseValue(final double loseValue) {
        this.loseValue = loseValue;
    }

    public double getWinValue() {
        return winValue;
    }

    public void setWinValue(final double winValue) {
        this.winValue = winValue;
    }

    public boolean isUseRandom() {
        return useRandom;
    }

    public void setUseRandom(boolean useRandom) {
        this.useRandom = useRandom;
    }

    public double getRandomProb() {
        return randomProb;
    }

    public void setRandomProb(double randomProb) {
        this.randomProb = randomProb;
    }
}