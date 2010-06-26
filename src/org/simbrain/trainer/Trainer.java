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

/**
 * Superclass for all forms of supervised training algorithms. A trainer
 * combines input data, training data, and error information in a single
 * package, which can be visualized in a special gui dialog.
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

    /** Input layer. */
    private List<Neuron> inputLayer;

    /** Output layer. */
    private List<Neuron> outputLayer;

    /** Current error. */
    private double currentError;

    /** Iteration number. */
    private int iteration;

    /** Listener list. */
    public List<TrainerListener> listeners = new ArrayList<TrainerListener>();

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
    public Trainer(Network network) {
        this.network = network;
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

    /**
     * @return the inputData
     */
    public double[][] getInputData() {
        return inputData;
    }

    /**
     * @param inputData the inputData to set
     */
    public void setInputData(double[][] inputData) {
        this.inputData = inputData;
    }

    /**
     *  Use a csv file to load input data.
     *
     * @param inputFile the inputData as a csv file
     */
    public void setInputData(final File inputFile) {
        this.inputData = Utils.getDoubleMatrix(inputFile);
    }

    /**
     * @return the trainingData
     */
    public double[][] getTrainingData() {
        return trainingData;
    }

    /**
     *  Set training data.
     *
     * @param trainingData the trainingData to set
     */
    public void setTrainingData(double[][] trainingData)  {

        if (trainingData[0].length != outputLayer.size()) {
            return;
            // TODO: Throw appropriate exception
            // Exception("Training data size must be the same as output layer size");
        }
        // TODO: CHECK correct throw exception otherwise
        this.trainingData = trainingData;
    }

    /**
     * Use a csv file to load training data.
     *
     * @param trainingDataFile the training data as a .csv file
     */
    public void setTrainingData(final File trainingDataFile) {
        this.trainingData = Utils.getDoubleMatrix(trainingDataFile);
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
    }

    /**
     * Set the input layer to a whole network.
     *
     * @param inputNetwork
     */
   public void setInputLayer(Network inputNetwork) {
       this.inputLayer = inputNetwork.getFlatNeuronList();
   }

   /**
    * @param inputLayer the inputLayer to set
    */
   public void setInputLayer(NeuronGroup inputGroup) {
       this.inputLayer = inputGroup.getNeuronList(); // TODO: This should be all that's available!
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
    }

    /**
     * @param outputLayer the outputLayer to set
     */
    public void setOutputLayer(Network outputNetwork) {
        this.outputLayer = outputNetwork.getFlatNeuronList();
    }

    /**
     * @param outputLayer the outputLayer to set
     */
    public void setOutputLayer(NeuronGroup outputGroup) {
        this.outputLayer = outputGroup.getNeuronList(); // TODO: This should be all that's available!
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
    public void setNetwork(Network network) {
        this.network = network;
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

}
