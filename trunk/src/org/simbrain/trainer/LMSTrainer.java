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

import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.neurons.ClampedNeuron;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * 
 * @author jyoshimi
 */
public class LMSTrainer extends Trainer {

    // TODO
    // 
    //  Flag for bias update
    //  Make a script that create a viewable network
    //  Validate sum squared error
    // Clarify the relation to Perceptrons, Widrow-Hoff, etc.

    /** Current error. */
    private double rmsError;

    /** Learning rate. */
    private double learningRate = .01; // TODO: Make settable

    public LMSTrainer(Network network) {
        this.setNetwork(network);
    }

    @Override
    public double train(int iterations) {
        for (int i = 0; i < iterations; i++) {
            iterate();
        }
        return rmsError;
    }

    // TODO Convenience methods below?

    /**
     * Iterate network training.
     */
    public void iterate() {

        rmsError = 0;

        // Set local variables
        int numRows = getInputData().length;
        int numInputs = getInputLayer().size();
        int numOutputs = getOutputLayer().size();

        // Run through training data
        for (int row = 0; row < numRows; row++) {

            // Set input layer values
            for (int i = 0; i < numInputs; i++) {
                getInputLayer().get(i).setActivation(getInputData()[row][i]);
            }

            // Update network
            this.getNetwork().update();

            // Iterate through weights and biases and update them
            for (int i = 0; i < numOutputs; i++) {

                // Get target neuron and compute error
                Neuron target = getOutputLayer().get(i);
                double targetValue = this.getTrainingData()[row][i];
                double error = targetValue - target.getActivation();
                rmsError += (error * error); // TODO: Over computing?

                // Update weights (for backprop, use recursion here?)
		//		UpdateIncomingSynapses()
		//			if (neuron.getFanIn() == 0) break. \\but recurrent connections?  must also populate and check a list
                for (Synapse synapse : target.getFanIn()) {
                    double deltaW = (learningRate * error * synapse.getSource()
                            .getActivation());
                    synapse.setStrength(synapse.getStrength() + deltaW);
                }

                // Update bias of target neuron (TODO: Use interface?)
                ((LinearNeuron)target).setBias(learningRate * error);
            }
            rmsError = Math.sqrt(rmsError / (numInputs * numOutputs));
        }
    }

    public static void main(String[] args) {
        test();
    }

    public static void test2() {

        double inputData[][] = { { .95, 0, 0, 0 }, { 0, .95, 0, 0 },
                { 0, 0, .95, 0 }, { 0, 0, 0, .95 } };
        double trainingData[][] = { { .95, 0 }, { .95, 0 }, { 0, .95 },
                { 0, .95 } };

        // TODO: Long API! Must be shortcuts...

        // Build network
        RootNetwork network = new RootNetwork();

        // Set up input layer
        List<Neuron> inputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 4; i++) {
            ClampedNeuron neuron = new ClampedNeuron();
            network.addNeuron(neuron);
            inputLayer.add(neuron);
            System.out.println("Input " + i + " = " + neuron.getId());
        }

        // Set up output layer
        List<Neuron> outputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            LinearNeuron neuron = new LinearNeuron();
            neuron.setBias(0);
            neuron.setLowerBound(0);
            neuron.setUpperBound(1);
            network.addNeuron(neuron);
            System.out.println("Output " + i + " = " + neuron.getId());
            outputLayer.add(neuron);
        }

        // Connect input layer to output layer
        ClampedSynapse synapse = new ClampedSynapse(null, null);
        synapse.setLowerBound(0);
        synapse.setUpperBound(1);
        AllToAll.setBaseSynapse(synapse);
        AllToAll connection = new AllToAll(network, inputLayer, outputLayer);
        connection.connectNeurons();

        // Set initial weights (from an Emergent sim)
        Network.getSynapse(inputLayer.get(0), outputLayer.get(0)).setStrength(.352391);
        Network.getSynapse(inputLayer.get(1), outputLayer.get(0)).setStrength(.354468);
        Network.getSynapse(inputLayer.get(2), outputLayer.get(0)).setStrength(.338344);
        Network.getSynapse(inputLayer.get(3), outputLayer.get(0)).setStrength(.3593);
        Network.getSynapse(inputLayer.get(0), outputLayer.get(1)).setStrength(.561543);
        Network.getSynapse(inputLayer.get(1), outputLayer.get(1)).setStrength(.584706);
        Network.getSynapse(inputLayer.get(2), outputLayer.get(1)).setStrength(.355258);
        Network.getSynapse(inputLayer.get(3), outputLayer.get(1)).setStrength(.555266);

        // Initialize the trainer
        LMSTrainer trainer = new LMSTrainer(network);
        trainer.setInputLayer(inputLayer);
        trainer.setOutputLayer(outputLayer);
        trainer.setInputData(inputData);
        trainer.setTrainingData(trainingData);
        int epochs = 1;
        for (int i = 0; i < epochs; i++) {
            double error = trainer.train(1);
            System.out.println(network);
            System.out.println("Epoch " + i + ", error = " + error);
        }
    }
    
    public static void test() {

        double inputData[][] = { { 1, 0}};
        double trainingData[][] = { {1,0}};

        // TODO: Long API! Must be shortcuts...

        // Build network
        RootNetwork network = new RootNetwork();

        // Set up input layer
        List<Neuron> inputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            ClampedNeuron neuron = new ClampedNeuron();
            network.addNeuron(neuron);
            inputLayer.add(neuron);
            System.out.println("Input " + i + " = " + neuron.getId());
        }

        // Set up output layer
        List<Neuron> outputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            LinearNeuron neuron = new LinearNeuron();
            neuron.setBias(0);
            neuron.setLowerBound(0);
            neuron.setUpperBound(1);
            network.addNeuron(neuron);
            System.out.println("Output " + i + " = " + neuron.getId());
            outputLayer.add(neuron);
        }

        // Connect input layer to output layer
        ClampedSynapse synapse = new ClampedSynapse(null, null);
        synapse.setLowerBound(0);
        synapse.setUpperBound(1);
        AllToAll.setBaseSynapse(synapse);
        AllToAll connection = new AllToAll(network, inputLayer, outputLayer);
        connection.connectNeurons();

        // Set initial weights (from an Emergent sim)
        Network.getSynapse(inputLayer.get(0), outputLayer.get(0)).setStrength(.5);
        Network.getSynapse(inputLayer.get(1), outputLayer.get(0)).setStrength(.5);
        Network.getSynapse(inputLayer.get(0), outputLayer.get(1)).setStrength(.5);
        Network.getSynapse(inputLayer.get(1), outputLayer.get(1)).setStrength(.5);

        // Initialize the trainer
        LMSTrainer trainer = new LMSTrainer(network);
        trainer.learningRate = .01;
        trainer.setInputLayer(inputLayer);
        trainer.setOutputLayer(outputLayer);
        trainer.setInputData(inputData);
        trainer.setTrainingData(trainingData);
        int epochs = 1;
        for (int i = 0; i < epochs; i++) {
            double error = trainer.train(1);
            System.out.println(network);
            System.out.println("Epoch " + i + ", error = " + error);
        }
    }

}
