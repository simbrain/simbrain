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

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.dl4j.WeightMatrix;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.neuron_update_rules.TransferFunction;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Array-backed backprop. To be supplanted by new nd4j objects, but still allows backprop with "loose" neurons and old
 * style neuron groups, which has some pedagogical purpose.
 * <p>
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public class BackpropTrainer extends IterableTrainer {

    /**
     * Default learning rate.
     */
    public static final double DEFAULT_LEARNING_RATE = 0.1;

    /**
     * Default momentum.
     */
    public static final double DEFAULT_MOMENTUM = 0.2;

    /**
     * The backprop network to be trained.
     */
    private BackpropNetwork net;

    /**
     * Memory of last weight updates for momentum.
     */
    private List<INDArray> lastWeightUpdates = new ArrayList<>();

    /**
     * Memory of last bias updates for momentum.
     */
    private List<INDArray> lastBiasUpdates = new ArrayList<>();

    /**
     * Activation vectors.
     */
    private List<INDArray> activations = new ArrayList<>();

    /**
     * Reference to neuron groups.
     */
    private List<NeuronGroup> neuronGroups = new ArrayList<NeuronGroup>();

    /**
     * Net inputs.
     */
    private List<INDArray> netInputs = new ArrayList<>();

    /**
     * Biases.
     */
    private List<INDArray> biases = new ArrayList<>();

    /**
     * Errors for a single input row.
     */
    private INDArray errors;

    /**
     * Aggregate errors for a batch of input rows.
     */
    private INDArray batchErrors;

    /**
     * Deltas on on the neurons of the network (error times derivative).
     */
    private List<INDArray> deltas = new ArrayList<INDArray>();

    /**
     * Holder for derivatives.
     */
    private List<INDArray> derivs = new ArrayList<INDArray>();

    /**
     * Input layer. Holds current input vector from input dataset. Separate for simpler indexing on other lists.
     */
    private INDArray inputLayer;

    /**
     * Current target vector.
     */
    private INDArray targetVector;

    /**
     * Inputs.
     */
    private INDArray inputData;

    /**
     * Targets.
     */
    private INDArray targetData;

    /**
     * List of activation functions for easy reference.
     */
    private List<TransferFunction> updateRules = new ArrayList<TransferFunction>();

    @UserParameter(label = "Learning Rate", description = "Learning Rate", minimumValue = 0, maximumValue = 10,
            order = 1)
    private double learningRate = DEFAULT_LEARNING_RATE;

    /**
     * Current update method.
     */
    @UserParameter(label = "Update Method", description = "Update Method", order = 10)
    private UpdateMethod updateMethod = UpdateMethod.SINGLE;

    /**
     * Specifies the method for batching data when calculating network outputs and errors.
     */
    public enum UpdateMethod {
        EPOCH {
            public String toString() {
                return "Epoch (whole dataset per iteration)";
            }
        }, STOCHASTIC {
            public String toString() {
                return "Stochastic (random row per iteration)";
            }
        }, SINGLE {
            public String toString() {
                return "Single (one row per iteration)";
            }
        }
    }
    // TODO: Mini-batch

    /**
     * Momentum. Must be between 0 and 1.
     */
    @UserParameter(label = "Momentum", description = "Momentum", minimumValue = 0, maximumValue = 10, order = 50)
    private double momentum = DEFAULT_MOMENTUM;

    /**
     * Mean squared error of the most recent training step.
     */
    private double mse;

    /**
     * Construct the trainer.
     *
     * @param network the network to train
     */
    public BackpropTrainer(BackpropNetwork network) {
        super(network);
        net = network;

        // Synapse group list is ordered from input to output layers
        for (WeightMatrix wm : net.getWeightMatrixList()) {
            lastWeightUpdates.add(Nd4j.zeros(wm.getWeightMatrix().rows(), wm.getWeightMatrix().columns()));
        }

        // Initialize layers
        int ii = 0;
        for (NeuronGroup neuronGroup : net.getNeuronGroupList()) {
            if (ii > 0) {
                activations.add(Nd4j.zeros(1,neuronGroup.size()));
                netInputs.add(Nd4j.zeros(1,neuronGroup.size()));
                deltas.add(Nd4j.zeros(1,neuronGroup.size()));
                INDArray bs = Nd4j
                        .create(Utils.castToFloat(neuronGroup.getBiases()))
                        .reshape(1, neuronGroup.getBiases().length);
                biases.add(bs);
                lastBiasUpdates.add(Nd4j.zeros(bs.rows(), bs.columns()));
                updateRules.add((TransferFunction) neuronGroup.getNeuronList().get(0).getUpdateRule());
                neuronGroups.add(neuronGroup);
                derivs.add(Nd4j.zeros(1,neuronGroup.size()));
            } else {
                inputLayer = Nd4j.zeros(1,neuronGroup.size());
            }
            ii++;
        }
        errors = Nd4j.zeros(1, getOutputLayer().length());
        batchErrors = Nd4j.zeros(1, getOutputLayer().length());
        setLearningRate(DEFAULT_LEARNING_RATE);
        setMomentum(DEFAULT_MOMENTUM);
    }

    /**
     * Construct the backprop trainer.
     *
     * @param network the network
     * @param layers  the layers to train
     */
    public BackpropTrainer(Trainable network, List<List<Neuron>> layers) {
        //TODO: Here to appease SRNTrainer.  Not yet re-implemented.
        super(network);
    }

    @Override
    public void apply() {
        // Apply one training step according to the currently selected update method
        mse = 0;
        int numTrainingExamples = getMinimumNumRows(network);
        if (updateMethod == UpdateMethod.EPOCH) {
            mse = trainRows(0, numTrainingExamples);
        } else if (updateMethod == UpdateMethod.STOCHASTIC) {
            int rowNum = ThreadLocalRandom.current().nextInt(numTrainingExamples);
            mse = trainRow(rowNum);
        } else if (updateMethod == UpdateMethod.SINGLE) {
            mse = trainRow(getIteration() % numTrainingExamples);
        }
        incrementIteration();
        getEvents().fireErrorUpdated();
    }

    /**
     * Backpropagate error on a single row of the dataset.
     *
     * @param row Which row of the dataset to use for this update.
     * @return mean squared error of the row
     */
    private double trainRow(int row) {
        batchErrors.muli(0);
        // Get the inputs and feed them forward
        inputLayer = inputData.getRow(row, true);
        updateNetwork();
        // Backpropagate error
        targetVector = targetData.getRow(row, true);
        targetVector.subi(getOutputLayer(), errors);
        batchErrors.addi(errors);
        backpropagateError();
        // Update weights and biases
        updateParameters();
        // Return the MSE for the row
        return (double) batchErrors.mul(batchErrors).sumNumber() / network.getOutputNeurons().size();
    }

    /**
     * Backpropagate errors for all rows in the dataset.
     *
     * @return mean squared error
     */
    private double trainRows(int firstRow, int lastRow) {
        // Get inputs and feed them forward row-by-row
        batchErrors.muli(0);
        for (int row = firstRow; row < lastRow; row++) {

            // Get the inputs and feed them forward
            inputLayer = inputData.getRow(row, true);
            updateNetwork();
            targetVector = targetData.getRow(row, true);
            targetVector.subi(getOutputLayer(), errors);

            // Calculate batch errors
            batchErrors.addi(errors);
            System.out.println(batchErrors);
        }
        batchErrors.divi(lastRow - firstRow);
        // Back propagate batch errors
        backpropagateError();
        // Update weights and biases
        updateParameters();
        // Return the MSE for the batch
        return (double) batchErrors.mul(batchErrors).sumNumber() / network.getOutputNeurons().size();
    }

    /**
     * Update the array-based "shadow" network.
     */
    private void updateNetwork() {

        for (int i = 0; i < net.getWeightMatrixList().size(); i++) {

            // Set up variables for easy reading
            INDArray netInput = netInputs.get(i);
            INDArray biasVec = biases.get(i);
            INDArray weights = net.getWeightMatrixList().get(i).getWeightMatrix();

            // Set inputs
            INDArray inputs;
            if (i == 0) {
                inputs = inputLayer;
            } else {
                inputs = activations.get(i - 1);
            }

            // Multiply weight matrix times inputs and store in next layer netInput
            inputs.mmuli(weights, netInput);

            // Add biases to the net input
            netInput.addi(biasVec);

            // Apply the transfer function to net input to get the activation values for the next layer and store
            // that value in the activations vector, also calculate derivatives.
            updateRules.get(i).applyFunctionAndDerivative(netInput, activations.get(i), derivs.get(i));
        }
    }

    /**
     * Set error values using backprop (roughly output errors times intervening weights, going "backwards" from output
     * towards input).
     */
    private void backpropagateError() {
        int maxLayerIndex = activations.size() - 1;

        // Calculate output deltas from error and derivative
        batchErrors.muli(derivs.get(maxLayerIndex), deltas.get(maxLayerIndex));
        INDArray wts = net.getWeightMatrixList().get(maxLayerIndex).getWeightMatrix();
        deltas.get(maxLayerIndex).mmuli(wts.transpose(),deltas.get(maxLayerIndex - 1));

        // Deltas for 2nd to last layer
        deltas.get(maxLayerIndex - 1).muli(derivs.get(maxLayerIndex - 1));
        // For multiple hidden layers
        for (int layerIndex = maxLayerIndex - 1; layerIndex > 0; layerIndex--) {
            wts = net.getWeightMatrixList().get(layerIndex).getWeightMatrix();
            deltas.get(layerIndex).mmuli(wts.transpose(),deltas.get(layerIndex - 1));
            deltas.get(layerIndex - 1).muli(derivs.get(layerIndex - 1));
        }
    }

    /**
     * Apply weight and bias updates.
     */
    private void updateParameters() {
        int layerIndex = 0;
        for (WeightMatrix weightMatrix : net.getWeightMatrixList()) {
            INDArray wm = weightMatrix.getWeightMatrix();
            INDArray prevLayer;
            INDArray lastDeltas = lastWeightUpdates.get(layerIndex);
            INDArray lastBiasDeltas = lastBiasUpdates.get(layerIndex);

            if (layerIndex == 0) {
                prevLayer = inputLayer;
            } else {
                prevLayer = activations.get(layerIndex - 1);
            }
            INDArray biasVector = biases.get(layerIndex);
            INDArray currentLayer = activations.get(layerIndex);

            // Update weights, traversing along weight matrix in column-major order
            int kk = 0;
            for (int ii = 0; ii < prevLayer.length(); ii++) {
                for (int jj = 0; jj < currentLayer.length(); jj++) {
                    float deltaVal =
                            (float) (learningRate * deltas.get(layerIndex).getDouble(jj) * prevLayer.getDouble(ii) + (momentum * lastDeltas.getDouble(kk)));
                    wm.putScalar(kk, wm.getDouble(kk) + deltaVal);
                    lastDeltas.putScalar(kk, deltaVal);
                    kk++;
                }
            }
            // Update biases
            for (int ii = 0; ii < biasVector.length(); ii++) {
                float deltaVal =
                        (float) (learningRate * deltas.get(layerIndex).getDouble(ii) + (momentum * lastBiasDeltas.getDouble(ii)));
                biasVector.putScalar(ii, biasVector.getDouble(ii) + deltaVal);
                lastBiasDeltas.putScalar(ii, deltaVal);
            }
            weightMatrix.getEvents().fireUpdated();
            layerIndex++;
        }
    }

    /**
     * Helper to get output layer.
     */
    private INDArray getOutputLayer() {
        return activations.get(activations.size() - 1);
    }

    @Override
    public double getError() {
        return mse;
    }

    @Override
    public void randomize() {
        // Randomize weights
        net.getWeightMatrixList().forEach(WeightMatrix::randomize);
        // Randomize biases
        // TODO: Move randomization of ndarrays to utility method
        for (int kk = 0; kk < biases.size(); ++kk) {
            for (int ii = 0; ii < biases.get(kk).length(); ii++) {
                biases.get(kk).putScalar(ii, (Math.random() * 0.1) - 0.05);
            }
        }
    }

    /**
     * Print debug info.
     */
    public void printDebugInfo() {
        System.out.println("---------------------------");
        System.out.println("Node Layer 1");
        System.out.println("\tActivations:" + inputLayer);
        for (int i = 0; i < activations.size(); i++) {
            System.out.println("Weight Layer " + (i + 1) + " --> " + (i + 2));
            System.out.println("\tWeights:" + net.getWeightMatrixList().get(i));
            System.out.println("Node Layer " + (i + 2));
            System.out.println("\tActivations: " + activations.get(i));
            System.out.println("\tBiases: " + biases.get(i));
            System.out.println("\tDeltas: " + deltas.get(i));
            System.out.println("\tNet inputs: " + netInputs.get(i));
            System.out.println("\tDerivatives: " + derivs.get(i));
        }
        System.out.println("MSE:" + getError());
    }

    public void printShapes() {
        System.out.println("---------------------------");
        System.out.println("Node Layer 1");
        System.out.println("\tActivations:" + Utils.shapeString(inputLayer));
        for (int i = 0; i < activations.size(); i++) {
            System.out.println("Weight Layer " + (i + 1) + " --> " + (i + 2));
            System.out.println("\tWeights:" + Utils.shapeString(net.getWeightMatrixList().get(i).getWeightMatrix()));
            System.out.println("Node Layer " + (i + 2));
            System.out.println("\tActivations: " + Utils.shapeString(activations.get(i)));
            System.out.println("\tBiases: " + Utils.shapeString(biases.get(i)));
            System.out.println("\tDeltas: " + Utils.shapeString(deltas.get(i)));
            System.out.println("\tNet inputs: " + Utils.shapeString(netInputs.get(i)));
            System.out.println("\tDerivatives: " + Utils.shapeString(derivs.get(i)));
        }
        System.out.println("MSE:" + getError());
    }

    @Override
    public void commitChanges() {
        for (int ii = 0; ii < activations.size(); ++ii) {
            for (int jj = 0; jj < neuronGroups.get(ii).size(); ++jj) {
                neuronGroups.get(ii).getNeuron(jj).forceSetActivation(activations.get(ii).getDouble(jj));
                ((BiasedUpdateRule) neuronGroups.get(ii).getNeuron(jj).getUpdateRule()).setBias(biases.get(ii).getDouble(jj));
            }
        }
    }

    /**
     * Initialize input and target datasets ND4J matrices.
     */
    public void initData() {
        // Store data as columns since that's what everything else deals with so there is no need to transpose later.
        if (network.getTrainingSet().getInputData() != null) {
            inputData = Nd4j.create(Utils.castToFloat(network.getTrainingSet().getInputData()));
        }
        if (network.getTrainingSet().getTargetData() != null) {
            targetData = Nd4j.create(Utils.castToFloat(network.getTrainingSet().getTargetData()));
        }
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    public void setMomentum(double momentum) {
        this.momentum = momentum;
    }

    public List<INDArray> getBiases() {
        return biases;
    }

    public void setUpdateMethod(UpdateMethod updateMethod) {
        this.updateMethod = updateMethod;
    }

    public BackpropNetwork getNetwork() {
        return net;
    }
}