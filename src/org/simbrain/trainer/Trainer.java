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
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.util.Utils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Superclass for all forms of supervised training algorithms. A trainer
 * combines input data, training data, and error information in a single
 * package, which can be visualized in a special GUI dialog.
 *
 * @author jyoshimi
 */
public abstract class Trainer {

    //TODO: Tools for batch training at this level?
    // train(min-error) / train to get past an error

    /**
     * Reference to the network being trained.
     */
    private Network network;

    /** Name of network, for serializing. */
    private String networkName;

    // Note that persistence only works for networks with groups, since only
    // groups (as opposed to arbitrary lists of neurons) have names

    /** Name of input layer, for serializing. */
    private String inputLayerName;

    /** Name of output layer, for serializing. */
    private String outputLayerName;

    /** Input layer. */
    private List<Neuron> inputLayer;

    /** Output layer. */
    private List<Neuron> outputLayer;

    /** Current error. */
    private double currentError;

    /** Iteration number. */
    private int iteration;

    /** Listener list. */
    private List<TrainerListener> listeners = new ArrayList<TrainerListener>();

    /**
     * Same number of columns as input network.
     * Same number of rows as training data.
     */
    private double[][] inputData;

    /**
     * Same number of columns as output network.
     * Same number of rows as training data.
     */
    private double[][] trainingData;

    /**
     * @param network parent network
     */
    public Trainer() {
    }

    /**
     * @param network parent network
     */
    public Trainer(Network network) {
        setNetwork(network);
    }

    /**
     * Initialize the trainer.
     */
    public abstract void init();

    /**
     * Train the network for specified iterations. Return error. Overrode by
     * subclasses with specific algorithms;
     *
     * @param iterations number of times to iterate
     * @return current error
     */
    public abstract double train(int iterations); // return error

    // TODO: Below, Check that input data matches input layer? But they need to
    // be set separately. Similarly for training data / training layer.
    //  Example:
    //  if (trainingData[0].length != outputLayer.size()) {
    //  System.err.println("Training data size must be the same as output layer size");
    //  return;
    //  //Throw appropriate exception
    //}

    /**
     * @return the inputData
     */
    public final double[][] getInputData() {
        return inputData;
    }

    /**
     * @param inputData the inputData to set
     */
    public final void setInputData(double[][] inputData) {
        this.inputData = inputData;
        init();
        fireInputDataChanged(inputData);
    }

    /**
     *  Use a csv file to load input data.
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
     *  Set training data.
     *
     * @param trainingData the trainingData to set
     */
    public void setTrainingData(double[][] trainingData)  {
        this.trainingData = trainingData;
        init();
        fireTrainingDataChanged(inputData);
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
     * @param inputLayer the inputLayer to set
     */
    public void setInputLayer(List<Neuron> inputLayer) {
        this.inputLayer = inputLayer;
        init();
        fireInputLayerChanged(inputLayer);
    }

    /**
     * @param inputLayer the inputLayer to set
     */
    public void setInputLayer(NeuronGroup inputGroup) {
        inputLayerName = inputGroup.getId();
        setInputLayer(inputGroup.getNeuronList());
    }

    /**
     * @return the outputLayer
     */
    public List<Neuron> getOutputLayer() {
        return outputLayer;
    }

    /**
     * @param outputLayer the outputLayer to set
     */
    public void setOutputLayer(List<Neuron> outputLayer) {
        this.outputLayer = outputLayer;
        init();
        fireOutputLayerChanged(outputLayer);
    }

    /**
     * @param outputLayer the outputLayer to set
     */
    public void setOutputLayer(NeuronGroup outputGroup) {
        outputLayerName = outputGroup.getId();
        setOutputLayer(outputGroup.getNeuronList());
    }

    /**
     * @return the network
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * @param network the network to set
     */
    public void setNetwork(Network newNetwork) {
        this.network = newNetwork;
        if (network != null) {
            networkName = network.getId();
        }
        fireNetworkChanged(network, newNetwork);
        init();
    }

    /**
     * Notify listeners that the error value has been updated.
     */
    public void fireErrorUpdated() {
        for (TrainerListener listener : listeners) {
            listener.errorUpdated(currentError);
        }
    }

    /**
     * Notify listeners that the trainer's network has changed.
     */
    private void fireNetworkChanged(Network oldNetwork, Network newNetwork) {
        for (TrainerListener listener : listeners) {
            listener.networkChanged(oldNetwork, newNetwork);
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
     * Notify listeners that input layer was changed.
     */
    private void fireInputLayerChanged(List<Neuron> newInputLayer) {
        for (TrainerListener listener : listeners) {
            listener.inputLayerChanged(newInputLayer);
        }
    }

    /**
     * Notify listeners that the output layer changed.
     */
    private void fireOutputLayerChanged(List<Neuron> outputLayer) {
        for (TrainerListener listener : listeners) {
            listener.outputLayerChanged(outputLayer);
        }
    }

    /**
     * @return the currentError
     */
    public double getCurrentError() {
        return currentError;
    }

    /**
     * @param currentError the currentError to set
     */
    public void setCurrentError(double currentError) {
        this.currentError = currentError;
        fireErrorUpdated();
    }

    /**
     * @return the iteration
     */
    public int getIteration() {
        return iteration;
    }

    /**
     * @param iteration the iteration to set
     */
    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    /**
     * Add a listener.
     *
     * @param trainerListener the listener to add
     */
    public void addListener(TrainerListener trainerListener) {
        listeners.add(trainerListener);
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
            retString += "Output Layer: " + outputLayer.size() + " neuron(s) \n";
        }
        if (inputData != null) {
            retString += "Input Data: " + inputData.length + "x" + inputData[0].length + " table \n";            
        }
        if (trainingData != null) {
            retString += "Training Data: " + trainingData.length + "x" + trainingData[0].length + "table \n";
        }
        return retString;
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(Trainer.class, "network");
        xstream.omitField(Trainer.class, "inputLayer");
        xstream.omitField(Trainer.class, "outputLayer");
        xstream.omitField(Trainer.class, "listeners");
        xstream.omitField(BackpropTrainer.class, "layers");
        xstream.omitField(BackpropTrainer.class, "errorMap");
        xstream.omitField(BackpropTrainer.class, "biasDeltaMap");
        xstream.omitField(BackpropTrainer.class, "weightDeltaMap");
        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        if (listeners == null) {
            listeners = new ArrayList<TrainerListener>();
        }
        return this;
    }

    /**
     * Set the input and output groups using the given names.
     */
    public void postAddInit() {
        if (listeners == null) {
            listeners = new ArrayList<TrainerListener>();
        }
        if (network != null) {
            NeuronGroup inputGroup = (NeuronGroup) network
                    .getGroup(inputLayerName);
            if (inputGroup != null) {
                setInputLayer(inputGroup);
            }
            NeuronGroup outputGroup = (NeuronGroup) network
                    .getGroup(outputLayerName);
            if (outputGroup != null) {
                setOutputLayer(outputGroup);
            }
            init();
        }
    }

    /**
     * @return the networkName
     */
    public String getNetworkName() {
        return networkName;
    }

    /**
     * @return the inputLayerName
     */
    public String getInputLayerName() {
        return inputLayerName;
    }

    /**
     * @return the outputLayerName
     */
    public String getOutputLayerName() {
        return outputLayerName;
    }

}
