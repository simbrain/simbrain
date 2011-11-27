/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.trainers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simbrain.network.interfaces.BiasedNeuron;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.util.Comparators;
import org.simbrain.util.ClassDescriptionPair;
import org.simbrain.util.Utils;

/**
 * Superclass for all forms of supervised training algorithms. A trainer
 * combines input data, training data, and error information in a single
 * package, which can be visualized in a special GUI dialog. Network, input, and
 * output layers are immutable. To change these you must create a new Trainer
 * object. I tried having those be mutable and many headaches ensued.
 *
 * @author jyoshimi
 */
public abstract class Trainer {

    /**
     * Reference to the network being trained.
     */
    private final Network network;

    /** Input layer. */
    private final List<Neuron> inputLayer;

    /** Output layer. */
    private final List<Neuron> outputLayer;

    /** Listener list. */
    private final List<TrainerListener> listeners = new ArrayList<TrainerListener>();

    /**
     * Same number of columns as input network. Same number of rows as training
     * data.
     */
    private double[][] inputData;

    /**
     * Same number of columns as output network. Same number of rows as training
     * data.
     */
    private double[][] trainingData;

    /** List of Trainer types. */
    private static final ClassDescriptionPair[] RULE_LIST = {
            new ClassDescriptionPair(Backprop.class, "Backprop"),
            new ClassDescriptionPair(LMSIterative.class, "LMS-Iterative"),
            new ClassDescriptionPair(LMSOffline.class, "LMS-Offline") };

    /**
     * Construct a trainer from a network, input, and output layer.
     *
     * @param network parent network
     * @param inputLayer input layer
     * @param outputLayer output layer
     */
    public Trainer(Network network, List<Neuron> inputLayer,
            List<Neuron> outputLayer) {
        this.network = network;
        this.inputLayer = inputLayer;
        this.outputLayer = outputLayer;
        // TODO: Allow for vertical sorting... Or just document this.
        Collections.sort(inputLayer, Comparators.X_ORDER);
        Collections.sort(outputLayer, Comparators.X_ORDER);

        // SimnetUtils.printLayers(SimnetUtils.getIntermedateLayers(network,
        // inputLayer, outputLayer));
    }

    /**
     * Copy constructor. NOTE: Subclasses must supply a copy constructor.
     *
     * @param trainer trainer whose data and input / output lists should be
     *            copied
     */
    public Trainer(Trainer trainer) {
        this(trainer.getNetwork(), trainer.getInputLayer(), trainer
                .getOutputLayer());
        this.setInputData(trainer.getInputData());
        this.setTrainingData(trainer.getTrainingData());
    }

    /**
     * Initialize the trainer.
     */
    public abstract void init();

    /**
     * Apply the algorithm. For iterable algorithms, this represents a single
     * iteration.
     */
    public abstract void apply();

    /**
     * Randomize the network associated with this trainer, as appropriate to the
     * algorithm.
     */
    public abstract void randomize();

    /**
     * @return the inputData
     */
    public final double[][] getInputData() {
        return inputData;
    }

    /**
     * @param newData the inputData to set
     * @throws InvalidDataException
     */
    public final void setInputData(double[][] newData) {
        if (newData != null) {
            if (newData[0].length != inputLayer.size()) {
                throw new InvalidDataException("Data mismatch: selected data has "
                        + newData[0].length + " columns; input layer has "
                        + inputLayer.size() + " neurons");
            }
            // System.out.println("Input Data: \n" +
            // Utils.doubleMatrixToString(newData));
            this.inputData = newData;
            init();
            fireInputDataChanged(inputData);
        }
    }

    /**
     * Use a csv file to load input data.
     *
     * @param inputFile the inputData as a csv file
     */
    public final void setInputData(final File inputFile) {
        setInputData(Utils.getDoubleMatrix(inputFile));
    }

    /**
     * @return the trainingData
     */
    public final double[][] getTrainingData() {
        return trainingData;
    }

    /**
     * Set training data.
     *
     * @param newData the trainingData to set
     * @throws InvalidDataException
     */
    public void setTrainingData(double[][] newData) {
        if (newData != null) {
            if (newData[0].length != outputLayer.size()) {
                throw new InvalidDataException(
                        "Data mismatch: selected data has " + newData[0].length
                                + " columns; output layer has "
                                + outputLayer.size() + " neurons");
            }
            this.trainingData = newData;
            init();
            fireTrainingDataChanged(trainingData);
            // System.out.println("Training Data: \n" +
            // Utils.doubleMatrixToString(newData));
        }
    }

    /**
     * Use a csv file to load training data.
     *
     * @param trainingDataFile the training data as a .csv file
     */
    public void setTrainingData(final File trainingDataFile) {
        setTrainingData(Utils.getDoubleMatrix(trainingDataFile));
    }

    /**
     * @return the inputLayer
     */
    public List<Neuron> getInputLayer() {
        return inputLayer;
    }

    /**
     * @return the outputLayer
     */
    public List<Neuron> getOutputLayer() {
        return outputLayer;
    }

    /**
     * @return the network
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * Notify listeners that the error value has been updated. Only makes sense
     * for iterable methods.
     */
    public void fireErrorUpdated() {
        for (TrainerListener listener : listeners) {
            listener.errorUpdated();
        }
    }

    /**
     * Notify listeners that the input data has changed
     */
    private void fireInputDataChanged(double[][] inputData) {
        for (TrainerListener listener : listeners) {
            listener.inputDataChanged(inputData);
        }
    }

    /**
     * Notify listeners that the training data was changed.
     */
    private void fireTrainingDataChanged(double[][] trainingData) {
        for (TrainerListener listener : listeners) {
            listener.trainingDataChanged(inputData);
        }
    }

    /**
     * Randomize the output layer weights and biases.
     */
    public void randomizeOutputWeightsAndBiases() {
        for (Neuron neuron : getOutputLayer()) {
            neuron.randomizeFanIn();
            if (neuron.getUpdateRule() instanceof BiasedNeuron) {
                ((BiasedNeuron) neuron.getUpdateRule()).setBias(Math.random());
            }
        }
    }

    /**
     * Add a listener.
     *
     * @param trainerListener the listener to add
     */
    public void addListener(TrainerListener trainerListener) {
        listeners.add(trainerListener);
    }

    /**
     * Returns a description of the network topology.
     *
     * @return the description
     */
    public String getTopologyDescription() {
        String retString = inputLayer.size() + " > " + outputLayer.size();
        return retString;
    }

    @Override
    public String toString() {
        String retString = "";
        if (network != null) {
            retString += "Network: " + network.getId() + "\n";
        }
        if (inputLayer != null) {
            retString += "Input Layer: " + inputLayer.size() + " neuron(s) \n";
        }
        if (outputLayer != null) {
            retString += "Output Layer: " + outputLayer.size()
                    + " neuron(s) \n";
        }
        if (inputData != null) {
            retString += "Input Data: " + inputData.length + "x"
                    + inputData[0].length + " table \n";
        }
        if (trainingData != null) {
            retString += "Training Data: " + trainingData.length + "x"
                    + trainingData[0].length + "table \n";
        }
        return retString;
    }

    /**
     * @return the ruleList
     */
    public static ClassDescriptionPair[] getRuleList() {
        return RULE_LIST;
    }

    /**
     * @return the listeners
     */
    public List<TrainerListener> getListeners() {
        return listeners;
    }

}
