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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.jblas.DoubleMatrix;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.neuron_update_rules.TransferFunction;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.randomizer.Randomizer;

/**
 * Array-backed backprop, currently using JBlas.
 *
 * @author Jeff Yoshimi (50-50 co-authors)
 * @author Zach Tosi (50-50 co-authors)
 */
public class BackpropTrainer2 extends IterableTrainer {

    /** Current error. */
    private double mse;

    /** Default learning rate. */
    private static final double DEFAULT_LEARNING_RATE = .1;

    /** Learning rate. */
    private double learningRate = DEFAULT_LEARNING_RATE;

    /** The backprop network to be trained. */
    private BackpropNetwork net;

    /** Weight matrices ordered input to output. */
    private List<DoubleMatrix> weightMatrices;

    // TODO: Consider not representing input layer so that
    // indexing is consistent. So far causing headaches and
    // source of future bugs.
    /** Activation vectors ordered input to output. */
    private List<DoubleMatrix> layers;

    /** Net inputs for hidden through output layers. */
    private List<DoubleMatrix> netInputs;

    /** Biases for hidden through output layers. */
    private List<DoubleMatrix> biases;

    /** Errors for hidden through output layers. */
    private List<DoubleMatrix> errors;

    /** Inputs. */
    private DoubleMatrix inputs;

    /** Targets. */
    private DoubleMatrix targets;

    /** Parameter randomizer. */
    Randomizer rand = new Randomizer();

    /**
     * Construct the trainer.
     *
     * @param network the network to train
     */
    public BackpropTrainer2(final Trainable network) {
        super(network);

        if (!(network instanceof BackpropNetwork)) {
            throw new IllegalArgumentException(
                    "Backprop trainer must be applied to backprop network");
        }
        net = (BackpropNetwork) network;

        // Initialize list capacities
        layers = new ArrayList<DoubleMatrix>(net.getNeuronGroupList().size());
        netInputs = new ArrayList<DoubleMatrix>(
                net.getNeuronGroupList().size() - 1);
        biases = new ArrayList<DoubleMatrix>(
                net.getNeuronGroupList().size() - 1);
        errors = new ArrayList<DoubleMatrix>(
                net.getNeuronGroupList().size() - 1);
        weightMatrices = new ArrayList<DoubleMatrix>(
                net.getSynapseGroupList().size());

        // Given construction can assume that synapse group list is ordered from
        // input to output layers
        for (SynapseGroup sg : net.getSynapseGroupList()) {
            weightMatrices
                    .add(new DoubleMatrix(sg.getWeightMatrix()).transpose());
        }

        // Initialize layers
        int ii = 0;
        for (NeuronGroup ng : net.getNeuronGroupList()) {
            layers.add(DoubleMatrix.zeros(ng.size()));
            if (ii > 0) {
                biases.add(new DoubleMatrix(ng.getBiases()));
                netInputs.add(DoubleMatrix.zeros(ng.size()));
                errors.add(DoubleMatrix.zeros(ng.size()));
            }
            ii++;
        }

        // Initialize input and target datasets
        inputs = new DoubleMatrix(network.getTrainingSet().getInputData())
                .transpose();
        targets = new DoubleMatrix(network.getTrainingSet().getTargetData())
                .transpose();

        // Initialize randomizer
        rand.setPdf(ProbDistribution.NORMAL);
        rand.setParam1(0);
        rand.setParam2(.5);
    }

    @Override
    public void apply() throws DataNotInitializedException {
        int numRows = getMinimumNumRows(network); // Ignore extra rows
        int row = ThreadLocalRandom.current().nextInt(numRows);

        mse = 0;

        // Inputs are transposed for input dataset so "rows" are now columns
        DoubleMatrix inputVector = inputs.getColumn(row);
        // Set activations on input layer
        layers.get(0).copy(inputVector);
        System.out.println("-------\nInput: " + inputVector);

        // Update network
        updateNetwork();

        // Backpropagate error
        DoubleMatrix targetVector = targets.getColumn(row);
        DoubleMatrix outputError = errors.get(errors.size() - 1);
        targetVector.subi(getOutputLayer(), outputError);
        backpropagateError();

        // Update weights and biases
        updateParameters();

        System.out.println("Weights after: " + weightMatrices);
        System.out.println("Outputs: " + getOutputLayer());
        System.out.println("Targets: " + targetVector);

        // Update MSE
        for (int j = 0; j < outputError.length; j++) {
            mse += outputError.get(j) * outputError.get(j);
        }
        mse = mse / network.getOutputNeurons().size();
        incrementIteration();
        fireErrorUpdated();
    }

    private void backpropagateError() {

        // From second to last hidden layer backwards to first
        // hidden layer
        for (int ii = layers.size() - 2; ii > 0; ii--) {
            errors.get(ii).mmuli(weightMatrices.get(ii), errors.get(ii - 1));
        }

    }

    private void updateParameters() {

        // Update weights: learning rate * (error * f'(netin) * last layer
        // input)
        // Update biases: learning rate * (error * f'(netinput))

        int layerIndex = 1;
        for (DoubleMatrix wm : weightMatrices) {
            DoubleMatrix prevLayer = layers.get(layerIndex - 1);
            DoubleMatrix error = errors.get(layerIndex - 1);
            DoubleMatrix biasVector = biases.get(layerIndex - 1);

            // TODO: Can't use activations for non-logistic
            DoubleMatrix currentLayer = layers.get(layerIndex);
            DoubleMatrix derivs = DoubleMatrix.zeros(currentLayer.length);
            ((TransferFunction) net.getNeuronGroup(layerIndex)
                    .getNeuronListUnsafe().get(0).getUpdateRule())
                            .getDerivative(currentLayer, derivs);

            // TODO: Optimize with matrix operations
            // JBlas data laid out in a 1-d array
            int kk = 0;
            for (int ii = 0; ii < currentLayer.length; ii++) {
                for (int jj = 0; jj < prevLayer.length; jj++) {
                    wm.data[kk] -= learningRate * error.data[ii]
                            * derivs.data[ii] * prevLayer.data[jj];
                    kk++;
                }
            }
            for (int ii = 0; ii < biasVector.length; ii++) {
                biasVector.data[ii] -= learningRate * error.data[ii]
                        * derivs.data[ii];
            }
            layerIndex++;

        }

    }

    /**
     * Update the shadow array-based network.
     */
    private void updateNetwork() {

        int ii = 1;
        for (DoubleMatrix wm : weightMatrices) {

            // Set up variables for easy reading
            DoubleMatrix inputs = layers.get(ii - 1);
            DoubleMatrix activations = layers.get(ii);
            DoubleMatrix netInput = netInputs.get(ii - 1);
            DoubleMatrix biasVec = biases.get(ii - 1);

            // Activations = actFunction(matrix * inputs + biases)
            wm.mmuli(inputs, netInput);
            activations.copy(netInput);
            activations.addi(biasVec);
            // TODO: Maybe store a list of these for convenience
            ((TransferFunction) net.getNeuronGroup(ii).getNeuronListUnsafe()
                    .get(0).getUpdateRule()).applyFunctionInPlace(activations);

            System.out.println("Weights: " + wm);
            System.out.println("Activations: " + activations);
            ii++;
        }

    }

    /**
     * Helper to get output layer.
     */
    private DoubleMatrix getOutputLayer() {
        return layers.get(layers.size() - 1);
    }

    @Override
    public double getError() {
        return mse;
    }

    @Override
    public void randomize() {
        for (DoubleMatrix dm : weightMatrices) {
            for (int ii = 0; ii < dm.data.length; ii++) {
                dm.data[ii] = rand.getRandom();
            }
        }
        for (DoubleMatrix biasVector : biases) {
            for (int ii = 0; ii < biasVector.length; ii++) {
                biasVector.data[ii] = rand.getRandom();
            }
        }
        // TODO: Must push random values back to network afterwards
    }

    /**
     * Push the parameters of the array-based shadow network out to the Simbrain
     * network.
     */
    public void commitChanges() {
        System.out.println("here");
    }

}
