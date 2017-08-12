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

import org.apache.log4j.chainsaw.Main;
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
    private List<DoubleMatrix> weightMatrices = new ArrayList<DoubleMatrix>();

    /** Activation vectors. */
    private List<DoubleMatrix> layers = new ArrayList<DoubleMatrix>();

    /** Net inputs. */
    private List<DoubleMatrix> netInputs = new ArrayList<DoubleMatrix>();

    /** Biases. */
    private List<DoubleMatrix> biases = new ArrayList<DoubleMatrix>();

    /** Error. */
    private List<DoubleMatrix> errors = new ArrayList<DoubleMatrix>();

    /**
     * Input layer. Holds current input vector from input dataset. Separate for
     * simpler indexing on other lists.
     */
    private DoubleMatrix inputLayer;

    /** Current target vector. */
    private DoubleMatrix targetVector;

    /** Inputs. */
    private DoubleMatrix inputData;

    /** Targets. */
    private DoubleMatrix targetData;

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

        // Given construction can assume that synapse group list is ordered from
        // input to output layers
        for (SynapseGroup sg : net.getSynapseGroupList()) {
            weightMatrices
                    .add(new DoubleMatrix(sg.getWeightMatrix()).transpose());
        }

        // Initialize layers
        int ii = 0;
        for (NeuronGroup ng : net.getNeuronGroupList()) {
            inputLayer = DoubleMatrix.zeros(ng.size());
            if (ii > 0) {
                layers.add(DoubleMatrix.zeros(ng.size()));
                netInputs.add(DoubleMatrix.zeros(ng.size()));
                errors.add(DoubleMatrix.zeros(ng.size()));
                biases.add(new DoubleMatrix(ng.getBiases()));
            }
            ii++;
        }

        // Initialize randomizer
        rand.setPdf(ProbDistribution.NORMAL);
        rand.setParam1(0);
        rand.setParam2(1);
    }

    @Override
    public void apply() throws DataNotInitializedException {
        int numTrainingExamples = getMinimumNumRows(network);
        int exampleNum = ThreadLocalRandom.current()
                .nextInt(numTrainingExamples);

        mse = 0;

        inputLayer = inputData.getColumn(exampleNum);

        // Update network
        updateNetwork();

        // Backpropagate error
        targetVector = targetData.getColumn(exampleNum);
        DoubleMatrix outputError = errors.get(errors.size() - 1);
        targetVector.subi(getOutputLayer(), outputError);
        backpropagateError();

        // Print stats
        printStatus();

        // Update weights and biases
        updateParameters();

        // Update MSE
        for (int j = 0; j < outputError.length; j++) {
            mse += (outputError.get(j) * outputError.get(j));
        }
        mse = mse / network.getOutputNeurons().size();
        incrementIteration();
        fireErrorUpdated();
    }

    /**
     * Set error values using backprop (roughly output errors times intervening
     * weights, going "backwards" from output towards input.
     */
    private void backpropagateError() {

        // From output layer backwards to input layer
        for (int ii = layers.size() - 1; ii > 0; ii--) {
            errors.get(ii).transpose().mmuli(
                    weightMatrices.get(ii - 1).transpose(), errors.get(ii - 1));
        }

    }

    /**
     * Apply weight and bias updates.
     */
    private void updateParameters() {

        // Update weights: learning rate * (error * f'(netin) * last layer
        // input)
        // Update biases: learning rate * (error * f'(netinput))

        int layerIndex = 0;
        for (DoubleMatrix wm : weightMatrices) {
            DoubleMatrix prevLayer;
            if (layerIndex == 0) {
                prevLayer = inputLayer;
            } else {
                prevLayer = layers.get(layerIndex - 1);
            }
            DoubleMatrix error = errors.get(layerIndex);
            DoubleMatrix biasVector = biases.get(layerIndex);

            // TODO: Can't use activations for non-logistic
            DoubleMatrix currentLayer = layers.get(layerIndex);
            DoubleMatrix derivs = DoubleMatrix.zeros(currentLayer.length);
            ((TransferFunction) net.getNeuronGroup(layerIndex + 1)
                    .getNeuronListUnsafe().get(0).getUpdateRule())
                            .getDerivative(currentLayer, derivs);

            // System.out.println("Deriv: " + derivs);

            // TODO: Optimize with matrix operations
            // JBlas data laid out in a 1-d array
            int kk = 0;
            for (int ii = 0; ii < currentLayer.length; ii++) {
                for (int jj = 0; jj < prevLayer.length; jj++) {
                    wm.data[kk] += learningRate * error.data[ii]
                            * derivs.data[ii] * prevLayer.data[jj];
                    kk++;
                }
            }
            for (int ii = 0; ii < biasVector.length; ii++) {
                biasVector.data[ii] += learningRate * error.data[ii]
                        * derivs.data[ii];
            }
            layerIndex++;
        }

    }

    /**
     * Update the array-based "shadow" network.
     */
    private void updateNetwork() {

        int ii = 0;
        for (DoubleMatrix wm : weightMatrices) {

            // Set up variables for easy reading
            DoubleMatrix inputs;
            if (ii == 0) {
                inputs = inputLayer;
            } else {
                inputs = layers.get(ii - 1);
            }
            DoubleMatrix activations = layers.get(ii);
            DoubleMatrix netInput = netInputs.get(ii);
            DoubleMatrix biasVec = biases.get(ii);

            // Activations = actFunction(matrix * inputs + biases)
            wm.mmuli(inputs, netInput);
            activations.copy(netInput);
            activations.addi(biasVec);
            // TODO: Maybe store a list of these for convenience
            ((TransferFunction) net.getNeuronGroup(ii + 1).getNeuronListUnsafe()
                    .get(0).getUpdateRule()).applyFunctionInPlace(activations);

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
    }

    /**
     * Print debug info.
     */
    private void printStatus() {
        System.out.println("---------------------------");
        System.out.println("Targets: " + targetVector);
        System.out.println("Node Layer 1");
        System.out.println("\tActivations:" + inputLayer);
        for (int i = 0; i < layers.size(); i++) {
            System.out.println("Weight Layer " + (i + 1) + " --> "
                    + (i + 2));
            System.out.println("\tWeights:" + weightMatrices.get(i));
            System.out.println("Node Layer " + (i + 2));
            System.out.println("\tActivations: " + layers.get(i));
            System.out.println("\tBiases: " + biases.get(i));
            System.out.println("\tErrors: " + errors.get(i));
            System.out.println("\tNet inputs: " + netInputs.get(i));
        }

    }

    /**
     * Push the parameters of the array-based shadow network out to the Simbrain
     * network.
     */
    public void commitChanges() {
        for (int ii = 0; ii < net.getNeuronGroupList().size(); ii++) {
            // net.getNeuronGroupList().get(ii).setBiases(biases.get(ii).data);
            if (ii > 0) {
                // net.getSynapseGroupList().get(ii-1).setWeights(weightMatrices.get(ii-1).data);
            }
        }
    }

    /**
     * Initialize input and target datasets as JBlas arrays.
     */
    public void initData() {
        // Store data as columns since that's what everything else deals with
        // So no need to do tranposes later.
        if (network.getTrainingSet().getInputData() != null) {
            inputData = new DoubleMatrix(
                    network.getTrainingSet().getInputData()).transpose();
        }
        if (network.getTrainingSet().getTargetData() != null) {
            targetData = new DoubleMatrix(
                    network.getTrainingSet().getTargetData()).transpose();
        }
    }

}
