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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.neuron_update_rules.BiasedUpdateRule;
import org.simbrain.network.neuron_update_rules.ClampedNeuronRule;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.Matrices;
import org.simbrain.util.propertyeditor.ComboBoxWrapper;

import Jama.Matrix;

/**
 * Offline/Batch Learning with least mean squares.
 *
 * @author ztosi
 * @author jyoshimi
 */
public class LMSOffline extends Trainer {

    /** Current solution type. */
    private SolutionType solutionType = SolutionType.WIENER_HOPF;

    /** Whether or not ridge regression is to be performed. */
    private boolean ridgeRegression;

    /** The magnitude of the ridge regression. */
    private double alpha;

    /**
     * Construct the LMSOOffline object, with a trainable network the Synapse
     * group where the new synapses will be placed.
     *
     * @param network the network to train
     */
    public LMSOffline(Trainable network) {
        super(network);
    }

    /**
     * Solution methods for offline LMS.
     */
    public enum SolutionType {
        /**
         * Wiener-Hopf solution.
         */
        WIENER_HOPF {
            @Override
            public String toString() {
                return "Wiener-Hopf";
            }
        },

        /**
         * Moore-Penrose Solution.
         */
        MOORE_PENROSE {
            @Override
            public String toString() {
                return "Moore-Penrose";
            }
        }

    };

    @Override
    public void apply() {

        fireTrainingBegin();

        int index = 0;
        for (Neuron n : network.getOutputNeurons()) {
            if (n.getUpdateRule() instanceof SigmoidalRule) {
                for (int i = 0; i < network.getTrainingData().length; i++) {
                    network.getTrainingData()[i][index] = ((SigmoidalRule) n
                            .getUpdateRule()).getInverse(
                            network.getTrainingData()[i][index], n);
                }
            }
            index++;
        }

        if (solutionType == SolutionType.WIENER_HOPF) {
            weinerHopfSolution(network);
        } else if (solutionType == SolutionType.MOORE_PENROSE) {
            moorePenroseSolution(network);
        } else {
            throw new IllegalArgumentException("Solution type must be "
                    + "'MoorePenrose' or 'WeinerHopf'.");
        }

        fireTrainingEnd();

    }

    /**
     * Implements the Wiener-Hopf solution to LMS linear regression.
     */
    public void weinerHopfSolution(Trainable network) {
        Matrix inputMatrix = new Matrix(network.getInputData());
        Matrix trainingMatrix = new Matrix(network.getTrainingData());

        fireProgressUpdate("Correlating State Matrix (R = S'S)...", 0);
        trainingMatrix = inputMatrix.transpose().times(trainingMatrix);

        fireProgressUpdate(
                "Cross-Correlating States with Teacher data (P = S'D)...", 15);
        inputMatrix = inputMatrix.transpose().times(inputMatrix);

        fireProgressUpdate("Computing Inverse Correlation Matrix...", 30);
        try {

            if (ridgeRegression) {
                Matrix scaledIdentity = Matrix.identity(
                        inputMatrix.getRowDimension(),
                        inputMatrix.getColumnDimension()).times(alpha * alpha);
                inputMatrix = inputMatrix.plus(scaledIdentity);
            }

            inputMatrix = inputMatrix.inverse();

            fireProgressUpdate("Computing Weights...", 80);
            double[][] wOut = inputMatrix.times(trainingMatrix).getArray();
            fireProgressUpdate("Setting Weights...", 95);
            SimnetUtils.setWeights(network.getInputNeurons(),
                    network.getOutputNeurons(), wOut);
            fireProgressUpdate("Done!", 100);

            // TODO: What error does JAMA actually throw for singular Matrices?
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(new JFrame(), ""
                    + "State Correlation Matrix is Singular",
                    "Training Failed", JOptionPane.ERROR_MESSAGE);
            fireProgressUpdate("Training Failed", 0);
        }

        trainingMatrix = null;
        inputMatrix = null;
    }

    /**
     * Moore penrose.
     */
    public void moorePenroseSolution(Trainable network) {
        Matrix inputMatrix = new Matrix(network.getInputData());
        Matrix trainingMatrix = new Matrix(network.getTrainingData());

        fireProgressUpdate("Computing Moore-Penrose Pseudoinverse...", 0);
        // Computes Moore-Penrose Pseudoinverse
        inputMatrix = Matrices.pinv(inputMatrix);

        fireProgressUpdate("Computing Weights...", 50);
        double[][] wOut = inputMatrix.times(trainingMatrix).getArray();

        fireProgressUpdate("Setting Weights...", 75);
        SimnetUtils.setWeights(network.getInputNeurons(),
                network.getOutputNeurons(), wOut);
        fireProgressUpdate("Done!", 100);

        inputMatrix = null;
        trainingMatrix = null;
    }

    /**
     * Set solution type.
     *
     * @param solutionType the solutionType to set
     */
    public void setSolutionType(SolutionType solutionType) {
        this.solutionType = solutionType;
    }

    /**
     * Returns the current solution type inside a comboboxwrapper. Used by
     * preference dialog.
     *
     * @return the the comboBox
     */
    public ComboBoxWrapper getSolutionType() {
        return new ComboBoxWrapper() {
            public Object getCurrentObject() {
                return solutionType;
            }

            public Object[] getObjects() {
                return SolutionType.values();
            }
        };
    }

    /**
     * Set the current parse style. Used by preference dialog.
     *
     * @param solutionType the current solution.
     */
    public void setSolutionType(ComboBoxWrapper solutionType) {
        setSolutionType((SolutionType) solutionType.getCurrentObject());
    }

    public boolean isRidgeRegression() {
        return ridgeRegression;
    }

    public void setRidgeRegression(boolean ridgeRegression) {
        this.ridgeRegression = ridgeRegression;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    /**
     * Main method for testing.
     *
     * @param args not used
     */
    public static void main(String[] args) {

        Network network = test2();
        // System.out.println(network);

        // Write to file
        String FILE_OUTPUT_LOCATION = "./";
        File theFile = new File(FILE_OUTPUT_LOCATION + "result.xml");
        try {
            Network.getXStream().toXML(network, new FileOutputStream(theFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * Simple AND Test
     */
    private static Network test1() {
        Network network = new Network();
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
        AllToAll connection = new AllToAll(network, inputList, outputList);
        connection.connectNeurons();

        // AND Task
        double inputData[][] = { { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 } };
        double trainingData[][] = { { -1 }, { -1 }, { -1 }, { 1 } };

        // Initialize the trainer
        // REDO
        // LMSOffline trainer = new LMSOffline(network, inputList, outputList);
        // trainer.setInputData(inputData);
        // trainer.setTrainingData(trainingData);
        // //trainer.setSolutionType(SolutionType.MOORE_PENROSE);
        // trainer.setSolutionType(SolutionType.WIENER_HOPF);
        // trainer.apply();
        return network;
    }

    /**
     * Simple association test
     */
    private static Network test2() {

        Network network = new Network();

        double inputData[][] = { { .95, 0, 0, 0 }, { 0, .95, 0, 0 },
                { 0, 0, .95, 0 }, { 0, 0, 0, .95 } };
        double trainingData[][] = { { .95, 0 }, { .95, 0 }, { 0, .95 },
                { 0, .95 } };

        // Set up input layer
        List<Neuron> inputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 4; i++) {
            Neuron neuron = new Neuron(network, new ClampedNeuronRule());
            neuron.setLocation(10 + (i * 40), 70);
            neuron.setIncrement(1);
            network.addNeuron(neuron);
            inputLayer.add(neuron);
            System.out.println("Input " + i + " = " + neuron.getId());
        }

        // Set up output layer
        List<Neuron> outputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            Neuron neuron = new Neuron(network, new LinearRule());
            ((BiasedUpdateRule) neuron.getUpdateRule()).setBias(0);
            neuron.setLocation(15 + (i * 40), 0);
            neuron.setLowerBound(0);
            neuron.setUpperBound(1);
            network.addNeuron(neuron);
            // System.out.println("Output " + i + " = " + neuron.getId());
            outputLayer.add(neuron);
        }

        // Connect Layers
        AllToAll connection = new AllToAll(network, inputLayer, outputLayer);
        connection.connectNeurons();

        // Initialize the trainer
        // REDO
        // LMSOffline trainer = new LMSOffline(network, inputLayer,
        // outputLayer);
        // trainer.setInputData(inputData);
        // trainer.setTrainingData(trainingData);
        // //trainer.setSolutionType(SolutionType.MOORE_PENROSE);
        // trainer.setSolutionType(SolutionType.WIENER_HOPF);
        // trainer.apply();
        return network;
    }

}
