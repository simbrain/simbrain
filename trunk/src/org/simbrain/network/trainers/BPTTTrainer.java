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

import java.util.HashMap;
import java.util.List;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.DifferentiableUpdateRule;
import org.simbrain.network.subnetworks.BPTTNetwork;

/**
 * Trainer for backprop through time Networks. As a test use the "Walker"
 * tables.
 *
 * @author jyoshimi
 */
public class BPTTTrainer extends IterableTrainer {

    /** Reference to bptt being trained. */
    private final BPTTNetwork bptt;

    /** Current error. */
    private double mse;

    /** Default learning rate. */
    private static final double DEFAULT_LEARNING_RATE = .25;

    /** Learning rate. */
    private double learningRate = DEFAULT_LEARNING_RATE;

    /** Default momentum. */
    private static final double DEFAULT_MOMENTUM = .9;

    /** Momentum. Must be between 0 and 1. */
    private double momentum = DEFAULT_MOMENTUM;

    /** For storing current error contribution of each neuron. */
    private HashMap<Neuron, Double> errorMap;

    /** For storing weight deltas. */
    private HashMap<Synapse, Double> weightDeltaMap;

    /** For storing bias deltas. */
    private HashMap<Neuron, Double> biasDeltaMap;

    /**
     * Construct the SRN trainer.
     *
     * @param bptt
     *            the simple recurrent network
     */
    public BPTTTrainer(BPTTNetwork bptt) {
        super(bptt);
        this.bptt = bptt;
        errorMap = new HashMap<Neuron, Double>();
        weightDeltaMap = new HashMap<Synapse, Double>();
        biasDeltaMap = new HashMap<Neuron, Double>();
        setIteration(0);
        mse = 0;
    }

    @Override
    public void apply() {
        mse = 0;

        int numRows = getMinimumNumRows(network);
        int numInputs = network.getInputNeurons().size();
        // System.out.println("Data:" + numInputs + "/" + numRows);

        if ((numRows == 0) || (numInputs == 0)) {
            // TODO: Throw warning
            return;
        }

        for (int row = 0; row < numRows; row++) {
            if (firstPatternInSet()) {
                // System.out.println("First in set:" + iteration);
                // For new patterns begin with a regular forward propagation
                bptt.initNetwork();
                bptt.getInputLayer().setActivations(
                        network.getTrainingSet().getInputData()[row]);
                // bptt.getInputLayer().printActivations();
                bptt.getHiddenLayer().update();
                bptt.getOutputLayer().update();
                weightDeltaMap.clear();
                biasDeltaMap.clear();
            } else {
                // Update network. This puts outputs in to inputs.
                bptt.update();
                // bptt.getInputLayer().printActivations();
            }

            // Set weight and bias deltas by backpropagating error
            backpropagateStoreError(network, row);

            // Update weights
            if (lastPatternInSet()) {
                // System.out.println("Last in set:" + iteration);
                for (Synapse synapse : weightDeltaMap.keySet()) {
                    // System.out.println(synapse.getId() + ":"
                    // + Utils.round(synapse.getStrength(), 2) + " + "
                    // + Utils.round(weightDeltaMap.get(synapse), 2));
                    synapse.setStrength(synapse.getStrength()
                            + weightDeltaMap.get(synapse)
                            / bptt.getStepsPerSequences());
                }

                // Update biases
                for (Neuron neuron : biasDeltaMap.keySet()) {
                    BiasedUpdateRule biasedNeuron = (BiasedUpdateRule) (neuron
                            .getUpdateRule());
                    biasedNeuron.setBias(biasedNeuron.getBias()
                            + biasDeltaMap.get(neuron)
                            / bptt.getStepsPerSequences());
                }
            }
            incrementIteration();
        }

        // Update MSE/ TODO: Think about this error rep...
        mse = mse / (numRows * network.getOutputNeurons().size());
        fireErrorUpdated();

    }

    /**
     * True if this is the iteration that begans a training series.
     *
     * @return true if first patter in a set.
     */
    private boolean firstPatternInSet() {
        return ((getIteration() % bptt.getStepsPerSequences()) == 0);
    }

    /**
     * True if this is the iteration that ends a training series.
     *
     * @return true if last pattern in a set.
     */
    private boolean lastPatternInSet() {
        return ((getIteration() % bptt.getStepsPerSequences()) == (bptt
                .getStepsPerSequences() - 1));
    }

    /**
     * Compute error contribution for all nodes using backprop algorithm.
     *
     * @param row
     *            current row of training data
     */
    private void backpropagateStoreError(Trainable network, int row) {

        int i = 0;
        for (Neuron outputNeuron : bptt.getOutputNeurons()) {
            double targetValue = network.getTrainingSet().getTargetData()[row][i++];
            double outputError = targetValue - outputNeuron.getActivation();
            storeErrorAndDeltas(outputNeuron, outputError);
            mse += Math.pow(outputError, 2);
        }

        for (Neuron neuron : bptt.getHiddenLayer().getNeuronList()) {
            // Compute sum of fan-out errors on this neuron
            double sumFanOutErrors = 0;
            for (Synapse synapse : neuron.getFanOut().values()) {
                Neuron nextLayerNeuron = synapse.getTarget();
                sumFanOutErrors += (errorMap.get(nextLayerNeuron) * synapse
                        .getStrength());
            }
            // Compute errors and deltas for previous layer
            storeErrorAndDeltas(neuron, sumFanOutErrors);
        }
    }

    /**
     * Store the error value, bias delta, and fan-in weight deltas for this
     * neuron.
     *
     * @param neuron
     *            neuron whose activation function's derivative is used
     * @param error
     *            simple error for outputs, sum of errors in fan-out, times
     *            weights for hidden units
     */
    private void storeErrorAndDeltas(Neuron neuron, double error) {

        // Store error signal for this neuron
        double errorSignal = 0;
        if (neuron.getUpdateRule() instanceof DifferentiableUpdateRule) {
            double derivative = ((DifferentiableUpdateRule) neuron
                    .getUpdateRule()).getDerivative(neuron.getWeightedInputs());
            errorSignal = error * derivative;
            errorMap.put(neuron, errorSignal);
        }

        // Compute and store weight deltas for the fan-in to this neuron
        for (Synapse synapse : neuron.getFanIn()) {
            double lastWeightDelta = 0;
            if (weightDeltaMap.get(synapse) != null) {
                lastWeightDelta = weightDeltaMap.get(synapse);
            }
            double weightDelta = learningRate * errorSignal
                    * synapse.getSource().getActivation() + momentum
                    * lastWeightDelta;
            // Add up weight deltas
            if (weightDeltaMap.get(synapse) == null) {
                weightDeltaMap.put(synapse, weightDelta);
            } else {
                weightDeltaMap.put(synapse, weightDeltaMap.get(synapse)
                        + weightDelta);
            }
        }

        // Compute and store bias delta for this neuron
        if (neuron.getUpdateRule() instanceof BiasedUpdateRule) {
            double biasUpdate = learningRate * errorSignal;
            // Add up weight deltas
            if (biasDeltaMap.get(neuron) == null) {
                biasDeltaMap.put(neuron, biasUpdate);
            } else {
                biasDeltaMap.put(neuron, biasDeltaMap.get(neuron) + biasUpdate);
            }

        }
    }

    @Override
    public double getError() {
        return mse;
    }

    @Override
    public void randomize() {
        randomize(bptt.getHiddenLayer().getNeuronList());
        randomize(bptt.getOutputLayer().getNeuronList());
    }

    /**
     * Randomize the specified layer.
     *
     * @param layer
     *            the layer to randomize
     */
    private void randomize(List<Neuron> layer) {
        for (Neuron neuron : layer) {
            neuron.clear(); // Looks nicer in the GUI
            neuron.randomizeFanIn();
            neuron.randomizeBias(-.5, .5);
        }
    }

    /**
     * @return the learningRate
     */
    public double getLearningRate() {
        return learningRate;
    }

    /**
     * @param learningRate
     *            the learningRate to set
     */
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    /**
     * @return the momentum
     */
    public double getMomentum() {
        return momentum;
    }

    /**
     * @param momentum
     *            the momentum to set
     */
    public void setMomentum(double momentum) {
        this.momentum = momentum;
    }

}
