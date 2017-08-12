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
    private static final double DEFAULT_LEARNING_RATE = .01;

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

    /** Input layer. Separate for simpler indexing on other lists. */
    private DoubleMatrix inputLayer;

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

        // Initialize input and target datasets. Store data as columns
        // since that's what everything else deals with
        inputData = new DoubleMatrix(network.getTrainingSet().getInputData())
                .transpose();
        targetData = new DoubleMatrix(network.getTrainingSet().getTargetData())
                .transpose();

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

        System.out.println("-------\nInput: " + inputLayer);

        // Update network
        updateNetwork();

        // Backpropagate error
        DoubleMatrix targetVector = targetData.getColumn(exampleNum);
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
            mse += (outputError.get(j) * outputError.get(j));
        }
        mse = mse / network.getOutputNeurons().size();
        incrementIteration();
        fireErrorUpdated();
    }

    private void backpropagateError() {

        // From output layer backwards to input layer
        for (int ii = layers.size() - 1; ii > 0; ii--) {
            errors.get(ii).transpose().mmuli(
                    weightMatrices.get(ii - 1).transpose(), errors.get(ii - 1));
        }

    }

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
    }

}
