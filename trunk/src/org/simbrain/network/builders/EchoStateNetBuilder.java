/*
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
package org.simbrain.network.builders;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.simbrain.network.connections.Sparse2;
import org.simbrain.network.groups.NeuronLayer;
import org.simbrain.network.groups.NeuronLayer.LayerType;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.NeuronUpdateRule;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neurons.ClampedNeuron;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron.SigmoidType;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.trainer.LMSOffline;
import org.simbrain.trainer.ReservoirComputingUtils;
import org.simbrain.trainer.LMSOffline.SolutionType;

/**
 * Builds an Echo-State Network with options for all valid weight
 * configurations.
 *
 * @author ztosi
 */
public final class EchoStateNetBuilder {

    /** The root network for the net builder */
    private final RootNetwork network;

    /** Size of the input, reservoir, and output layers */
    private int numInputNodes, numReservoirNodes, numOutputNodes;

    /**
     * ESN parameters: reservoir sparsity, sparsity of the weights from the
     * input layer to the reservoir, the sparsity of back weights from output to
     * reservoir (if they exist), and the desired spectral radius of the
     * reservoir's weight matrix.
     */
    private double resSparsity, inSparsity, backSparsity, spectralRadius;

    /** Default network has weights from the output to the reservoir */
    private boolean backWeights = true;

    /** Default network has no recurrent output weights */
    private boolean recurrentOutWeights = false;
    
    /** Default network has direct input to output connections */
    private boolean directInOutWeights = true;
    
    /** Default reservoir neuron type */
    private NeuronUpdateRule reservoirNeuronType = new LinearNeuron();

    /** Reference to input layer. */
    private List<Neuron> inputLayer;

    /** Reference to input layer. */
    private List<Neuron> reservoirLayer;

    /** Reference to input layer. */
    private List<Neuron> outputLayer;

   /** Default TANH neurons for the reservoir */
    {
        ((SigmoidalNeuron) reservoirNeuronType).setType(SigmoidType.TANH);
    }


    /** Default output neuron type */
    private NeuronUpdateRule outputNeuronType = new LinearNeuron();

    /** Initial position of network (from bottom left). */
    private Point2D.Double initialPosition = new Point2D.Double(0, 0);

    /** Initial position of reservoir grid */
    private Point2D.Double initialGridPostion;

    /** Initial position of output layer */
    private Point2D.Double initialOutPosition;

    /** space between layers */
    private int betweenLayerInterval = 100;

    /** space between neurons within layers */
    private int betweenNeuronInterval = 50;

    /**
     * Constructor with size of layers specified.
     *
     * @param network the root network wherein ESNBuilder will build components
     * @param inputNodes number of input nodes
     * @param reservoirNodes number of reservoir nodes
     * @param outputNodes number of output nodes
     */
    public EchoStateNetBuilder(final RootNetwork network, int inputNodes,
            int reservoirNodes, int outputNodes) {
        this.network = network;
        setNumInputNodes(inputNodes);
        setNumReservoirNodes(reservoirNodes);
        setNumOutputNodes(outputNodes);

    }

    /**
     * Builds an echo-state network ready to be trained based on the parameters
     * of the class. Does not create any synapses to the output layer.
     */
    public void buildNetwork() {

        // initialize the Layers
        inputLayer = initializeLayer(new ClampedNeuron(),
                LayerType.Input, numInputNodes);
        reservoirLayer = initializeLayer(reservoirNeuronType,
                LayerType.Hidden, numReservoirNodes);
        outputLayer = initializeLayer(new LinearNeuron(),
                LayerType.Output, numOutputNodes);

        // Layout layers
        // TODO: create new layout for reservoirs
        LineLayout layerLayout = new LineLayout(betweenNeuronInterval,
                LineOrientation.HORIZONTAL);
        layerLayout.setInitialLocation(new Point((int) initialPosition.getX()
                - getWidth(inputLayer) / 2, (int) initialPosition.getY()));
        layerLayout.layoutNeurons(inputLayer);

        GridLayout reservoirLayout = new GridLayout(betweenNeuronInterval,
                betweenNeuronInterval, (int) Math.sqrt(numReservoirNodes));
        initialGridPostion = new Point2D.Double((int) inputLayer.get(0).getX()
               , (int) initialPosition.getY()
                - betweenLayerInterval - (int)Math.sqrt(numReservoirNodes)*
                GridLayout.getVSpacing());
        reservoirLayout.setInitialLocation(initialGridPostion);
        reservoirLayout.layoutNeurons(reservoirLayer);

        initialOutPosition = new Point2D.Double((int) initialPosition.getX()
                - getWidth(outputLayer) / 2, (int) initialPosition.getY()
                - (2 * betweenLayerInterval + Math.sqrt(numReservoirNodes)
                        * GridLayout.getVSpacing()));
        layerLayout.setInitialLocation(initialOutPosition);
        layerLayout.layoutNeurons(outputLayer);

        // Connect layers which do not undergo training
        connectSparse(inputLayer, reservoirLayer, inSparsity);
        connectSparse(reservoirLayer, reservoirLayer, resSparsity);
        if (backWeights) {
            connectSparse(outputLayer, reservoirLayer, backSparsity);
        }

        // Scale the reservoir's weights to have the desired spectral radius
        SimnetUtils.scaleEigenvalue(reservoirLayer, reservoirLayer,
                spectralRadius);
    }

    /**
     * Initializes a layer.
     * @param nodeType
     *            type of nodes in the layer
     * @param layerType
     *            type of layer
     * @return a list of the neurons in this layer
     */
    private List<Neuron> initializeLayer(NeuronUpdateRule nodeType,
            LayerType layerType, int nodes) {

        List<Neuron> nodeList = new ArrayList<Neuron>();
        for (int i = 0; i < nodes; i++) {
            Neuron node = new Neuron(network, nodeType);
            network.addNeuron(node);
            node.setIncrement(1); // TODO: Reasonable?
            nodeList.add(node);
        }
        // Create group based on layer and add to the network
        NeuronLayer layer = new NeuronLayer(network, nodeList, layerType);
        network.addGroup(layer);

        return nodeList;
    }

    /**
     * Sparsely connects two layers (or one recurrently).
     * @param src
     *            the source layer
     * @param tar
     *            the target layer
     * @param sparsity
     *            the desired sparsity of the weights from src to tar
     */
    private void connectSparse(List<Neuron> src, List<Neuron> tar,
            double sparsity) {
        Sparse2 sparseConnections = new Sparse2(network, src, tar);
        // TODO: More elegant way to do this?
        Sparse2.setExcitatoryProbability(sparsity / 2);
        Sparse2.setInhibitoryProbability(sparsity / 2);
        sparseConnections.connectNeurons();
    }

    /**
     * Train the ESN using the provided data.
     *
     * @param inputData input data for input nodes
     * @param trainingData training data
     */
    public void train(double[][] inputData, double[][] trainingData) {

        // TODO: Check that inputData.length matches inputlayer size, similarly
        // for outputlayer and training data

        // TODO: Add methods for training to various configurations...

        // Generate the reservoir data to be used in training
        double[][] mainInputData = ReservoirComputingUtils.generateData(
                this,inputData,
                trainingData);

        //System.out.println("-------");
        //System.out.println(Utils.doubleMatrixToString(mainInputData));
        
        ArrayList<Neuron> full = new ArrayList<Neuron>();
        
        for(Neuron node : inputLayer){
            full.add(node);
        }
        for(Neuron node : reservoirLayer){
            full.add(node);
        }

        LMSOffline trainer = new LMSOffline(network);
        trainer.setInputData(mainInputData);
        trainer.setTrainingData(trainingData);
        trainer.setInputLayer(full);
        trainer.setOutputLayer(getOutputLayer());
        trainer.setSolutionType(SolutionType.WIENER_HOPF); //TODO: Ability to set
        trainer.train(1);

    }

    /**
     * @return this root network
     */
    public RootNetwork getRootNetwork() {
        return network;
    }

    /**
     * Return the width of the specified layer, in pixels.
     * @param layer
     *            layer to "measure"
     * @return width of layer
     */
    private int getWidth(List<Neuron> layer) {
        return layer.size() * betweenNeuronInterval;
    }

    /**
     * @param inputNodes
     *            number of input nodes
     */
    public void setNumInputNodes(int inputNodes) {
        this.numInputNodes = inputNodes;
    }

    public int getNumInputNodes() {
        return numInputNodes;
    }

    public void setNumReservoirNodes(int reservoirNodes) {
        this.numReservoirNodes = reservoirNodes;
    }

    public int getNumReservoirNodes() {
        return numReservoirNodes;
    }

    public void setNumOutputNodes(int outputNodes) {
        this.numOutputNodes = outputNodes;
    }

    public int getNumOutputNodes() {
        return numOutputNodes;
    }

    public void setResSparsity(double resSparsity) {
        this.resSparsity = resSparsity;
    }

    public double getResSparsity() {
        return resSparsity;
    }

    public void setInSparsity(double inSparsity) {
        this.inSparsity = inSparsity;
    }

    public double getInSparsity() {
        return inSparsity;
    }

    public void setBackSparsity(double backSparsity) {
        this.backSparsity = backSparsity;
    }

    public double getBackSparsity() {
        return backSparsity;
    }

    public void setSpectralRadius(double spectralRadius) {
        this.spectralRadius = spectralRadius;
        if (spectralRadius >= 1.0) {
            System.out.println("Warning: Setting the spectral radius to a \n"
                    + "value greater than or equal to 1 will guarantee the \n"
                    + "network has no echo-states for any left-infinite \n"
                    + "input sequences, unless certain parameters are met \n"
                    + "relating to the amplitude of the input.");
        }
    }

    public double getSpectralRadius() {
        return spectralRadius;
    }

    public void setReservoirNeuronType(NeuronUpdateRule reservoirNeuronType) {
        this.reservoirNeuronType = reservoirNeuronType;
    }

    public NeuronUpdateRule getReservoirNeuronType() {
        return reservoirNeuronType;
    }

    public void setOutputNeuronType(NeuronUpdateRule outputNeuronType) {
        this.outputNeuronType = outputNeuronType;
    }

    public NeuronUpdateRule getOutputNeuronType() {
        return outputNeuronType;
    }

    public boolean hasBackWeights() {
        return backWeights;
    }

    public void setBackWeights(boolean backWeights) {
        this.backWeights = backWeights;
    }

    /**
     * @return the inputLayer
     */
    public List<Neuron> getInputLayer() {
        return inputLayer;
    }

    /**
     * @return the reservoirLayer
     */
    public List<Neuron> getReservoirLayer() {
        return reservoirLayer;
    }

    /**
     * @return the outputLayer
     */
    public List<Neuron> getOutputLayer() {
        return outputLayer;
    }
    
    public void setRecurrentOutWeights(boolean recurrentWeights) {
        this.recurrentOutWeights = recurrentWeights;
    }

    public boolean hasRecurrentOutWeights() {
        return recurrentOutWeights;
    }

    public void setDirectInOutWeights(boolean directInOutWeights) {
        this.directInOutWeights = directInOutWeights;
    }

    public boolean hasDirectInOutWeights() {
        return directInOutWeights;
    }

    /**
     * Test the esn builder using an xor through time task.
     *
     * @param args
     */
    public static void main (String args []){
        final RootNetwork network = new RootNetwork();
        EchoStateNetBuilder esn = new EchoStateNetBuilder(network, 2, 100, 10);
        esn.setInSparsity(0.2);
        esn.setResSparsity(0.2);
        esn.setBackSparsity(0.2);
        esn.setSpectralRadius(0.8);

        esn.buildNetwork();

        // Create xor through time data
        int history = 10;
        int numInputs = history + 50000;
        double [][] preInputData = new double [history][2];
        double [][] inputData = new double [numInputs][2];
        double [][] teachData = new double [numInputs][history];
        double[] buffer = new double[history + numInputs];

        Random generator = new Random();
        for (int i = 0; i < history + numInputs; i++) {

            int truthTableRow = generator.nextInt(4);
            double[] xorVals = getXORValues(truthTableRow);

            if (i < history) {
                preInputData[i][0] = xorVals[0];
                preInputData[i][1] = xorVals[1];
                buffer[i] = xorVals[2];
            } else {
                inputData[i - history][0] = xorVals[0];
                inputData[i - history][1] = xorVals[1];
                buffer[i] = xorVals[2];
            }
        }

        // Populate teacher matrix
        for (int i = 0; i < numInputs; i++) {
            for (int j = 0; j < history; j++) {
                teachData[i][j] = buffer[i + j];
            }
        }

        // Print diagnostics
        //System.out.println(Utils.doubleMatrixToString(preInputData));
        //System.out.println("-------");
        //System.out.println(Utils.doubleMatrixToString(inputData));
        //System.out.println("-------");
        //System.out.println(Utils.doubleArrayToString(buffer));
        //System.out.println("-------");
        //System.out.println(Utils.doubleMatrixToString(teachData));

        // Train the network
        esn.train(inputData, teachData);

        // Write to file
        String FILE_OUTPUT_LOCATION = "./";
        File theFile = new File(FILE_OUTPUT_LOCATION + "esn.xml");
        try {
            RootNetwork.getXStream().toXML(network,
                    new FileOutputStream(theFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * Helper method for test function.
     *
     * @param truthTableRow one of the 4 rows an xor's truth table
     * @return the specified row: <1,1-1>,<-1,1,1>, <1,-1,1>, or <1,1,-1>
     */
    private static double[] getXORValues(int truthTableRow) {
        double[] retVal = new double[3];
        if (truthTableRow == 0) {
            retVal[0] = 1.0;
            retVal[1] = 1.0;
            retVal[2] = -1.0;
        } else if (truthTableRow == 1) {
            retVal[0] = -1.0;
            retVal[1] = 1.0;
            retVal[2] = 1.0;
        } else if (truthTableRow == 2) {
            retVal[0] = 1.0;
            retVal[1] = -1.0;
            retVal[2] = 1.0;
        } else if (truthTableRow == 3) {
            retVal[0] = -1.0;
            retVal[1] = -1.0;
            retVal[2] = -1.0;
        }
        return retVal;
    }

   

}
