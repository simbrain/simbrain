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

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.interfaces.BiasedNeuron;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.neurons.ClampedNeuron;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * Train using least mean squares.
 *
 * @author jyoshimi
 */
public class LMSIterative extends TrainingMethod implements IterableAlgorithm {

    /** Current error. */
    private double rmsError;

    /** Learning rate. */
    private double learningRate = .01;

    /** Iteration number. */
    private int iteration;

    @Override
    public void init(Trainer trainer) {
        iteration = 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getIteration() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public double getError() {
        return rmsError;
    }

    @Override
    public void apply(Trainer trainer) {

        rmsError = 0;

        // Set local variables
        int numRows = trainer.getInputData().length;
        int numInputs = trainer.getInputLayer().size();
        int numOutputs = trainer.getOutputLayer().size();

        // Run through training data
        for (int row = 0; row < numRows; row++) {

            // Set input layer values
            for (int i = 0; i < numInputs; i++) {
                trainer.getInputLayer().get(i).setActivation(trainer.getInputData()[row][i]);
            }

            // Update output node
            trainer.getNetwork().updateNeurons(trainer.getOutputLayer());


            // Iterate through weights and biases and update them
            for (int i = 0; i < numOutputs; i++) {

                // Get target neuron and compute error
                Neuron outputNeuron = trainer.getOutputLayer().get(i);
                double targetValue = trainer.getTrainingData()[row][i];
                double error = targetValue - outputNeuron.getActivation();
                rmsError += (error * error); // TODO: Validate rmse

                // Update weights
                for (Synapse synapse : outputNeuron.getFanIn()) {
                    double deltaW = (learningRate * error * synapse.getSource()
                            .getActivation());
                    //System.out.println(Utils.round(deltaW,2) + "=" + error + " * " + synapse.getSource()
                    //        .getActivation());
                    synapse.setStrength(synapse.getStrength() + deltaW);
                }

                //System.out.println("Row " + row + "," + outputNeuron.getId() + ":"
                //        + targetValue + " - " + 
                //        Utils.round(outputNeuron.getActivation(),2) + " (" + 
                //        Utils.round(error,2) + ")");                

                // Update bias of target neuron
                BiasedNeuron bias = (BiasedNeuron)outputNeuron.getUpdateRule();
                bias.setBias(bias.getBias() + (learningRate * error));
            }
            rmsError = Math.sqrt(rmsError / (numInputs * numOutputs));
            trainer.fireErrorUpdated();
            iteration++;
        }
    }


    @Override
    public void randomize(Trainer trainer) {
        trainer.randomizeOutputWeightsAndBiases();
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
     * Test method.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        test();
    }

    /**
     * A test with a 4-2 network and specific start weights to validate against
     * an emergent sim.
     */
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
            Neuron neuron = new Neuron(network, new ClampedNeuron()); 
            network.addNeuron(neuron);
            inputLayer.add(neuron);
            //System.out.println("Input " + i + " = " + neuron.getId());
        }

        // Set up output layer
        List<Neuron> outputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            Neuron neuron = new Neuron(network, new LinearNeuron());
            ((BiasedNeuron)neuron.getUpdateRule()).setBias(0);
            neuron.setLowerBound(0);
            neuron.setUpperBound(1);
            network.addNeuron(neuron);
            //System.out.println("Output " + i + " = " + neuron.getId());
            outputLayer.add(neuron);
        }

        // Connect input layer to output layer
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
        //REDO
//        LMSIterative trainer = new LMSIterative(network, inputLayer, outputLayer);
//        trainer.setInputData(inputData);
//        trainer.setTrainingData(trainingData);
//        int epochs = 1000; // Error gets low with 1000 epochs
//        for (int i = 0; i < epochs; i++) {
//            trainer.apply();
//            //System.out.println(network);
//            System.out.println("Epoch " + i + ", error = "
//                    + ((IterableAlgorithm) trainer).getError());
//        }
    }

    /**
     * A simple AND gate.
     */
    public static void test() {

        double inputData[][] = { { 1, 1}, {-1,1}, {1,-1},{-1,-1}};
        double trainingData[][] = { {1},{0},{0},{0}};

        // TODO: Long API! Must be shortcuts...

        // Build network
        RootNetwork network = new RootNetwork();

        // Set up input layer
        List<Neuron> inputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            Neuron neuron = new Neuron(network, new ClampedNeuron()); 
            network.addNeuron(neuron);
            inputLayer.add(neuron);
            //System.out.println("Input " + i + " = " + neuron.getId());
        }

        // Set up output layer
        List<Neuron> outputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 1; i++) {
            Neuron neuron = new Neuron(network, new LinearNeuron()); 
            ((BiasedNeuron)neuron.getUpdateRule()).setBias(0);
            neuron.setLowerBound(0);
            neuron.setUpperBound(1);
            network.addNeuron(neuron);
            //System.out.println("Output " + i + " = " + neuron.getId());
            outputLayer.add(neuron);
        }

        // Connect input layer to output layer
        AllToAll connection = new AllToAll(network, inputLayer, outputLayer);
        connection.connectNeurons();

        // Set initial weights
        network.randomizeWeights();

        // Initialize the trainer
        //REDO
//        LMSIterative trainer = new LMSIterative(network, inputLayer, outputLayer);
//        trainer.learningRate = .01;
//        trainer.setInputData(inputData);
//        trainer.setTrainingData(trainingData);
//        int epochs = 1000;
//        for (int i = 0; i < epochs; i++) {
//            trainer.apply();
//            //System.out.println(network);
//            System.out.println("Epoch " + i + ", error = "
//                    + ((IterableAlgorithm) trainer).getError());
//        }
    }

}
