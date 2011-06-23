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
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.Matrices;

import Jama.Matrix;

/**
 * Offline/Batch Learning with least mean squares.
 *
 * @author ztosi
 * @author jyoshimi
 */
public class LMSOffline extends Trainer {

    /**
     * Solution methods for offline LMS.
     */
    public enum SolutionType {
        /**
         * Wiener-Hopf solution.
         */
        WIENER_HOPF,

        /**
         * Moore-Penrose Solution.
         */
        MOORE_PENROSE
    };

    /** Current solution type. */
    private SolutionType solutionType = SolutionType.WIENER_HOPF;

    /**
     * Default constructor.
     *
     * @param network parent network
     */
    public LMSOffline(RootNetwork network) {
        super(network);
    }

    // TODO: At superclass some notion of iteratable or not. Cf. gauge.
    @Override
    public double train(int iteration) {
        if (solutionType == SolutionType.WIENER_HOPF) {
            weinerHopfSolution();
        } else if (solutionType == SolutionType.MOORE_PENROSE) {
            moorePenroseSolution();
        } else {
            throw new IllegalArgumentException("Solution type must be "
                    + "'MoorePenrose' or 'WeinerHopf'.");
        }
        return 0.0;
    }


    @Override
    public void init() {
    }

    /**
     * Implements the Wiener-Hopf solution to LMS linear regression.
     */
    public void weinerHopfSolution() {
        Matrix inputMatrix = new Matrix(getInputData());
        Matrix trainingMatrix = new Matrix(getTrainingData());

        trainingMatrix = inputMatrix.transpose().times(trainingMatrix);
        inputMatrix = inputMatrix.transpose().times(inputMatrix);

        inputMatrix = inputMatrix.inverse();

        double[][] wOut = inputMatrix.times(trainingMatrix).getArray();
        SimnetUtils.setWeightsFillBlanks(this.getNetwork(), getInputLayer(),
                getOutputLayer(), wOut);

        trainingMatrix = null;
        inputMatrix = null;
    }

    /**
     * Moore penrose.
     */
    public void moorePenroseSolution() {
        Matrix inputMatrix = new Matrix(getInputData());
        Matrix trainingMatrix = new Matrix(getTrainingData());

        // Computes Moore-Penrose Pseudoinverse
        inputMatrix = Matrices.pinv(inputMatrix);

        double[][] wOut = inputMatrix.times(trainingMatrix).getArray();
        SimnetUtils.setWeightsFillBlanks(this.getNetwork(), getInputLayer(),
                getOutputLayer(), wOut);
        inputMatrix = null;
        trainingMatrix = null;
    }

    /**
     * @return the solutionType
     */
    public SolutionType getSolutionType() {
        return solutionType;
    }

    /**
     * @param solutionType the solutionType to set
     */
    public void setSolutionType(SolutionType solutionType) {
        this.solutionType = solutionType;
    }

    /**
     * Testing method.
     *
     * @param args not used
     */
    public static void main(String[] args) {

        RootNetwork network = test2();
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
     * Simple AND Test
     */
    private static RootNetwork test1() {
        RootNetwork network = new RootNetwork();
        Neuron input1 = new Neuron(network, "ClampedNeuron");
        input1.setLocation(10, 70);
        input1.setIncrement(1);
        Neuron input2 = new Neuron(network, "ClampedNeuron");
        input2.setLocation(70, 70);
        input2.setIncrement(1);
        Neuron output = new Neuron(network, "LinearNeuron");
        output.setLocation(15, 0);
        network.addNeuron(input1);
        network.addNeuron(input2);
        network.addNeuron(output);
        List<Neuron> inputList = new ArrayList<Neuron>();
        inputList.add(input1);
        inputList.add(input2);
        List<Neuron> outputList = new ArrayList<Neuron>();
        outputList.add(output);

        // ConnectionLayers
        Synapse synapse = new Synapse(null, null, new ClampedSynapse());
        synapse.setLowerBound(0);
        synapse.setUpperBound(1);
        AllToAll.setBaseSynapse(synapse);
        AllToAll connection = new AllToAll(network, inputList, outputList);
        connection.connectNeurons();

        // AND Task
        double inputData[][] = { { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 } };
        double trainingData[][] = { { -1 }, { -1 }, { -1 }, { 1 } };

        // Initialize the trainer
        LMSOffline trainer = new LMSOffline(network);
        trainer.setInputData(inputData);
        trainer.setTrainingData(trainingData);
        trainer.setInputLayer(inputList);
        trainer.setOutputLayer(outputList);
        //trainer.setSolutionType(SolutionType.MOORE_PENROSE);
        trainer.setSolutionType(SolutionType.WIENER_HOPF);
        trainer.train(1);
        return network;
    }

    /**
     * Simple association test
     */
    private static RootNetwork test2() {

        RootNetwork network = new RootNetwork();

        double inputData[][] = { { .95, 0, 0, 0 }, { 0, .95, 0, 0 },
                { 0, 0, .95, 0 }, { 0, 0, 0, .95 } };
        double trainingData[][] = { { .95, 0 }, { .95, 0 }, { 0, .95 },
                { 0, .95 } };

        // Set up input layer
        List<Neuron> inputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 4; i++) {
            Neuron neuron = new Neuron(network, new ClampedNeuron());
            neuron.setLocation(10 + (i*40), 70);
            neuron.setIncrement(1);
            network.addNeuron(neuron);
            inputLayer.add(neuron);
            System.out.println("Input " + i + " = " + neuron.getId());
        }

        // Set up output layer
        List<Neuron> outputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            Neuron neuron = new Neuron(network, new LinearNeuron());
            ((BiasedNeuron)neuron.getUpdateRule()).setBias(0);
            neuron.setLocation(15 + (i*40), 0);
            neuron.setLowerBound(0);
            neuron.setUpperBound(1);
            network.addNeuron(neuron);
            //System.out.println("Output " + i + " = " + neuron.getId());
            outputLayer.add(neuron);
        }

        // Connect Layers
        Synapse synapse = new Synapse(null, null, new ClampedSynapse());
        synapse.setLowerBound(0);
        synapse.setUpperBound(1);
        AllToAll.setBaseSynapse(synapse);
        AllToAll connection = new AllToAll(network, inputLayer, outputLayer);
        connection.connectNeurons();

        // Initialize the trainer
        LMSOffline trainer = new LMSOffline(network);
        trainer.setInputData(inputData);
        trainer.setTrainingData(trainingData);
        trainer.setInputLayer(inputLayer);
        trainer.setOutputLayer(outputLayer);
        //trainer.setSolutionType(SolutionType.MOORE_PENROSE);
        trainer.setSolutionType(SolutionType.WIENER_HOPF);
        trainer.train(1);
        return network;
    }
}
