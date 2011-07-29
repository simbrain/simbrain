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

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.interfaces.BiasedNeuron;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neurons.ClampedNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron;
import org.simbrain.network.synapses.ClampedSynapse;
import org.simbrain.network.util.SimnetUtils;

/**
 * Backprop trainer.
 *
 * @author jyoshimi
 */
public class Backprop extends Trainer implements IterableAlgorithm {

    /** Iteration number. */
    private int iteration;

    /** Current error. */
    private double rmsError;

    /** Default learning rate. */
    private static final double DEFAULT_LEARNING_RATE = .1;

    /** Learning rate. */
    private double learningRate = DEFAULT_LEARNING_RATE;

    /** For storing errors. */
    private HashMap<Neuron, Double> errorMap;

    /** For storing weight deltas. */
    private HashMap<Synapse, Double> weightDeltaMap;

    /** For storing bias deltas. */
    private HashMap<BiasedNeuron, Double> biasDeltaMap;

    /** Internal representation of network. */
    private List<List<Neuron>> layers;

    /**
     * Construct the trainer.
     *
     * @param network parent network
     * @param inputLayer input layer
     * @param outputLayer output layer
     */
    public Backprop(RootNetwork network, List<Neuron> inputLayer,
            List<Neuron> outputLayer) {
        super(network, inputLayer, outputLayer);
    }

    /**
     * Copy constructor.
     *
     * @param trainer trainer to copy
     */
    public Backprop(Trainer trainer) {
        super(trainer);
    }

    @Override
    public void init() {
        errorMap = new HashMap<Neuron, Double>();
        weightDeltaMap = new HashMap<Synapse, Double>();
        biasDeltaMap = new HashMap<BiasedNeuron, Double>();
        layers = SimnetUtils.getIntermedateLayers(getNetwork(),
                getInputLayer(), getOutputLayer());
        iteration = 0;
        rmsError = 0;
        //SimnetUtils.printLayers(layers);
    }

    /**
     * Update internally constructed network.
     */
    private void updateNetwork() {
        for (List<Neuron> layer : layers) {
            if (layers.indexOf(layer) != 0) {
                getNetwork().updateNeurons(layer);
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
     * @param learningRate the learningRate to set
     */
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    /**
     * Returns the number of rows in whichever dataset has fewer rows.
     *
     * @return least number of rows
     */
    private int getMinimumNumRows() {
        if ((getInputData() == null) || (getTrainingData() == null)) {
            return 0;
        }
        int inputRows = getInputData().length;
        int targetRows = getTrainingData().length;
        if (inputRows < targetRows) {
            return inputRows;
        } else {
            return targetRows;
        }
    }


    @Override
    public void apply() {

        rmsError = 0;

        // Set local variables
        int numRows = getMinimumNumRows();
        int numInputs = getInputLayer().size();
        //System.out.println("Data:" + numInputs + "/" + numRows);

        if((numRows == 0) || (numInputs == 0)) {
            return;
        }

        for (int row = 0; row < numRows; row++) {

            // Set activations on input layer
            for (int i = 0; i < numInputs; i++) {
                getInputLayer().get(i).setActivation(getInputData()[row][i]);
            }

            // Update network
            updateNetwork();

            // Update all weight and bias deltas in tables.
            //  These tables will then be used to update the weights and biases
            updateWeightBiasDeltas(row);

            // Update weights
            for (Synapse synapse : weightDeltaMap.keySet()) {
                //System.out.println(synapse.getId() + ":"
                //        + Utils.round(synapse.getStrength(), 2) + " + "
                //        + Utils.round(weightDeltaMap.get(synapse), 2));
                synapse.setStrength(synapse.getStrength()
                        + weightDeltaMap.get(synapse));
            }

            // Update biases
            for (BiasedNeuron neuronRule : biasDeltaMap.keySet()) {
                neuronRule.setBias(neuronRule.getBias() + biasDeltaMap.get(neuronRule));
            }

        }

        // Update RMS error
        rmsError = Math.sqrt(rmsError / (numRows * getOutputLayer().size()));
        iteration += this.getIteration();
    }

    /**
     * Update weights and biases.
     *
     * @param row row of training data to use for updating.
     */
    private void updateWeightBiasDeltas(int row) {
        int numOutputs = getOutputLayer().size();

        // Iterate through layers, beginning with the output layer
        // for each layer update weight and bias deltas
        for (int i = layers.size() - 1; i > 0; i--) {

            List<Neuron> layer = layers.get(i);

            // Special update for output layer
            if (i == layers.size() - 1) {
                for (int j = 0; j < numOutputs; j++) {
                    // Get target neuron and compute error
                    Neuron target = getOutputLayer().get(j);
                    double targetValue = this.getTrainingData()[row][j];
                    double outputError = targetValue - target.getActivation();
                    propagateError(target, outputError);
                    rmsError += Math.pow(outputError, 2);
                }
            } else {
                for (Neuron neuron : layer) {

                    // Compute upstream error
                    double sumFanOutErrors = 0;
                    for (Synapse synapse : neuron.getFanOut()) {
                        Neuron outputNeuron = synapse.getTarget();
                        // TODO: Why? Happens on 2layer case
                        if (errorMap.get(outputNeuron) != null) {
                            sumFanOutErrors += (errorMap.get(outputNeuron) * synapse
                                    .getStrength());
                        }
                    }
                    // Propagate errors back to previous layer
                    propagateError(neuron, sumFanOutErrors);
                }
            }
        }
    }

    /**
     * Propagate error from one neuron to those that connect to it.
     *
     * @param neuron neuron whose activation function's derivative is used
     * @param error error to multiply by (Base error)
     * @return product of derivative and error //TODO: Why? Used?
     */
    private double propagateError(Neuron neuron, Double error) {
        // TODO:  - Add more activation functions.
        //         - Generalize to arbitrary bounds
        double errorTimesDerivative = 0;
        if (neuron.getUpdateRule() instanceof SigmoidalNeuron) {
            errorTimesDerivative = neuron.getActivation()
                    * (1 - neuron.getActivation())
                    * error;
            errorMap.put(neuron, errorTimesDerivative);
        }
        // Compute and store weight deltas
        for (Synapse synapse : neuron.getFanIn()) {
            double weightDelta = learningRate * errorTimesDerivative
                    * synapse.getSource().getActivation();
            weightDeltaMap.put(synapse, weightDelta);
        }

        // Compute and store bias delta
        if (neuron.getUpdateRule() instanceof BiasedNeuron) {
            biasDeltaMap.put((BiasedNeuron) neuron.getUpdateRule(),
                    learningRate * errorTimesDerivative);
        }

        return errorTimesDerivative;
    }

    @Override
    public void randomize() {
        for (List<Neuron> layer : layers) {
            // Don't update input layer
            if (layers.indexOf(layer) > 0) {
                for (Neuron neuron : layer) {
                    neuron.randomizeFanIn();
                    if (neuron.getUpdateRule() instanceof BiasedNeuron) {
                        ((BiasedNeuron) neuron.getUpdateRule()).setBias(Math
                                .random());
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getIteration() {
        return iteration;
    }

    /**
     * {@inheritDoc}
     */
    public double getError() {
        return rmsError;
    }

    /**
     * @return the layers
     */
    public List<List<Neuron>> getLayers() {
        return layers;
    }

    /**
     * @param layers the layers to set
     */
    public void setLayers(List<List<Neuron>> layers) {
        this.layers = layers;
    }

    /**
     * Test method.
     *
     * @param args
     */
    public static void main(String[] args) {
        test();
    }

    /**
     * Test the neural network.
     */
    public static void test() {

        double inputData[][] = { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 } };
        double trainingData[][] = { { 0 }, { 1 }, { 1 }, { 0 } };

        // Build network
        RootNetwork network = new RootNetwork();

        // Layout object
        LineLayout layout = new LineLayout(50, LineOrientation.HORIZONTAL);
        int initialYPosition = 400;
        int layerInterval = 100;

        // Set up input layer
        List<Neuron> inputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            Neuron neuron = new Neuron(network, new ClampedNeuron()); 
            neuron.setIncrement(1); // For easier testing
            neuron.setLowerBound(0);
            network.addNeuron(neuron);
            inputLayer.add(neuron);
        }
        layout.setInitialLocation(new Point(10, initialYPosition));
        layout.layoutNeurons(inputLayer);

        // Set up hidden layer 1
        List<Neuron> hiddenLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 5; i++) {
            Neuron neuron = new Neuron(network, new SigmoidalNeuron());
            neuron.setLowerBound(0);
            network.addNeuron(neuron);
            hiddenLayer.add(neuron);
        }
        layout.setInitialLocation(new Point(10, initialYPosition - layerInterval ));
        layout.layoutNeurons(hiddenLayer);

        // Set up hidden layer 2
        List<Neuron> hiddenLayer2 = new ArrayList<Neuron>();
        for (int i = 0; i < 5; i++) {
            Neuron neuron = new Neuron(network, new SigmoidalNeuron());
            neuron.setLowerBound(0);
          //  neuron.setClipping(true);
            network.addNeuron(neuron);
            hiddenLayer2.add(neuron);
            //System.out.println("Hidden2-" + i + " = " + neuron.getId());
        }
        layout.setInitialLocation(new Point(10, initialYPosition - layerInterval*2));
        layout.layoutNeurons(hiddenLayer2);

        // Set up output layer
        List<Neuron> outputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 1; i++) {
            Neuron neuron = new Neuron(network, new SigmoidalNeuron());
            neuron.setLowerBound(0);
            network.addNeuron(neuron);
            outputLayer.add(neuron);
        }
        layout.setInitialLocation(new Point(10, initialYPosition - layerInterval*3));
        layout.layoutNeurons(outputLayer);

        // Prepare base synapse for connecting layers
        Synapse synapse = new Synapse(null, null, new ClampedSynapse());
        synapse.setLowerBound(-.5);
        synapse.setUpperBound(.5);

        // Connect input layer to hidden layer
        AllToAll.setBaseSynapse(synapse);
        AllToAll connection = new AllToAll(network, inputLayer, hiddenLayer);
        connection.connectNeurons();

        // Connect hidden layer to hidden layer2
        AllToAll connection2 = new AllToAll(network, hiddenLayer, hiddenLayer2);
        connection2.connectNeurons();

        // Connect hidden layer2 to output layer
        AllToAll connection3 = new AllToAll(network, hiddenLayer2, outputLayer);
        connection3.connectNeurons();

        // Randomize weights and biases
        network.randomizeWeights();
        network.randomizeBiases(-1, 1);

        // Initialize the trainer
        Backprop trainer = new Backprop(network, inputLayer, outputLayer);
        trainer.learningRate = .9;
        trainer.setInputData(inputData);
        trainer.setTrainingData(trainingData);
        trainer.init();
        int epochs = 10000; // Takes 10K iteration to get < 0.1
        for (int i = 0; i < epochs; i++) {
            trainer.apply();
            System.out.println("Epoch " + i + ", error = " + trainer.getError());
        }

        String FILE_OUTPUT_LOCATION = "./";
        File theFile = new File(FILE_OUTPUT_LOCATION + "result.xml");
        try {
            RootNetwork.getXStream().toXML(network,
                    new FileOutputStream(theFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

}
