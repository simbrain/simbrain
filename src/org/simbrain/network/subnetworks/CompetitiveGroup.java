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

import java.util.Iterator;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.neuron_update_rules.LinearRule;

/**
 * <b>Competitive</b> implements a simple competitive network.
 *
 * Current implementations include Rummelhart-Zipser (PDP, 151-193), and
 * Alvarez-Squire 1994, PNAS, 7041-7045.
 *
 * @author Jeff Yoshimi
 */
public class CompetitiveGroup extends NeuronGroup {

    // TODO: Add "recall" function as with SOM

    /** Learning rate. */
    private double learningRate = .1;

    /** Winner value. */
    private double winValue = 1;

    /** loser value. */
    private double loseValue = 0;

    /** Normalize inputs boolean. */
    private boolean normalizeInputs = true;

    /** Use leaky learning boolean. */
    private boolean useLeakyLearning = false;

    /** Leaky learning rate . */
    private double leakyLearningRate = learningRate / 4;

    /**
     * Percentage by which to decay synapses on each update for for
     * Alvarez-Squire update.
     */
    private double synpaseDecayPercent = .0008;

    /** Max, value and activation values. */
    private double max, val, activation;

    /** Winner value. */
    private int winner;

    /** Current update method. */
    private UpdateMethod updateMethod = UpdateMethod.RUMM_ZIPSER;

    /**
     * Specific implementation of competitive learning.
     */
    public enum UpdateMethod {
        /**
         * Rummelhart-Zipser.
         */
        RUMM_ZIPSER {
            @Override
            public String toString() {
                return "Rummelhart-Zipser";
            }
        },

        /**
         * Alvarez-Squire.
         */
        ALVAREZ_SQUIRE {
            @Override
            public String toString() {
                return "Alvarez-Squire";
            }
        }
    }

    /**
     * Constructs a competitive network with specified number of neurons.
     *
     * @param numNeurons
     *            size of this network in neurons
     * @param root
     *            reference to Network.
     */
    public CompetitiveGroup(final Network root, final int numNeurons) {
        super(root);
        for (int i = 0; i < numNeurons; i++) {
            addNeuron(new Neuron(root, new LinearRule()));
        }
        setLabel("Competitive Group");
    }

    @Override
    public String getTypeDescription() {
        return "Competitive Group";
    }

    // /**
    // * Copy constructor.
    // *
    // * @param newParent new root network
    // * @param oldNet old network.
    // */
    // public Competitive(Network newRoot, Competitive oldNet) {
    // super(newRoot);
    // setEpsilon(oldNet.getEpsilon());
    // setLeakyEpsilon(oldNet.getLeakyEpsilon());
    // setLoseValue(oldNet.getLoseValue());
    // setWinValue(oldNet.getWinValue());
    // setNormalizeInputs(oldNet.getNormalizeInputs());
    // }

    @Override
    public void update() {

        super.update();

        max = 0;
        winner = 0;

        // Determine Winner
        for (int i = 0; i < getNeuronList().size(); i++) {
            Neuron n = getNeuronList().get(i);
            if (!n.isClamped()) {
                n.update();
            }
            if (n.getActivation() > max) {
                max = n.getActivation();
                winner = i;
            }
        }

        // Update weights on winning neuron
        for (int i = 0; i < getNeuronList().size(); i++) {
            Neuron neuron = getNeuronList().get(i);
            if (i == winner) {
                neuron.setActivation(winValue);
                if (updateMethod == UpdateMethod.RUMM_ZIPSER) {
                    rummelhartZipser(neuron);
                } else if (updateMethod == UpdateMethod.ALVAREZ_SQUIRE) {
                    squireAlvarezWeightUpdate(neuron);
                    decayAllSynapses();
                }
            } else {
                neuron.setActivation(loseValue);
                if (useLeakyLearning) {
                    leakyLearning(neuron);
                }

            }
        }
        // normalizeIncomingWeights();
    }

    /**
     * Update winning neuron's weights in accordance with Alvarez and Squire
     * 1994, eq 2. TODO: rate is unused... in fact everything before
     * "double deltaw = learningRate" (line 200 at time of writing) cannot
     * possibly change any variables in the class.
     *
     * @param neuron
     *            winning neuron.
     */
    private void squireAlvarezWeightUpdate(final Neuron neuron) {
        for (Synapse synapse : neuron.getFanIn()) {
            double deltaw = learningRate
                    * synapse.getTarget().getActivation()
                    * (synapse.getSource().getActivation() - synapse
                            .getTarget().getAverageInput());
            synapse.setStrength(synapse.clip(synapse.getStrength() + deltaw));
        }
    }

    /**
     * Update winning neuron's weights in accordance with PDP 1, p. 179.
     *
     * @param neuron
     *            winning neuron.
     */
    private void rummelhartZipser(final Neuron neuron) {
        double sumOfInputs = neuron.getTotalInput();
        // Apply learning rule
        for (Synapse synapse : neuron.getFanIn()) {
            activation = synapse.getSource().getActivation();

            // Normalize the input values
            if (normalizeInputs) {
                if (sumOfInputs != 0) {
                    activation = activation / sumOfInputs;
                }
            }

            double deltaw = learningRate * (activation - synapse.getStrength());
            synapse.setStrength(synapse.clip(synapse.getStrength() + deltaw));
        }
    }

    /**
     * Decay attached synapses in accordance with Alvarez and Squire 1994, eq 3.
     */
    private void decayAllSynapses() {
        for (Neuron n : getNeuronList()) {
            for (Synapse synapse : n.getFanIn()) {
                synapse.decay(synpaseDecayPercent);
            }
        }

    }

    /**
     * Apply leaky learning to provided learning.
     *
     * @param neuron
     *            neuron to apply leaky learning to
     */
    private void leakyLearning(final Neuron neuron) {
        double sumOfInputs = neuron.getTotalInput();
        for (Synapse incoming : neuron.getFanIn()) {
            activation = incoming.getSource().getActivation();
            if (normalizeInputs) {
                if (sumOfInputs != 0) {
                    activation = activation / sumOfInputs;
                }
            }
            val = incoming.getStrength() + leakyLearningRate
                    * (activation - incoming.getStrength());
            incoming.setStrength(val);
        }
    }

    /**
     * Normalize weights coming in to this network, separately for each neuron.
     */
    public void normalizeIncomingWeights() {

        for (Neuron n : getNeuronList()) {
            double normFactor = n.getSummedIncomingWeights();
            for (Synapse s : n.getFanIn()) {
                s.setStrength(s.getStrength() / normFactor);
            }
        }
    }

    /**
     * Normalize all weights coming in to this network.
     */
    public void normalizeAllIncomingWeights() {

        double normFactor = getSummedIncomingWeights();
        for (Neuron n : getNeuronList()) {
            for (Synapse s : n.getFanIn()) {
                s.setStrength(s.getStrength() / normFactor);
            }
        }
    }

    /**
     * Randomize all weights coming in to this network.
     *
     * TODO: Add gaussian option...
     */
    public void randomizeIncomingWeights() {

        for (Iterator<Neuron> i = getNeuronList().iterator(); i.hasNext();) {
            Neuron n = i.next();
            for (Synapse s : n.getFanIn()) {
                s.randomize();
            }
        }
    }

    /**
     * Returns the sum of all incoming weights to this network.
     *
     * @return the sum of all incoming weights to this network.
     */
    private double getSummedIncomingWeights() {
        double ret = 0;
        for (Iterator<Neuron> i = getNeuronList().iterator(); i.hasNext();) {
            Neuron n = i.next();
            ret += n.getSummedIncomingWeights();
        }
        return ret;
    }

    /**
     * Randomize and normalize weights.
     */
    public void randomize() {
        randomizeIncomingWeights();
        normalizeIncomingWeights();
    }

    /**
     * Return the learning rate.
     *
     * @return the learning rate
     */
    public double getLearningRate() {
        return learningRate;
    }

    /**
     * Sets learning rate.
     *
     * @param rate
     *            The new epsilon value.
     */
    public void setLearningRate(final double rate) {
        this.learningRate = rate;
    }

    /**
     * Return the loser value.
     *
     * @return the loser Value
     */
    public final double getLoseValue() {
        return loseValue;
    }

    /**
     * Sets the loser value.
     *
     * @param loseValue
     *            The new loser value
     */
    public final void setLoseValue(final double loseValue) {
        this.loseValue = loseValue;
    }

    /**
     * Return the winner value.
     *
     * @return the winner value
     */
    public final double getWinValue() {
        return winValue;
    }

    /**
     * Sets the winner value.
     *
     * @param winValue
     *            The new winner value
     */
    public final void setWinValue(final double winValue) {
        this.winValue = winValue;
    }

    /**
     * Return leaky learning rate.
     *
     * @return Leaky learning rate
     */
    public double getLeakyLearningRate() {
        return leakyLearningRate;
    }

    /**
     * Sets the leaky learning rate.
     *
     * @param leakyRate
     *            Leaky rate value to set
     */
    public void setLeakyLearningRate(final double leakyRate) {
        this.leakyLearningRate = leakyRate;
    }

    /**
     * Return the normalize inputs value.
     *
     * @return the normailize inputs value
     */
    public boolean getNormalizeInputs() {
        return normalizeInputs;
    }

    /**
     * Sets the normalize inputs value.
     *
     * @param normalizeInputs
     *            Normalize inputs value to set
     */
    public void setNormalizeInputs(final boolean normalizeInputs) {
        this.normalizeInputs = normalizeInputs;
    }

    /**
     * Return the leaky learning value.
     *
     * @return the leaky learning value
     */
    public boolean getUseLeakyLearning() {
        return useLeakyLearning;
    }

    /**
     * Sets the leaky learning value.
     *
     * @param useLeakyLearning
     *            The leaky learning value to set
     */
    public void setUseLeakyLearning(final boolean useLeakyLearning) {
        this.useLeakyLearning = useLeakyLearning;
    }

    /**
     * @return the synpaseDecayPercent
     */
    public double getSynpaseDecayPercent() {
        return synpaseDecayPercent;
    }

    /**
     * @param synpaseDecayPercent
     *            the synpaseDecayPercent to set
     */
    public void setSynpaseDecayPercent(double synpaseDecayPercent) {
        this.synpaseDecayPercent = synpaseDecayPercent;
    }

    /**
     * @return the updateMethod
     */
    public UpdateMethod getUpdateMethod() {
        return updateMethod;
    }

    /**
     * @param updateMethod
     *            the updateMethod to set
     */
    public void setUpdateMethod(UpdateMethod updateMethod) {
        this.updateMethod = updateMethod;
    }

}
