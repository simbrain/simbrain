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

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;

import Jama.Matrix;

/**
 * Offline/Batch Learning with least mean sqaures
 *
 * @ Author ztosi, jyoshimi
 */

public class LMSOffline extends Trainer {

    /** Current Error. */
    //	TODO: create separate testing set, calculate rmsError. 
    // private double rmsError;

    /**
     * Accommodates multiple input layers (where input layer in this context
     * means connected to the output layer.
     **/
    private List<List<? extends Neuron>> inputLayers;

    /**
     * State Collecting Matrix. Each row is a concatenation of activation
     * vectors across the networ. More specifically , each row =
     * {input_i,...hidden_j,...,output}
     */
    private double[][] stateMatrix;

    public LMSOffline(RootNetwork network, List inputList) {
        super(network);
        inputLayers = new ArrayList<List<? extends Neuron>>();
        inputLayers.add(inputList); //Curently bypassing superclass getInputLayer()
    }

    @Override
    public double train(int iteration) {
        stateCollection();
        // TODO: Create option to use moore-penrose pseudoinverse instead of
        // weiner-hopf
        setWOut(weinerHopfSolution());
        // TODO: return rmsError. Create separate testing set?
        return 0.0;
    }

    @Override
    public void init() {
    }

    /**
     * Adds another input layer.
     *
     * @param Layers MUST be added in order of activation flow
     **/
    public void addInputLayer(List<? extends Neuron> newInputLayer) {
        inputLayers.add(newInputLayer);
    }

    /**
     * Takes in the states of each input layer and concatenates them into a
     * single 1-D array for state harvesting
     * 
     * @param _layerStates: 2-array where row index corresponds to index in the
     *            list of input layers and each array likewise stored is the
     *            state of that layer
     * @param _sNumColumns: column length of the state collecting matrix
     * @return: the appropriate state array for statMatrix
     */
    public double[] concactStates(double[][] _layerStates, int _sNumColumns) {
        double[] Srow = new double[_sNumColumns];
        int counter = 0;
        for (int i = 0; i < _layerStates.length; i++) {
            for (int j = 0; j < _layerStates[i].length; j++) {
                Srow[counter] = _layerStates[i][j];
                counter++;
            }
        }
        return Srow;
    }

    /**
     * Given a matrix of input data and teacher, data collects states of the
     * input layers in a matrix wherein rows correspond to the concatenation of
     * the states of each layer at a time index equal to their row number in
     * stateMatrix.
     */
    public void stateCollection() {

        // each row corresponds to that input layer's index in inputLayers
        // the columns the states of the neurons in their given layer
        double[][] layerStates = new double[inputLayers.size()][];

        int i = 0;

        // Populate layer list and determine sNumColumns
        int sNumRows = getInputData().length;
        int sNumColumns = 0;
        for (List<? extends Neuron> Layer : inputLayers) {
            // get overall number of columns for stateMatrix
            sNumColumns += Layer.size();
            // set column lengths for input layer states
            layerStates[i] = new double[Layer.size()];
            i++;
        }

        stateMatrix = new double[sNumRows][sNumColumns];

        // State harvesting

        // Iterate through rows of input data for training
        for (int row = 0; row < sNumRows; row++) {
            // Iterate through input layers and determine what kind of layer it
            // is
            for (List<? extends Neuron> layer : inputLayers) {
                for (int col = 0; col < layer.size(); col++) {
                    // is an input layer directly set by our input matrix
                    if (layer.get(0).getFanIn().isEmpty()
                            || layer.get(0).getFanIn() == null) {
                        layer.get(col).setActivation(getInputData()[row][col]);
                    }
                    // is a hidden layer
                    else if (!layer.equals(getOutputLayer())) {
                        layer.get(col).update();
                    }
                    // one of the input layers is the output layer
                    else {
                        // Teacher-forcing recurrent output connections
                        layer.get(col).setActivation(
                                getTrainingData()[row][col]);
                    }
                    // set values of the appropriate columns and rows of layer
                    // states
                    layerStates[inputLayers.indexOf(layer)][col] = layer.get(
                            col).getActivation();
                }

            }
            // concatenate into one array and harvest/collect
            stateMatrix[row] = concactStates(layerStates, sNumColumns);

        }
    }

    /**
     * Implements the Weiner-Hopf solution to LMS linear regression.
     *
     * @return: a weight matrix which will be used to connect any given number
     *          of input layers to the output layer.
     */
    public double[][] weinerHopfSolution() {
        Matrix S = new Matrix(stateMatrix);
        Matrix D = new Matrix(getTrainingData());

        D = S.transpose().times(D);
        S = S.transpose().times(S);

        S = S.inverse();

        double[][] wOut = S.times(D).transpose().getArray();
        D = null;
        S = null;

        return wOut;
    }

    /**
     * @param wOut
     */
    public void setWOut(double[][] wOut) {
        int index;
        for (int j = 0; j < getOutputLayer().size(); j++) {
            index = 0;
            for (List<? extends Neuron> lay : inputLayers) {
                for (int i = 0; i < lay.size(); i++) {
                    Synapse syn = new Synapse(lay.get(i), getOutputLayer().get(j));
                    this.getNetwork().addSynapse(syn);
                    syn.setStrength(wOut[j][index]);
                    index++;
                }
            }
        }
    }

    public static void main(String[] args) {
        
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
        List inputList = new ArrayList();
        inputList.add(input1);
        inputList.add(input2);
        List outputList = new ArrayList();
        outputList.add(output);


        int numCopiesData = 100;
        double inputData[][] = new double[numCopiesData*4][2];
        double trainingData[][] = new double[numCopiesData*4][1];
        for (int i = 0; i < numCopiesData * 4; i++) {
            if ((i % 4) == 1) {
                inputData[i][0] = 1;
                inputData[i][1] = 1;
                trainingData[i][0] = 1;
            } else if ((i % 4) == 2) {
                inputData[i][0] = -1;
                inputData[i][1] = 1;
                trainingData[i][0] = -1;
            } else if ((i % 4) == 3) {
                inputData[i][0] = 1;
                inputData[i][1] = -1;
                trainingData[i][0] = -1;
            } else if ((i % 4) == 4) {
                inputData[i][0] = -1;
                inputData[i][1] = -1;
                trainingData[i][0] = -1;
            }

        }

        // Initialize the trainer
        LMSOffline trainer = new LMSOffline(network, inputList);
        trainer.setInputData(inputData);
        trainer.setTrainingData(trainingData);
        trainer.setInputLayer(inputList);
        trainer.setOutputLayer(outputList);
        trainer.train(1);

        //System.out.println(network);

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

}
