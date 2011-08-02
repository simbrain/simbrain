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
    private boolean directInOutWeights = false;
    
    /** Default reservoir neuron type */
    private NeuronUpdateRule reservoirNeuronType = new SigmoidalNeuron();

    /** Reference to input layer. */
    private List<Neuron> inputLayer = new ArrayList<Neuron>();

    /** Reference to input layer. */
    private List<Neuron> reservoirLayer = new ArrayList<Neuron>();

    /** Reference to input layer. */
    private List<Neuron> outputLayer = new ArrayList<Neuron>();

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
     * Default Constructor, all values are assumed default. 
     */
    public EchoStateNetBuilder(final RootNetwork network){
    	this.network = network;
    }
    
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
        initializeLayer(inputLayer, new ClampedNeuron(),
                LayerType.Input, numInputNodes);
        initializeLayer(reservoirLayer, reservoirNeuronType,
                LayerType.Hidden, numReservoirNodes);
        initializeLayer(outputLayer, outputNeuronType,
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
    private void initializeLayer(List<Neuron> layer, NeuronUpdateRule nodeType,
            LayerType layerType, int nodes) {

        for (int i = 0; i < nodes; i++) {
            Neuron node = new Neuron(network, nodeType);
            network.addNeuron(node);
            node.setIncrement(1); // TODO: Reasonable?
            layer.add(node);
        }
        // Create group based on layer and add to the network
        network.addGroup(new NeuronLayer(network, layer, layerType));
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
        
        sparseConnections.connectNeurons(sparsity);
    }

    /**
     * Train the ESN using the provided data.
     *
     * @param inputData input data for input nodes
     * @param trainingData training data
     */
    public void train(SolutionType solution, double[][] inputData, 
    		double[][] trainingData) {

        // Generate the reservoir data to be used in training
        double[][] mainInputData = ReservoirComputingUtils.generateData(
                this,inputData,
                trainingData);

        //System.out.println("-------");
        //System.out.println(Utils.doubleMatrixToString(mainInputData));
        
        ArrayList<Neuron> full = new ArrayList<Neuron>();
        
        if(directInOutWeights){
        	for(Neuron node : inputLayer){
            	full.add(node);
        	}
        }
        for(Neuron node : reservoirLayer){
            full.add(node);
        }

        if(recurrentOutWeights){
        	for(Neuron node : reservoirLayer){
                full.add(node);
            }
        }
        
        if(mainInputData[0].length != full.size()){
        	throw new IllegalArgumentException("Input data length does not " +
        			"match training node set");
        }
        
        if(trainingData[0].length != outputLayer.size()){
        	throw new IllegalArgumentException("Output data length does not " +
        			"match the number of output nodes");
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
        EchoStateNetBuilder esn = new EchoStateNetBuilder(network, 1, 100, 1);
        esn.setBackWeights(true);
        esn.setDirectInOutWeights(false);
        esn.setInSparsity(0.2);
        esn.setResSparsity(0.05);
        esn.setBackSparsity(0.2);
        esn.setSpectralRadius(0.95);

        esn.buildNetwork();

        double [][] ins = sineWaveInputGen(54000);
        double [][] outs = sineWaveDataGen(ins, 300);
                    
        esn.train(SolutionType.WIENER_HOPF,ins, outs);

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

    public static double [] [] sineWaveInputGen(int numTrials){
        double [][] input = new double [numTrials][1];
        //numTrials -> 54,000
        int interval = (int) numTrials/100;
        int counter = interval;
        double frequency = 0.01;
        for(int i = 0; i < numTrials; i++){
            input[i][0] = frequency;
             if(i > counter){
                 counter += interval;
                 frequency += 0.01;
             }
        }
        return input;
    }
    
    public static double [] [] sineWaveDataGen(double [][] inputs, int interval){
        double [][] outs = new double[inputs.length][1];
        //Currently intervals are 300
        double time = 0;
        for(int i = 0; i < inputs.length; i++){
            outs[i][0] = Math.sin(inputs[i][0] * time);
            time += 1.0;
        }
        
        return outs;
        
    }
    

   

}
