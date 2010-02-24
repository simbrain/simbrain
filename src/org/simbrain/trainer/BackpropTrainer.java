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
 *  TODO:   Add neuron groups is an option.
 *          Generalize derivative
 *          Clean up methods; remove redundant code
 *          Add different errors (at superclass?)
 *          Layered layout 
 *          Add update code to groups (?)
 *          Deal with recurrent network 
 *              - For each neuron: 
 *                     - IsContained (then recurrent)
 *                     - Is check > MAX
 *                 - InvalidNeuron = IsContained || MaxCheck
 *               - If each neuron is invalid, stop.
 *
 * @author jyoshimi
 */
public class BackpropTrainer extends Trainer {

    /** Current error. */
    private double rmsError;

    /** Learning rate. */
    private double learningRate = .1;

    /** For storing errors. */
    private HashMap<Neuron, Double> errorMap = new HashMap<Neuron, Double>();

    /** For storing weight deltas. */
    private HashMap<Synapse, Double> weightDeltaMap = new HashMap<Synapse, Double>();

    /** For storing bias deltas. */
    private HashMap<BiasedNeuron, Double> biasDeltaMap = new HashMap<BiasedNeuron, Double>();

    /** Internal representation of network. */
    private ArrayList<NeuronGroup> layers = new ArrayList<NeuronGroup>();

    /**
     * Constructor.
     *
     * @param network
     */
    public BackpropTrainer(Network network) {
        this.setNetwork(network);
    }

    /**
     * Initialize.
     */
    public void init() {
        buildNetworkRepresentation();
    }

    /**
     * Recursively build up a representation of the network, beginning with output layer.
     */
    private void buildNetworkRepresentation() {
        System.out.println("Building network representation...");
        System.out.println("Adding layer " + layers.size());

        NeuronGroup outputLayer = new NeuronGroup(this.getNetwork()
                .getRootNetwork(), this.getOutputLayer());
        outputLayer.setName("Output Layer");
        layers.add(outputLayer);
        //TODO:  A way to make this part of method below?
        this.getNetwork().getRootNetwork().addGroup(outputLayer);
        //layers.add(new HashSet<Neuron>(this.getOutputLayer()));
        addLayer(outputLayer);
    }

    /**
     * Add the "next layer down" from the given layer.
     */
    private void addLayer(NeuronGroup neuronGroup) {
        NeuronGroup newGroup = new NeuronGroup(this.getNetwork()
                .getRootNetwork(), Collections.EMPTY_LIST);
        int furtherConnectionCount = 0;
        for (Neuron neuron : neuronGroup.getNeuronList()) {
            for (Synapse synapse : neuron.getFanIn()) {
                newGroup.addNeuron(synapse.getSource());
                furtherConnectionCount += synapse.getSource().getFanIn().size();
            }
        }
        System.out.println("Adding layer " + layers.size());
        newGroup.setName("Layer" + layers.size());
        //group.setName("Layer" )
        this.getNetwork().getRootNetwork().addGroup(newGroup);
        layers.add(newGroup);

        // Recursive step
        if (furtherConnectionCount > 0) {
            addLayer(newGroup);
        } else {
            //System.out.println("First layer: " + newGroup.getName());
            // Sanity check. Does last layer == input layer?
        }
    }


    /**
     * Update internally constructed network. TODO: Confusing code. Improve.
     */
    public void updateNetwork() {
        // Update beginning with the first hidden layer.
        for (int i = layers.size() - 2; i >= 0; i--) {
            //int j = 0; System.out.println("\nUpdating group: " + layers.get(i).getName()
            //        + " iteration: " + j++);
            for (Neuron neuron : layers.get(i).getNeuronList()) {
                neuron.update();
                neuron.setActivation(neuron.getBuffer());
                //  TODO: Make a better method.  "forceUpdate" or something.
                //System.out.println(neuron.getId() + ":" + neuron.getActivation());
            }
        }
    }

    @Override
    public double train(int iterations) {
        for (int i = 0; i < iterations; i++) {
            iterate();
        }
        return rmsError;
    }

    /**
     * Iterate network training.
     */
    public void iterate() {

        rmsError = 0;

        // Set local variables
        int numRows = getInputData().length;
        int numInputs = getInputLayer().size();

        // Iterate through training data; each pass is an epoch.
        for (int row = 0; row < numRows; row++) {

            // Set activations on input layer
            for (int i = 0; i < numInputs; i++) {
                getInputLayer().get(i).setActivation(getInputData()[row][i]);
            }

            // Update network
            updateNetwork();

            // Iterate through layers and set weight and bias deltas
            setWeightBiasDeltas(row);

            // Update weights
            for (Synapse synapse : weightDeltaMap.keySet()) {
                synapse.setStrength(synapse.getStrength()
                        + weightDeltaMap.get(synapse));
            }

            // Update biases
            for (BiasedNeuron neuron : biasDeltaMap.keySet()) {
                neuron.setBias(neuron.getBias() + biasDeltaMap.get(neuron));
            }

        }

        // Update RMS error
        rmsError = Math.sqrt(rmsError / (numRows * getOutputLayer().size()));

    }

    /**
     * Update weights and biases.
     *
     * @param row row of training data to use for updating.
     */
    private void setWeightBiasDeltas(int row) {
        int numOutputs = getOutputLayer().size();
        for (NeuronGroup layer : layers) {
            // Special update for output layer
            if (layer == layers.get(0)) {
                for (int i = 0; i < numOutputs; i++) {
                    // Get target neuron and compute error
                    Neuron target = getOutputLayer().get(i);
                    double targetValue = this.getTrainingData()[row][i];
                    double outputError = targetValue - target.getActivation();
                    //System.out.println(targetValue + " |--| " + target.getActivation());
                    double error = propagateError(target, outputError);
                    rmsError += Math.pow(error, 2);
                    //TODO: Choose error, sq. error, mean of each

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

                    propagateError(neuron,sumFanOutErrors);
                }
            }
        }
    }

    /**
     * Propagate error....
     *
     * @param neuron neuron whose activation function's derivative is used
     * @param error error to multiply by (Base error)
     * @return product of derivative and error
     */
    private double propagateError(Neuron neuron, Double error) {
        // TODO:  - Add more activation functions.
        //         - Generalize to arbitrary bounds
        double errorTimesDerivative = 0;
        if (neuron instanceof SigmoidalNeuron) {
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
        if (neuron instanceof BiasedNeuron) {
            biasDeltaMap.put((BiasedNeuron) neuron,
                    learningRate * errorTimesDerivative);
        }

        return errorTimesDerivative;
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
            ClampedNeuron neuron = new ClampedNeuron();
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
            SigmoidalNeuron neuron = new SigmoidalNeuron();
            neuron.setLowerBound(0);
            network.addNeuron(neuron);
            hiddenLayer.add(neuron);
        }
        layout.setInitialLocation(new Point(10, initialYPosition - layerInterval ));
        layout.layoutNeurons(hiddenLayer);

        // Set up hidden layer 2
        List<Neuron> hiddenLayer2 = new ArrayList<Neuron>();
        for (int i = 0; i < 5; i++) {
            SigmoidalNeuron neuron = new SigmoidalNeuron();
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
            SigmoidalNeuron neuron = new SigmoidalNeuron();
            neuron.setLowerBound(0);
            network.addNeuron(neuron);
            outputLayer.add(neuron);
        }
        layout.setInitialLocation(new Point(10, initialYPosition - layerInterval*3));
        layout.layoutNeurons(outputLayer);

        // Prepare base synapse for connecting layers
        ClampedSynapse synapse = new ClampedSynapse(null, null);
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
        int epochs = 10000;
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

}
