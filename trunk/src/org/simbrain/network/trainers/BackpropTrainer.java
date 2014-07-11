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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.DifferentiableUpdateRule;

/**
 * Backprop trainer. An implementation of the backpropagation learning
 * algorithm.
 *
 * @author jyoshimi
 */
public class BackpropTrainer extends IterableTrainer {

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

    /** Internal representation of network. */
    private List<List<Neuron>> layers;

    /**
     * Construct the backprop trainer.
     *
     * @param network
     *            the network
     * @param layers
     *            the layers to train
     */
    public BackpropTrainer(Trainable network, List<List<Neuron>> layers) {
        super(network);
        this.layers = layers;
        errorMap = new HashMap<Neuron, Double>();
        weightDeltaMap = new HashMap<Synapse, Double>();
        biasDeltaMap = new HashMap<Neuron, Double>();
        this.setIteration(0);
        mse = 0;
        // SimnetUtils.printLayers(layers);
    }

    // One pass through the training data
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

        network.initNetwork();
        for (int row = 0; row < numRows; row++) {

            // Set activations on input layer
            for (int i = 0; i < numInputs; i++) {
                network.getInputNeurons()
                        .get(i)
                        .forceSetActivation(
                                network.getTrainingSet().getInputData()[row][i]);
            }

            // Update network
            updateNetwork();

            // Set weight and bias deltas by backpropagating error
            backpropagateError(network, row);

            // Update weights
            for (Synapse synapse : weightDeltaMap.keySet()) {
                // System.out.println(synapse.getId() + ":"
                // + Utils.round(synapse.getStrength(), 2) + " + "
                // + Utils.round(weightDeltaMap.get(synapse), 2));
                synapse.setStrength(synapse.getStrength()
                        + weightDeltaMap.get(synapse));
            }

            // Update biases
            for (Neuron neuron : biasDeltaMap.keySet()) {
                BiasedUpdateRule biasedNeuron = (BiasedUpdateRule) (neuron
                        .getUpdateRule());
                biasedNeuron.setBias(biasedNeuron.getBias()
                        + biasDeltaMap.get(neuron));
            }

        }

        // Update MSE
        mse = mse / (numRows * network.getOutputNeurons().size());
        incrementIteration();
        fireErrorUpdated();
    }

    /**
     * Compute error contribution for all nodes using backprop algorithm.
     *
     * @param row
     *            current row of training data
     */
    private void backpropagateError(Trainable network, int row) {
        int numOutputs = network.getOutputNeurons().size();

        // Iterate through layers from the output to the input layer.
        // For each layer, update weight-deltas, bias-deltas, and errors
        for (int i = layers.size() - 1; i > 0; i--) {

            List<Neuron> layer = layers.get(i);

            // Special update for output layer
            if (i == layers.size() - 1) {
                for (int j = 0; j < numOutputs; j++) {
                    Neuron outputNeuron = network.getOutputNeurons().get(j);
                    double targetValue = network.getTrainingSet()
                            .getTargetData()[row][j];
                    double outputError = targetValue
                            - outputNeuron.getActivation();
                    storeErrorAndDeltas(outputNeuron, outputError);
                    mse += Math.pow(outputError, 2);
                }
            } else {
                for (Neuron hiddenLayerNeuron : layer) {

                    // Compute sum of fan-out errors on this neuron
                    double sumFanOutErrors = 0;
                    for (Synapse synapse : hiddenLayerNeuron.getFanOut()
                            .values()) {
                        Neuron nextLayerNeuron = synapse.getTarget();
                        // TODO: Why do I need this check?
                        // Happens on 2 layer case?
                        if (errorMap.get(nextLayerNeuron) != null) {
                            sumFanOutErrors += (errorMap.get(nextLayerNeuron) * synapse
                                    .getStrength());
                        }
                    }
                    // Compute errors and deltas for previous layer
                    storeErrorAndDeltas(hiddenLayerNeuron, sumFanOutErrors);
                }
            }
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
            weightDeltaMap.put(synapse, weightDelta);
        }

        // Compute and store bias delta for this neuron
        if (neuron.getUpdateRule() instanceof BiasedUpdateRule) {
            biasDeltaMap.put(neuron, learningRate * errorSignal);
        }
    }

    /**
     * Randomize the network.
     */
    public void randomize() {
        for (List<Neuron> layer : layers) {
            // Don't update input layer
            if (layers.indexOf(layer) > 0) {
                randomize(layer);
            }
        }
    }

    /**
     * Randomize the specified layer.
     *
     * @param layer
     *            the layer to randomize
     */
    protected void randomize(List<Neuron> layer) {
        for (Neuron neuron : layer) {
            neuron.clear(); // Looks nicer in the GUI
            neuron.randomizeFanIn();
            neuron.randomizeBias(-.5, .5);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getError() {
        return mse;
    }

    /**
     * Update internally constructed network.
     */
    protected void updateNetwork() {
        for (List<Neuron> layer : layers) {
            if (layers.indexOf(layer) != 0) {
                Network.updateNeurons(layer);
            }
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

    // /**
    // * Test method.
    // *
    // * @param args
    // */
    // public static void main(String[] args) {
    // test();
    // }
    //
    // /**
    // * Test the neural network.
    // */
    // public static void test() {
    //
    // double inputData[][] = { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 } };
    // double trainingData[][] = { { 0 }, { 1 }, { 1 }, { 0 } };
    //
    // // Build network
    // Network network = new Network();
    //
    // // Layout object
    // LineLayout layout = new LineLayout(50, LineOrientation.HORIZONTAL);
    // int initialYPosition = 400;
    // int layerInterval = 100;
    //
    // // Set up input layer
    // List<Neuron> inputLayer = new ArrayList<Neuron>();
    // for (int i = 0; i < 2; i++) {
    // Neuron neuron = new Neuron(network, new LinearRule());
    // neuron.getUpdateRule().setIncrement(1); // For easier testing
    // network.addNeuron(neuron);
    // inputLayer.add(neuron);
    // }
    // layout.setInitialLocation(new Point(10, initialYPosition));
    // layout.layoutNeurons(inputLayer);
    //
    // // Set up hidden layer 1
    // List<Neuron> hiddenLayer = new ArrayList<Neuron>();
    // for (int i = 0; i < 5; i++) {
    // Neuron neuron = new Neuron(network, new SigmoidalRule());
    // network.addNeuron(neuron);
    // hiddenLayer.add(neuron);
    // }
    // layout.setInitialLocation(new Point(10, initialYPosition
    // - layerInterval));
    // layout.layoutNeurons(hiddenLayer);
    //
    // // Set up hidden layer 2
    // List<Neuron> hiddenLayer2 = new ArrayList<Neuron>();
    // for (int i = 0; i < 5; i++) {
    // Neuron neuron = new Neuron(network, new SigmoidalRule());
    // network.addNeuron(neuron);
    // hiddenLayer2.add(neuron);
    // // System.out.println("Hidden2-" + i + " = " + neuron.getId());
    // }
    // layout.setInitialLocation(new Point(10, initialYPosition
    // - layerInterval * 2));
    // layout.layoutNeurons(hiddenLayer2);
    //
    // // Set up output layer
    // List<Neuron> outputLayer = new ArrayList<Neuron>();
    // for (int i = 0; i < 1; i++) {
    // Neuron neuron = new Neuron(network, new SigmoidalRule());
    // network.addNeuron(neuron);
    // outputLayer.add(neuron);
    // }
    // layout.setInitialLocation(new Point(10, initialYPosition
    // - layerInterval * 3));
    // layout.layoutNeurons(outputLayer);
    //
    // // Connect input layer to hidden layer
    // AllToAll connection = new AllToAll(network, inputLayer, hiddenLayer);
    // connection.connectNeurons(true);
    //
    // // Connect hidden layer to hidden layer2
    // AllToAll connection2 = new AllToAll(network, hiddenLayer, hiddenLayer2);
    // connection2.connectNeurons(true);
    //
    // // Connect hidden layer2 to output layer
    // AllToAll connection3 = new AllToAll(network, hiddenLayer2, outputLayer);
    // connection3.connectNeurons(true);
    //
    // // Randomize weights and biases
    // network.randomizeWeights();
    // network.randomizeBiases(-.5, .5);
    //
    // // Initialize the trainer
    // // REDO
    // // Backprop trainer = new Backprop(network, inputLayer, outputLayer);
    // // network.setLearningRate(.9);
    // // trainer.setInputData(inputData);
    // // trainer.setTrainingData(trainingData);
    // // trainer.init();
    // // int epochs = 10000; // Takes 10K iteration to get < 0.1
    // // for (int i = 0; i < epochs; i++) {
    // // trainer.apply();
    // // System.out.println("Epoch " + i + ", error = " + trainer.getError());
    // // }
    // //
    // // String FILE_OUTPUT_LOCATION = "./";
    // // File theFile = new File(FILE_OUTPUT_LOCATION + "result.xml");
    // // try {
    // // Network.getXStream().toXML(network,
    // // new FileOutputStream(theFile));
    // // } catch (FileNotFoundException e) {
    // // e.printStackTrace();
    // // }
    //
    // }

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
