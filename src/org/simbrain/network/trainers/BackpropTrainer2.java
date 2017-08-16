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
import org.simbrain.util.propertyeditor.ComboBoxWrapper;
import org.simbrain.util.randomizer.Randomizer;

/**
 * Array-backed backprop, currently using JBlas.
 *
 * 50-50 co-authors
 * @author Jeff Yoshimi
 * @author Zach Tosi
 */
public class BackpropTrainer2 extends IterableTrainer {

    /** Current error. */
    private double mse;

    /** Default learning rate. */
    private static final double DEFAULT_LEARNING_RATE = .1;

    /** Learning rate. */
    private double learningRate = DEFAULT_LEARNING_RATE;
    
    /** Default momentum. */
    private static final double DEFAULT_MOMENTUM = .99;

    /** Momentum. Must be between 0 and 1. */
    private double momentum = DEFAULT_MOMENTUM;

    /** The backprop network to be trained. */
    private BackpropNetwork net;

    /** Weight matrices ordered input to output. */
    private List<DoubleMatrix> weightMatrices = new ArrayList<DoubleMatrix>();

    /** Memory of last weight updates for momentum. */
    private List<DoubleMatrix> lastWeightUpdates = 
            new ArrayList<DoubleMatrix>();

    /** Memory of last bias updates for momentum. */
    private List<DoubleMatrix> lastBiasUpdates = new ArrayList<DoubleMatrix>();

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

    /** List of activation functions for easy reference. */
    private List<TransferFunction> updateRules = new ArrayList<TransferFunction>();

    /** Possible update methods. */
    private enum UpdateMethod {
        EPOCH, BATCH, STOCHASTIC, MINI_BATCH;
    }

    /** Current update method. */
    private UpdateMethod updateMethod = UpdateMethod.EPOCH;

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
            DoubleMatrix weights = new DoubleMatrix(sg.getWeightMatrix())
                    .transpose();
            weightMatrices.add(weights);
            lastWeightUpdates
                    .add(DoubleMatrix.zeros(weights.rows, weights.columns));
        }

        // Initialize layers
        int ii = 0;
        for (NeuronGroup ng : net.getNeuronGroupList()) {
            inputLayer = DoubleMatrix.zeros(ng.size());
            if (ii > 0) {
                layers.add(DoubleMatrix.zeros(ng.size()));
                netInputs.add(DoubleMatrix.zeros(ng.size()));
                errors.add(DoubleMatrix.zeros(ng.size()));
                DoubleMatrix bs = new DoubleMatrix(ng.getBiases());
                biases.add(bs);
                lastBiasUpdates.add(DoubleMatrix.zeros(bs.rows, bs.columns));
                updateRules.add((TransferFunction) ng.getNeuronList().get(0)
                        .getUpdateRule());
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

        mse = 0;
        if (updateMethod == UpdateMethod.EPOCH) {
            for (int row = 0; row < numTrainingExamples; row++) {
                mse += updateBackprop(row);
            }
            mse = mse / numTrainingExamples;
        } else if (updateMethod == UpdateMethod.STOCHASTIC) {
            int rowNum = ThreadLocalRandom.current()
                    .nextInt(numTrainingExamples);
            mse = updateBackprop(rowNum);
        }
        // TODO: Other types

        // Print stats
        // printDebugInfo();

        incrementIteration();
        fireErrorUpdated();
    }

    /**
     * Backpropagate error on indicated row of dataset.
     *
     * @param rowNum row of dataset to update.
     * @return mean squared error relative to outputs
     */
    private double updateBackprop(final int rowNum) {

        inputLayer = inputData.getColumn(rowNum);

        // Update network
        updateNetwork();

        // Backpropagate error
        targetVector = targetData.getColumn(rowNum);
        DoubleMatrix outputError = errors.get(errors.size() - 1);
        getOutputLayer().subi(targetVector, outputError);
        backpropagateError();

        // Update weights and biases
        updateParameters();

        // Update MSE
        double error = 0;
        for (int j = 0; j < outputError.length; j++) {
            error += (outputError.get(j) * outputError.get(j));
        }
        mse = mse / network.getOutputNeurons().size();
        return error;

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
            updateRules.get(ii).applyFunctionInPlace(activations);
            ii++;
        }

    }

    /**
     * Set error values using backprop (roughly output errors times intervening
     * weights, going "backwards" from output towards input).
     */
    private void backpropagateError() {

        // From output layer backwards to input layer
        for (int ii = layers.size() - 1; ii > 0; ii--) {
            errors.get(ii).transpose().mmuli(
                    weightMatrices.get(ii - 1).transpose(), errors.get(ii - 1));
            // Todo: below different? performance seems worse but have not
            // formally tested.
            // errors.get(ii).mmuli(weightMatrices.get(ii - 1),
            // errors.get(ii - 1));

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
            DoubleMatrix lastDeltas = lastWeightUpdates.get(layerIndex);
            DoubleMatrix lastBiasDeltas = lastBiasUpdates.get(layerIndex);

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
            updateRules.get(layerIndex).getDerivative(currentLayer, derivs);

            // System.out.println("Deriv: " + derivs);

            // TODO: Optimize with matrix operations
            // JBlas data laid out in a 1-d array
            int kk = 0;
            for (int ii = 0; ii < currentLayer.length; ii++) {
                for (int jj = 0; jj < prevLayer.length; jj++) {
                    double deltaVal = learningRate * error.data[ii]
                            * derivs.data[ii] * prevLayer.data[jj]
                            + momentum * lastDeltas.data[kk];
                    wm.data[kk] -= deltaVal;
                    lastDeltas.data[kk] = deltaVal;
                    kk++;
                }
            }
            for (int ii = 0; ii < biasVector.length; ii++) {
                double deltaVal = learningRate * error.data[ii]
                        * derivs.data[ii] + momentum * lastBiasDeltas.data[ii];
                biasVector.data[ii] -= deltaVal;
                lastBiasDeltas.data[ii] = deltaVal;
            }
            layerIndex++;
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
    private void printDebugInfo() {
        System.out.println("---------------------------");
        System.out.println("Targets: " + targetVector);
        System.out.println("Node Layer 1");
        System.out.println("\tActivations:" + inputLayer);
        for (int i = 0; i < layers.size(); i++) {
            System.out.println("Weight Layer " + (i + 1) + " --> " + (i + 2));
            System.out.println("\tWeights:" + weightMatrices.get(i));
            System.out.println("Node Layer " + (i + 2));
            System.out.println("\tNet inputs: " + netInputs.get(i));
            System.out.println("\tBiases: " + biases.get(i));
            System.out.println("\tActivations: " + layers.get(i));
            System.out.println("\tErrors: " + errors.get(i));
            DoubleMatrix derivs = DoubleMatrix.zeros(layers.get(i).length);
            updateRules.get(i).getDerivative(layers.get(i), derivs);
            System.out.println("\tDerivatives: " + derivs);
        }

    }

    @Override
    public void commitChanges() {
        // Push the parameters of the array-based shadow network out to the
        // Simbrain network.
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

    /**
     * Returns the current solution type inside a comboboxwrapper. Used by
     * preference dialog.
     *
     * @return the the comboBox
     */
    public ComboBoxWrapper getUpdateMethod() {
        return new ComboBoxWrapper() {
            public Object getCurrentObject() {
                return updateMethod;
            }

            public Object[] getObjects() {
                return UpdateMethod.values();
            }
        };
    }

    /**
     * Set the current update method. Used by preference dialog.
     * 
     * @param umw update method wrapper
     */
    public void setUpdateMethod(final ComboBoxWrapper umw) {
        updateMethod = ((UpdateMethod) umw.getCurrentObject());
    }

    /**
     * @return the learningRate
     */
    public double getLearningRate() {
        return learningRate;
    }

    /**
     * @param learningRate the learningRate to set
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
     * @param momentum the momentum to set
     */
    public void setMomentum(double momentum) {
        this.momentum = momentum;
    }
}
