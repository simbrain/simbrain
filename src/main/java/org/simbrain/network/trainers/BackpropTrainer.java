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
import org.simbrain.network.matrix.NeuronArray;
import org.simbrain.network.matrix.WeightMatrix;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.util.UserParameter;
import smile.math.matrix.Matrix;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Array-backed backprop. To be supplanted by new Smile objects, but still allows backprop with "loose" neurons and old
 * style neuron groups, which has some pedagogical purpose.
 * <p>
 *
 *  NOTE: This is likely to be replaced. It is not properly implemented now. It was quickly converted from ND4J to
 *  Smile to help debug other parts of the code and facilitate design.
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
    private List<Matrix> lastWeightUpdates = new ArrayList<>();

    /**
     * Memory of last bias updates for momentum.
     */
    private List<Matrix> lastBiasUpdates = new ArrayList<>();

    /**
     * Activation vectors.
     */
    private List<Matrix> activations = new ArrayList<>();

    private List<NeuronArray> layers = new ArrayList<>();

    /**
     * Net inputs.
     */
    private List<Matrix> netInputs = new ArrayList<>();

    /**
     * Biases.
     */
    private List<Matrix> biases = new ArrayList<>();

    /**
     * Errors for a single input row.
     */
    private Matrix errors;

    /**
     * Aggregate errors for a batch of input rows.
     */
    private Matrix batchErrors;

    /**
     * Deltas on on the neurons of the network (error times derivative).
     */
    private List<Matrix> deltas = new ArrayList<Matrix>();

    /**
     * Holder for derivatives.
     */
    private List<Matrix> derivs = new ArrayList<Matrix>();

    /**
     * Input layer. Holds current input vector from input dataset. Separate for simpler indexing on other lists.
     */
    private Matrix inputLayer;

    /**
     * Current target vector.
     */
    private Matrix targetVector;

    /**
     * Inputs.
     */
    private Matrix inputData;

    /**
     * Targets.
     */
    private Matrix targetData;

    // /**
    //  * List of activation functions for easy reference.
    //  */
    // private List<TransferFunction> updateRules = new ArrayList<TransferFunction>();

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
     * Construct the backprop trainer.
     *
     * @param network the network
     * @param layers  the layers to train
     */
    public BackpropTrainer(Trainable network, List<List<Neuron>> layers) {
    }

    /**
     * Construct the trainer.
     *
     * @param network the network to train
     */
    public BackpropTrainer(BackpropNetwork network) {
        net = network;

        // Weight Matrix group list is ordered from input to output layers
        for (WeightMatrix wm : net.getModelList().get(WeightMatrix.class)) {
            lastWeightUpdates.add(new Matrix(wm.getWeightMatrix().nrows(), wm.getWeightMatrix().ncols()));
        }

        // Initialize layers
        int ii = 0;
        for (NeuronArray na : net.getModelList().get(NeuronArray.class)) {
            if (ii > 0) {
                activations.add(new Matrix(1,na.size()));
                netInputs.add(new Matrix(1, na.size()));
                deltas.add(new Matrix(1, na.size()));
                // Matrix bs = new Matrix(wm.getBiases())
                //         .reshape(1, wm.getBiases().length);
                Matrix biasVector = new Matrix(1, na.size());
                biases.add(biasVector);
                // lastBiasUpdates.add(Nd4j.zeros(bs.rows(), bs.columns()));
                lastBiasUpdates.add(biasVector.clone());
                // updateRules.add((TransferFunction) neuronGroup.getNeuronList().get(0).getUpdateRule());
                layers.add(na);
                derivs.add(new Matrix(1,na.size()));
            } else {
                inputLayer = new Matrix(1,na.size());
            }
            ii++;
        }
        errors = new Matrix(1, getOutputLayer().ncols());
        batchErrors = new Matrix(1, getOutputLayer().ncols());
        setLearningRate(DEFAULT_LEARNING_RATE);
        setMomentum(DEFAULT_MOMENTUM);
    }

    @Override
    public void apply() {
        // Apply one training step according to the currently selected update method
        mse = 0;
        int numTrainingExamples = getMinimumNumRows();
        if (updateMethod == UpdateMethod.EPOCH) {
            mse = trainRows(0, numTrainingExamples);
        } else if (updateMethod == UpdateMethod.STOCHASTIC) {
            int rowNum = ThreadLocalRandom.current().nextInt(numTrainingExamples);
            mse = trainRow(rowNum);
        } else if (updateMethod == UpdateMethod.SINGLE) {
            mse = trainRow(getIteration() % numTrainingExamples);
        }
        incrementIteration();
        getEvents().getErrorUpdated().fireAndForget(mse);
    }

    /**
     * Backpropagate error on a single row of the dataset.
     *
     * @param row Which row of the dataset to use for this update.
     * @return mean squared error of the row
     */
    private double trainRow(int row) {
        batchErrors.mul(0);
        // Get the inputs and feed them forward
        inputLayer = net.getTrainingSet().getInputs().row(new int[]{row});
        updateNetwork();
        // Backpropagate error
        targetVector = targetData.row(new int[]{row});
        targetVector = getOutputLayer().sub(errors);
        batchErrors.add(errors);
        backpropagateError();
        // Update weights and biases
        updateParameters();
        // Return the MSE for the row
        return (double) batchErrors.mul(batchErrors).sum() /getOutputLayer().size();
    }

    /**
     * Backpropagate errors for all rows in the dataset.
     *
     * @return mean squared error
     */
    private double trainRows(int firstRow, int lastRow) {
        // Get inputs and feed them forward row-by-row
        batchErrors.mul(0);
        for (int row = firstRow; row < lastRow; row++) {

            // Get the inputs and feed them forward
            inputLayer = inputData.row(new int[]{row});
            updateNetwork();
            targetVector = targetData.row(new int[]{row});
            targetVector = getOutputLayer().sub(errors);

            // Calculate batch errors
            batchErrors.add(errors);
            System.out.println(batchErrors);
        }
        batchErrors.div(lastRow - firstRow);
        // Back propagate batch errors
        backpropagateError();
        // Update weights and biases
        updateParameters();
        // Return the MSE for the batch
        return (double) batchErrors.mul(batchErrors).sum() / getOutputLayer().size();
    }

    /**
     * Update the array-based "shadow" network.
     */
    private void updateNetwork() {

        for (int i = 0; i < net.getModelList().get(WeightMatrix.class).size(); i++) {

            // Set up variables for easy reading
            Matrix netInput = netInputs.get(i);
            Matrix biasVec = biases.get(i);
            Matrix weights = net.getModelList().get(WeightMatrix.class).stream().toList().get(i).getWeightMatrix();

            // Set inputs
            Matrix inputs;
            if (i == 0) {
                inputs = inputLayer;
            } else {
                inputs = activations.get(i - 1);
            }

            // Multiply weight matrix times inputs and store in next layer netInput
            inputs = weights.mm(netInput);

            // Add biases to the net input
            netInput.add(biasVec);

            // Apply the transfer function to net input to get the activation values for the next layer and store
            // that value in the activations vector, also calculate derivatives.
            // updateRules.get(i).applyFunctionAndDerivative(netInput, activations.get(i), derivs.get(i));
        }
    }

    /**
     * Set error values using backprop (roughly output errors times intervening weights, going "backwards" from output
     * towards input).
     */
    private void backpropagateError() {
        int maxLayerIndex = activations.size() - 1;

        // Calculate output deltas from error and derivative
        batchErrors = derivs.get(maxLayerIndex).mul(deltas.get(maxLayerIndex));
        Matrix wts = net.getModelList().get(WeightMatrix.class).stream().toList().get(maxLayerIndex).getWeightMatrix();
        deltas.set(maxLayerIndex, wts.transpose().mm(deltas.get(maxLayerIndex - 1)));

        // Deltas for 2nd to last layer
        deltas.set(maxLayerIndex - 1, deltas.get(maxLayerIndex - 1).mm(derivs.get(maxLayerIndex - 1)));
        // For multiple hidden layers
        for (int layerIndex = maxLayerIndex - 1; layerIndex > 0; layerIndex--) {
            wts = net.getModelList().get(WeightMatrix.class).stream().toList().get(layerIndex).getWeightMatrix();
            deltas.set(layerIndex, wts.transpose().mm(deltas.get(layerIndex - 1)));
            deltas.set(layerIndex - 1, deltas.get(layerIndex - 1).mm(derivs.get(layerIndex - 1)));
        }
    }

    /**
     * Apply weight and bias updates.
     */
    private void updateParameters() {
        int layerIndex = 0;
        for (WeightMatrix weightMatrix : net.getModelList().get(WeightMatrix.class)) {
            Matrix wm = weightMatrix.getWeightMatrix();
            Matrix prevLayer;
            Matrix lastDeltas = lastWeightUpdates.get(layerIndex);
            Matrix lastBiasDeltas = lastBiasUpdates.get(layerIndex);

            if (layerIndex == 0) {
                prevLayer = inputLayer;
            } else {
                prevLayer = activations.get(layerIndex - 1);
            }
            Matrix biasVector = biases.get(layerIndex);
            Matrix currentLayer = activations.get(layerIndex);

            // Update weights, traversing along weight matrix in column-major order
            int kk = 0;
            for (int ii = 0; ii < prevLayer.ncols(); ii++) {
                for (int jj = 0; jj < currentLayer.ncols(); jj++) {
                    float deltaVal =
                            (float) (learningRate * deltas.get(layerIndex).get(jj, 0) * prevLayer.get(ii, 0)
                                    + (momentum * lastDeltas.get(kk, 0)));
                    wm.set(kk, 0, wm.get(kk, 0) + deltaVal);
                    lastDeltas.set(kk, 0, deltaVal);
                    kk++;
                }
            }
            // // Update biases
            // for (int ii = 0; ii < biasVector.ncols(); ii++) {
            //     float deltaVal =
            //             (float) (learningRate * deltas.get(layerIndex).getDouble(ii) + (momentum * lastBiasDeltas.getDouble(ii)));
            //     biasVector.putScalar(ii, biasVector.getDouble(ii) + deltaVal);
            //     lastBiasDeltas.putScalar(ii, deltaVal);
            // }
            weightMatrix.getEvents().getUpdated().fireAndForget();
            layerIndex++;
        }
    }

    /**
     * Helper to get output layer.
     */
    private Matrix getOutputLayer() {
        return activations.get(activations.size() - 1);
    }

    @Override
    public double getError() {
        return mse;
    }

    @Override
    public void randomize() {
        // Randomize weights
        net.getModelList().get(WeightMatrix.class).forEach(WeightMatrix::randomize);
        // Randomize biases
        // TODO: Move randomization of ndarrays to utility method
        for (int kk = 0; kk < biases.size(); ++kk) {
            for (int ii = 0; ii < biases.get(kk).ncols(); ii++) {
                biases.get(kk).set(ii, 0, (Math.random() * 0.1) - 0.05);
            }
        }
    }

    @Override
    protected MatrixDataset getTrainingSet() {
        return net.getTrainingSet();
    }

    // /**
    //  * Print debug info.
    //  */
    // public void printDebugInfo() {
    //     System.out.println("---------------------------");
    //     System.out.println("Node Layer 1");
    //     System.out.println("\tActivations:" + inputLayer);
    //     for (int i = 0; i < activations.size(); i++) {
    //         System.out.println("Weight Layer " + (i + 1) + " --> " + (i + 2));
    //         System.out.println("\tWeights:" + net.getWeightMatrixList().get(i));
    //         System.out.println("Node Layer " + (i + 2));
    //         System.out.println("\tActivations: " + activations.get(i));
    //         System.out.println("\tBiases: " + biases.get(i));
    //         System.out.println("\tDeltas: " + deltas.get(i));
    //         System.out.println("\tNet inputs: " + netInputs.get(i));
    //         System.out.println("\tDerivatives: " + derivs.get(i));
    //     }
    //     System.out.println("MSE:" + getError());
    // }
    //
    // public void printShapes() {
    //     System.out.println("---------------------------");
    //     System.out.println("Node Layer 1");
    //     System.out.println("\tActivations:" + Utils.shapeString(inputLayer));
    //     for (int i = 0; i < activations.size(); i++) {
    //         System.out.println("Weight Layer " + (i + 1) + " --> " + (i + 2));
    //         System.out.println("\tWeights:" + Utils.shapeString(net.getWeightMatrixList().get(i).getWeightMatrix()));
    //         System.out.println("Node Layer " + (i + 2));
    //         System.out.println("\tActivations: " + Utils.shapeString(activations.get(i)));
    //         System.out.println("\tBiases: " + Utils.shapeString(biases.get(i)));
    //         System.out.println("\tDeltas: " + Utils.shapeString(deltas.get(i)));
    //         System.out.println("\tNet inputs: " + Utils.shapeString(netInputs.get(i)));
    //         System.out.println("\tDerivatives: " + Utils.shapeString(derivs.get(i)));
    //     }
    //     System.out.println("MSE:" + getError());
    // }

    @Override
    public void commitChanges() {
        for (int ii = 0; ii < activations.size(); ++ii) {
            for (int jj = 0; jj < layers.get(ii).size(); ++jj) {
                layers.get(ii).setActivations(activations.get(ii));
                // TODO
                // ((BiasedUpdateRule) layers.get(ii).getNeuron(jj).getUpdateRule()).setBias(biases.get(ii).get(jj
                //         , 0));
            }
        }
    }

    /**
     * Initialize input and target datasets ND4J matrices.
     */
    public void initData() {
        // TODO
        // // Store data as columns since that's what everything else deals with so there is no need to transpose later.
        // if (network.getTrainingSet().getInputData() != null) {
        //     inputData = Nd4j.create(Utils.castToFloat(network.getTrainingSet().getInputData()));
        // }
        // if (network.getTrainingSet().getTargetData() != null) {
        //     targetData = Nd4j.create(Utils.castToFloat(network.getTrainingSet().getTargetData()));
        // }
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

    public List<Matrix> getBiases() {
        return biases;
    }

    public void setUpdateMethod(UpdateMethod updateMethod) {
        this.updateMethod = updateMethod;
    }

    public BackpropNetwork getNetwork() {
        return net;
    }
}