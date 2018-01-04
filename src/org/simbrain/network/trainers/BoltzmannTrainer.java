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
package org.simbrain.network.trainers;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.subnetworks.BoltzmannMachine;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 *
 * @author Jeff Yoshimi
 */
public class BoltzmannTrainer extends Trainer {

    /**
     * Reference to trainable network.
     */
    private final BoltzmannMachine network;

    /**
     * Flag used for iterative training methods.
     */
    private boolean updateCompleted = true;

    /**
     * Iteration number. An epoch.
     */
    private int iteration = 0;

    /**
     * Map of Synapse statistics.
     */
    private Map<Synapse, Float> pcMap = new HashMap<>();
    private Map<Synapse, Float> pfMap = new HashMap<>();

    /**
     * Construct the UnsupervisedNeuronGroupTrainer trainer.
     *
     * @param network the parent network
     */
    public BoltzmannTrainer(BoltzmannMachine network) {
        super(network);
        this.network = network;
        this.setIteration(0);

    }

    @Override
    public void apply() throws DataNotInitializedException {

        // Q: Shouldn't we check target data too?
        if (network.getTrainingSet().getInputData() == null) {
            throw new DataNotInitializedException("Input data not initalized");
        }

        // See Fausett, pp. 369-370
        int numRows = network.getTrainingSet().getInputData().length;
        for (int row = 0; row < numRows; row++) {
            double[] inputs = network.getTrainingSet().getInputData()[row];

            // Compute PC: given that visible units are clamped, compute
            // probability that a given pair of neurons are both on relative to
            // the training set,and at equilibrium
            network.getInputLayer().forceSetActivations(inputs);
            network.getInputLayer().setClamped(true);

            // Steps 4-10
            // Get network to equilibrium
            // Update network N(default value 10) times
            for (int i = 0; i < BoltzmannMachine.DEFAULT_INIT_SIZE; i++) {
                network.update();
            }

            // Gather statistics for clamped case
            // Steps 12-19

            // Initialize all pc values to 0
            for (Synapse synapse : network.getFlatSynapseList()) {
                pcMap.put(synapse, 0f);
            }
            for (Neuron neuron : network.getFlatNeuronList()) {
                for (Synapse synapse : neuron.getFanIn()) {
                    if (synapse.getTarget().getActivation() > 0) {
                        pcMap.put(synapse, pcMap.get(synapse) + 1);
                    }
                }
            }
            for (Synapse synapse : pcMap.keySet()) {
                pcMap.put(synapse, pcMap.get(synapse) / numRows);
            }

            //////////////////////////////

            // Compute PF: given that visible units are "free", compute
            // probabilty that a given pair of neurons are both on relative to
            // the training set, and at equilibrium

            network.getInputLayer().forceSetActivations(inputs);
            network.getInputLayer().setClamped(false);

            // Do the stuff above again for pfmap

            // Let network reach equilibrium again
            for (int i = 0; i < BoltzmannMachine.DEFAULT_INIT_SIZE; i++) {
                network.update();
            }

            // Initialize pf to 0
            for (Synapse synapse : network.getFlatSynapseList()) {
                pfMap.put(synapse, 0f);
            }
            for (Neuron neuron : network.getFlatNeuronList()) {
                for (Synapse synapse : neuron.getFanIn()) {
                    if (synapse.getTarget().getActivation() > 0) {
                        pfMap.put(synapse, pfMap.get(synapse) + 1);
                    }
                }
            }
            // Step 36: Compute average
            for (Synapse synapse : pfMap.keySet()) {
                pfMap.put(synapse, pfMap.get(synapse) / numRows);
            }

            // Step 37: update the weights
            for (Synapse synapse : network.getFlatSynapseList()) {
                if (pcMap.get(synapse) > pfMap.get(synapse)) {
                    synapse.setStrength(synapse.getStrength() + 2);
                } else {
                    synapse.setStrength(synapse.getStrength() - 2);
                }
            }
        }

        incrementIteration();

        // Make sure excitatory/inhibitory are in proper lists
        revalidateSynapseGroups();

    }

    /**
     * @return boolean updated completed.
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Sets updated completed value.
     *
     * @param updateCompleted Updated completed value to be set
     */
    public void setUpdateCompleted(final boolean updateCompleted) {
        this.updateCompleted = updateCompleted;
    }

    /**
     * Increment the iteration number by 1.
     */
    public void incrementIteration() {
        iteration++;
    }

    /**
     * @param iteration the iteration to set
     */
    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    /**
     * Return the current iteration.
     *
     * @return current iteration.
     */
    public int getIteration() {
        return iteration;
    }

}
