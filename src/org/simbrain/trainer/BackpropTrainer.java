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
package org.simbrain.trainer;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.interfaces.BiasedNeuron;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neurons.ClampedNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * Backprop trainer.
 *
 * @author jyoshimi
 */
public class BackpropTrainer extends Trainer {

    // TODOS:
    // Clean up methods; remove redundant code
    // Generalize derivative to other activation functions
    // Add different errors (at superclass?)
    // Deal with recurrent networks (see notes)

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
    private List<NeuronGroup> layers;

    /** A switch representing whether or not momentum is being used */
    private boolean momentum;
    
    /**
     * Initialize without a network. A call to setNetwork and init() must occur
     * before the trainer can be used.
     */
    public BackpropTrainer() {
    }

    /**
     * Construct with an existing network.
     *
     * @param network
     */
    public BackpropTrainer(Network network) {
        super(network);
    }

    @Override
    public void init() {
        errorMap = new HashMap<Neuron, Double>();
        weightDeltaMap = new HashMap<Synapse, Double>();
        biasDeltaMap = new HashMap<BiasedNeuron, Double>();
        layers = new ArrayList<NeuronGroup>();
        buildNetworkRepresentation();
    }

    /**
     * Recursively build up a representation of the network, beginning with
     * output layer.
     *
     * TODO: May be useful elsewhere; possibly refactor to a separate class.
     */
    private void buildNetworkRepresentation() {
        //System.out.println("Building network representation...");
        //System.out.println("Adding layer " + layers.size());

        if ((this.getNetwork() == null) || this.getOutputLayer() == null) {
            return;
        }
        NeuronGroup outputLayer = new NeuronGroup(this.getNetwork()
                .getRootNetwork(), this.getOutputLayer());
        layers.add(outputLayer);
        addPreviousLayer(outputLayer);
    }


    /**
     * Add the "next layer down" in the hierarchy.
     *
     * @param neuronGroup the layer whose previous layer will be added.
     */
    private void addPreviousLayer(NeuronGroup neuronGroup) {
        NeuronGroup newGroup = new NeuronGroup(this.getNetwork()
                .getRootNetwork(), Collections.EMPTY_LIST);
        int furtherConnectionCount = 0;
        for (Neuron neuron : neuronGroup.getNeuronList()) {
            for (Synapse synapse : neuron.getFanIn()) {
                newGroup.addNeuron(synapse.getSource());
                furtherConnectionCount += synapse.getSource().getFanIn().size();
            }
        }
        // System.out.println("Adding layer " + layers.size());
        layers.add(newGroup);

        // Recursive step
        if (furtherConnectionCount > 0) {
            addPreviousLayer(newGroup);
        } else {
            //System.out.println("First layer: " + newGroup.getName());
            // Sanity check. Does last layer == input layer?
        }
    }


    /**
     * Update internally constructed network.
     *
     * TODO: Confusing code. Improve.
     */
    public void updateNetwork() {
        
    }

    @Override
    public double train(int iterations) {
        for (int i = 0; i < iterations; i++) {
            iterate();
        }
        return rmsError;
    }

    /**
     * Returns the number of rows in whichever dataset has fewer rows.
     *
     * @return least number of rows
     */
    private int getMinimumNumRows() {
        int inputRows = getInputData().length;
        int targetRows = getTrainingData().length;
        if (inputRows < targetRows) {
            return inputRows;
        } else {
            return targetRows;
        }
    }

    /**
     * Iterate network training.
     */
    public void iterate() {

        rmsError = 0;

        // Set local variables
        int numRows = getMinimumNumRows();
        int numInputs = getInputLayer().size();
        //System.out.println("Data:" + numInputs + "/" + numRows);

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
        this.setCurrentError(rmsError);
        this.setIteration(this.getIteration() + 1);
    }

    /**
     * Update weights and biases.
     *
     * @param row row of training data to use for updating.
     */
    private void updateWeightBiasDeltas(int row) {
        int numOutputs = getOutputLayer().size();

        // Iterate through layers, beginning with the output layer
        //  for each layer update weight and bias deltas
        for (NeuronGroup layer : layers) {
            // Special update for output layer
            if (layer == layers.get(0)) {
                for (int i = 0; i < numOutputs; i++) {
                    // Get target neuron and compute error
                    Neuron target = getOutputLayer().get(i);
                    double targetValue = this.getTrainingData()[row][i];
                    double outputError = targetValue - target.getActivation();
                    propagateError(target, outputError);
                    rmsError += Math.pow(outputError, 2);
                    //System.out.println("Row " + row + "," + target.getId() + ":"
                    //        + targetValue + " - " + 
                    //        SimbrainMath.roundDouble(target.getActivation(),2) + " (" + 
                    //        SimbrainMath.roundDouble(outputError,2) + ")");                
                    }
            } else {
                for (Neuron neuron : layer.getNeuronList()) {

                    // Compute upstream error
                    double sumFanOutErrors = 0;
                    for (Synapse synapse : neuron.getFanOut()) {
                        Neuron outputNeuron = synapse.getTarget();
                        sumFanOutErrors += (errorMap.get(outputNeuron)
                                * synapse.getStrength());
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

    /**
     * Randomize the network associated with this trainer.
     */
    public final void randomize() {
        for (NeuronGroup layer : layers) {
            if (layer != null) {
                layer.randomizeIncomingWeights();
                layer.randomizeBiases(0, 1);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

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
        BackpropTrainer trainer = new BackpropTrainer(network);
        trainer.learningRate = .9;
        trainer.setInputLayer(inputLayer);
        trainer.setOutputLayer(outputLayer);
        trainer.setInputData(inputData);
        trainer.setTrainingData(trainingData);
        trainer.init();
        int epochs = 10;
        for (int i = 0; i < epochs; i++) {
            double error = trainer.train(1);
            System.out.println("Epoch " + i + ", error = " + error);
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

    public void setMomentum(boolean momentum) {
        this.momentum = momentum;
    }

    public boolean hasMomentum() {
        return momentum;
    }

}
