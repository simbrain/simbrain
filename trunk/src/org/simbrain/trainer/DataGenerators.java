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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.interfaces.BiasedNeuron;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.neurons.ClampedNeuron;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.synapses.ClampedSynapse;
import org.simbrain.trainer.LMSOffline.SolutionType;
import org.simbrain.util.Utils;

/**
 * Provides static methods for generating input to be used in Trainers.
 * Currently provides methods for creating input data for reservoir networks, by
 * updating the states of a reservoir and then adding those states to a matrix.
 *
 * @author jyoshimi
 * @author ztosi
 */
public class DataGenerators {

    // TODO: These methods may not work if more complex network updating is
    // required
    // when setting the reservoir states.

    /**
     * Create data to be used in training a reservoir based network, as follows:
     * 1) Iterate through each row of the input data and use it to set the input
     * nodes 2) Update the reservoir 3) Concatenate the input and reservoir
     * states in to one state, one row of the return matrix.
     *
     * @param network root network
     * @param inputNeurons input neurons
     * @param inputData data for input neurons
     * @param reservoirNeurons reservoir neurons
     * @return a matrix of data to be used in training. Each row is an input +
     *         reservoir state.
     */
    public static double[][] generateCombinedInputReservoirData(
            RootNetwork network, List<Neuron> inputNeurons,
            double[][] inputData, List<Neuron> reservoirNeurons) {

        // / Return matrix
        double[][] returnMatrix = new double[inputData.length][inputNeurons
                .size() + reservoirNeurons.size()];

        // Iterate through each row of the input data
        for (int row = 0; row < inputData.length; row++) {

            // Clamp the input nodes and add their values to the current row of
            // the return matrix
            int col = 0;
            for (Neuron neuron : inputNeurons) {
                double clampValue = inputData[row][col];
                neuron.setActivation(clampValue);
                returnMatrix[row][col] = neuron.getActivation();
                col++;
            }

            // Update reservoir and add the resulting values to the
            // return matrix
            network.updateNeurons(reservoirNeurons);
            for (Neuron neuron : reservoirNeurons) {
                returnMatrix[row][col] = neuron.getActivation();
                col++;
            }

            // TODO?: Teacher-forcing recurrent output connections
        }

        //System.out.println(Utils.doubleMatrixToString(returnMatrix));

        return returnMatrix;

    }

    /**
     * Create data to be used in training a reservoir based network, as follows:
     * 1) Iterate through each row of the input data and use it to set the input
     * nodes 2) Update the reservoir 3) Add the reservoir state as one row of
     * the return matrix.
     *
     * @param network root network
     * @param inputNeurons input neurons
     * @param inputData data for input neurons
     * @param reservoirNeurons reservoir neurons
     * @return a matrix of data to be used in training. Each row is an input +
     *         reservoir state.
     */
    public static double[][] generateReservoirData(RootNetwork network,
            List<Neuron> inputNeurons, double[][] inputData,
            List<Neuron> reservoirNeurons) {

        // / Return matrix
        double[][] returnMatrix = new double[inputData.length][reservoirNeurons
                .size()];

        // Iterate through each row of the input data
        for (int row = 0; row < inputData.length; row++) {

            // Clamp the input nodes using the input data.
            int col = 0;
            for (Neuron neuron : inputNeurons) {
                double clampValue = inputData[row][col];
                neuron.setActivation(clampValue);
                col++;
            }

            // Update reservoir nodes and add the resulting values to the
            // return matrix
            col = 0;
            network.updateNeurons(reservoirNeurons);
            for (Neuron neuron : reservoirNeurons) {
                returnMatrix[row][col] = neuron.getActivation();
                col++;
            }

        }

        //System.out.println(Utils.doubleMatrixToString(returnMatrix));

        return returnMatrix;

    }

    /**
     * Test methods.
     *
     * @param args not used
     */
    public static void main(String[] args) {

        RootNetwork network = testReservoirNetwork();
        System.out.println(network);

        // Write to file
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
     * Train a simple reservoir network using the data generator and LMSOffline.
     *
     * @return trained root network
     */
    private static RootNetwork testReservoirNetwork() {

        RootNetwork network = new RootNetwork();

        // "Input" data, to be expanded to use hidden layer units as well
        double clampData[][] = { { .95, 0, 0, 0 }, { 0, .95, 0, 0 },
                { 0, 0, .95, 0 }, { 0, 0, 0, .95 } };

        // Training data
        double trainingData[][] = { { .95, 0 }, { .95, 0 }, { 0, .95 },
                { 0, .95 } };

        // Set up clamped layer
        List<Neuron> clampedLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 4; i++) {
            Neuron neuron = new Neuron(network, new ClampedNeuron());
            neuron.setLocation(10 + (i * 40), 70);
            neuron.setIncrement(1);
            network.addNeuron(neuron);
            clampedLayer.add(neuron);
            // System.out.println("Input " + i + " = " + neuron.getId());
        }

        // Set up hidden layer
        List<Neuron> hiddenLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            Neuron neuron = new Neuron(network, new LinearNeuron());
            neuron.setLocation(200 + (i * 40), 35);
            neuron.setIncrement(1);
            network.addNeuron(neuron);
            hiddenLayer.add(neuron);
            // System.out.println("Input " + i + " = " + neuron.getId());
        }

        // Set up output layer
        List<Neuron> outputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            Neuron neuron = new Neuron(network, new LinearNeuron());
            ((BiasedNeuron) neuron.getUpdateRule()).setBias(0);
            neuron.setLocation(15 + (i * 40), 0);
            neuron.setLowerBound(0);
            neuron.setUpperBound(1);
            network.addNeuron(neuron);
            // System.out.println("Output " + i + " = " + neuron.getId());
            outputLayer.add(neuron);
        }

        // Connect clamped to hidden layer
        Synapse synapse = new Synapse(null, null, new ClampedSynapse());
        synapse.setLowerBound(0);
        synapse.setUpperBound(1);
        AllToAll.setBaseSynapse(synapse);
        AllToAll connection = new AllToAll(network, clampedLayer, outputLayer);
        connection.connectNeurons();

        // Connect clamped to output layer
        synapse = new Synapse(null, null, new ClampedSynapse());
        synapse.setLowerBound(0);
        synapse.setUpperBound(1);
        AllToAll.setBaseSynapse(synapse);
        AllToAll connection2 = new AllToAll(network, clampedLayer, hiddenLayer);
        connection2.connectNeurons();

        // Connect hidden to output layer
        synapse = new Synapse(null, null, new ClampedSynapse());
        synapse.setLowerBound(0);
        synapse.setUpperBound(1);
        AllToAll.setBaseSynapse(synapse);
        AllToAll connection3 = new AllToAll(network, hiddenLayer, outputLayer);
        connection3.connectNeurons();

        // Randomize synapses
        network.randomizeWeights();

        // Create input data for trainer
        double[][] inputData = DataGenerators
                .generateCombinedInputReservoirData(network, clampedLayer,
                        clampData, hiddenLayer);

        // Create input layer for trainer (clamped + hidden)
        List<Neuron> inputLayer = new ArrayList<Neuron>();
        for (Neuron neuron : clampedLayer) {
            inputLayer.add(neuron);
        }
        for (Neuron neuron : hiddenLayer) {
            inputLayer.add(neuron);
        }

        // Initialize the trainer (comment / uncomment below for different
        // configurations)
        LMSOffline trainer = new LMSOffline(network);
        // LMSTrainer trainer = new LMSTrainer(network);
        trainer.setInputData(inputData);
        trainer.setTrainingData(trainingData);
        trainer.setInputLayer(inputLayer);
        trainer.setOutputLayer(outputLayer);
        trainer.setSolutionType(SolutionType.MOORE_PENROSE);
        trainer.train(1);
        // trainer.train(1000);
        return network;
    }

}
