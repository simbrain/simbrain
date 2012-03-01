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
package org.simbrain.network.groups.subnetworks;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
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
import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.LMSOffline.SolutionType;
import org.simbrain.network.trainers.ReservoirComputingUtils;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.network.util.NetworkLayoutManager;
import org.simbrain.network.util.NetworkLayoutManager.Direction;
import org.simbrain.network.util.SimnetUtils;

/**
 * Builds an Echo-State Network with options for all valid weight
 * configurations.
 *
 * @author ztosi
 */
public class EchoStateNetwork extends Subnetwork {

    /** Number of input nodes, reservoirNodes, and output nodes. */
    private int numInputs, numResNodes, numOutputs;
    
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
    private boolean recurrentOutWeights;

    /** Default network has direct input to output connections */
    private boolean directInOutWeights;

    /** Default reservoir neuron type */
    private NeuronUpdateRule reservoirNeuronType = new SigmoidalNeuron();

    /** Reference to input layer. */
    private NeuronGroup inputLayer;

    /** Reference to input layer. */
    private NeuronGroup reservoirLayer;

    /** Reference to input layer. */
    private NeuronGroup outputLayer;

   /** Default TANH neurons for the reservoir */
    {
        ((SigmoidalNeuron) reservoirNeuronType).setType(SigmoidType.TANH);
    }


    /** Default output neuron type */
    private NeuronUpdateRule outputNeuronType = new LinearNeuron();

    /** Initial position of network (from bottom left). */
    private Point2D initialPosition;

    /** Default space between layers */
    private static final int DEFAULT_LAYER_INTERVAL = 100;

    /** space between layers */
    private int betweenLayerInterval = DEFAULT_LAYER_INTERVAL;

    /** Default space between layers */
    private static final int DEFAULT_NEURON_INTERVAL = 50;

    /** space between neurons within layers */
    private int betweenNeuronInterval = DEFAULT_NEURON_INTERVAL;

    /** Noise. */
    private boolean noise;
    
    /** Max noise. */
    private double noiseMax;
    
    /** Min noise. */
    private double noiseMin;
    
    private Trainer trainer;
    
    /**
     * Constructor with size of layers specified.
     *
     * @param network the root network wherein ESNBuilder will build components
     * @param inputNodes number of input nodes
     * @param reservoirNodes number of reservoir nodes
     * @param outputNodes number of output nodes
     */
    public EchoStateNetwork(final RootNetwork network, int inputNodes,
            int reservoirNodes, int outputNodes, Point2D initialPosition) {
        super(network);
        this.initialPosition = initialPosition;
        this.numInputs = inputNodes;
        this.numResNodes = reservoirNodes;
        this.numOutputs = outputNodes;
        setLabel("Echo State Network");
    }
    
    @Override
    public void update() {
        inputLayer.update();
        reservoirLayer.update();
        outputLayer.update();
    }

    /**
     * Builds an echo-state network ready to be trained based on the parameters
     * of the class. Does not create any synapses to the output layer.
     */
    public void buildNetwork() {

        // initialize the Layers
        List<Neuron> inputLayerNeurons  = new ArrayList<Neuron>();
        List<Neuron> reservoirLayerNeurons  = new ArrayList<Neuron>();
        List<Neuron> outputLayerNeurons  = new ArrayList<Neuron>();
        initializeLayer(inputLayerNeurons, new ClampedNeuron(),
                numInputs);
        initializeLayer(reservoirLayerNeurons, reservoirNeuronType,
                numResNodes);
        initializeLayer(outputLayerNeurons, outputNeuronType,
                numOutputs);
        
        Sparse connector = new Sparse();
        connector.setEnableExRand(true);
        connector.setEnableInRand(true);
        
        LineLayout lineLayout = new LineLayout(betweenNeuronInterval,
                LineOrientation.HORIZONTAL);
        

        // Input Layer
        lineLayout.layoutNeurons(inputLayerNeurons);
        inputLayer = new NeuronGroup(getParentNetwork(), inputLayerNeurons);
        inputLayer.setLabel("Inputs");
        addNeuronGroup(inputLayer);

        
        // Reservoir Layer
        GridLayout gridLayout = new GridLayout(betweenNeuronInterval,
                betweenNeuronInterval, (int) Math.sqrt(numResNodes));
        gridLayout.layoutNeurons(reservoirLayerNeurons);
        reservoirLayer = new NeuronGroup(getParentNetwork(), reservoirLayerNeurons);
        addNeuronGroup(reservoirLayer);
        reservoirLayer.setLabel("Reservoir");
        NetworkLayoutManager.offsetNeuronGroup(inputLayer, reservoirLayer, Direction.NORTH, betweenLayerInterval);

        // Output Layer
        lineLayout.setInitialLocation(initialPosition);
        lineLayout.layoutNeurons(outputLayerNeurons);
        outputLayer = new NeuronGroup(getParentNetwork(), outputLayerNeurons);
        outputLayer.setLabel("Outputs");
        addNeuronGroup(outputLayer);
        NetworkLayoutManager.offsetNeuronGroup(reservoirLayer, outputLayer, Direction.NORTH, betweenLayerInterval);
        
        // Weights: Input layer to reservoir layer
        connector.setSparsity(inSparsity);
        connectNeuronGroups(inputLayer, reservoirLayer, connector);

        // Weights: reservoir layer to itself
        connector.setSparsity(resSparsity);
        connectNeuronGroups(reservoirLayer, reservoirLayer, connector);

        // Weights: reservoir layer to output layer
        //TODO: These only exist for the as yet unimplemented RLMS algorithm
        AllToAll allToAll  = new AllToAll(getParentNetwork(), reservoirLayerNeurons, outputLayerNeurons);
        connectNeuronGroups(reservoirLayer, outputLayer, allToAll);

        // Weights: output to reservoir
        if (backWeights) {
          connector.setSparsity(backSparsity);
          connectNeuronGroups(outputLayer, reservoirLayer, connector);
        }

        // Scale the reservoir's weights to have the desired spectral radius
        SimnetUtils.scaleEigenvalue(reservoirLayerNeurons, reservoirLayerNeurons,
                spectralRadius);
        
        // Add the group to the network
        getParentNetwork().addGroup(this);
        
//        //TODO: Re-think if RLMS is implemented...
//        ArrayList<Neuron> trainingInputs = new ArrayList<Neuron>();
//        if(directInOutWeights) {
//        	trainingInputs.addAll(inputLayerNeurons);
//        }
//        trainingInputs.addAll(reservoirLayerNeurons);
//        
//        if(recurrentOutWeights) {
//        	trainingInputs.addAll(outputLayerNeurons);
//        }
        
        trainer = new Trainer(getParentNetwork(),
        		inputLayerNeurons, outputLayerNeurons, new LMSOffline());
        trainer.setStateHarvester(true);
        
        //TODO:
        ReservoirComputingUtils.setEsn(this);
    }

    /**
     * Initializes a layer.
     * @param layer
     *            the layer to be initialized
     * @param nodeType
     *            type of nodes in the layer
     * @param layerType
     *            type of layer
     * @param nodes
     *            number of nodes in the layer
     */
    private void initializeLayer(List<Neuron> layer, NeuronUpdateRule nodeType, int nodes) {

        for (int i = 0; i < nodes; i++) {
            Neuron node = new Neuron(getParentNetwork(), nodeType);
            node.setIncrement(1); //TODO: Reasonable?
            layer.add(node);
        }
    }

/*    //TODO: When LSM is implemented move the special methods related to state harvesting to another class?
    *//**
     * Train the ESN using the provided data.
     *
     * @param inputData input data for input nodes
     * @param trainingData training data
     *//*
    public void train(double[][] inputData,
            double[][] trainingData) {

        // Generate the reservoir data to be used in training
        ReservoirComputingUtils.setNoise(noise);

        if (noise) {
            ReservoirComputingUtils.setNoiseMax(noiseMax);
            ReservoirComputingUtils.setNoiseMin(noiseMin);
        }

        // REDO
        
//        double[][] mainInputData = ReservoirComputingUtils.generateData(
//                this, inputData, trainingData);


        //System.out.println("-------");
        //System.out.println(Utils.doubleMatrixToString(mainInputData));

        ArrayList<Neuron> full = new ArrayList<Neuron>();

//        if (directInOutWeights) {
//            for (Neuron node : inputLayer) {
//                full.add(node);
//            }
//        }
//        for (Neuron node : reservoirLayer) {
//            full.add(node);
//        }
//
//        if (recurrentOutWeights) {
//            for (Neuron node : outputLayer) {
//                full.add(node);
//
//            }
//        }
        
        //REDO

//        if (mainInputData[0].length != full.size()) {
//            throw new IllegalArgumentException("Input data length does not "
//                    + "match training node set");
//        }

        if (trainingData[0].length != outputLayer.getNeuronList().size()) {
            throw new IllegalArgumentException("Output data length does not "
                    + "match the number of output nodes");
        }

        for (Neuron n : outputLayer.getNeuronList()) {
            if (n.getUpdateRule() instanceof SigmoidalNeuron) {
                for (int i = 0; i < trainingData.length; i++) {
                    int col =outputLayer.getNeuronList().indexOf(n);
                    trainingData[i][col] = ((SigmoidalNeuron)
                            n.getUpdateRule()).getInverse(trainingData[i][col],
                                    n);
                }
            }
        }

        Trainer trainer = new Trainer(getParentNetwork(), full, outputLayer.getNeuronList(), new LMSOffline());
//        trainer.setInputData(mainInputData); //REDO
        trainer.setTrainingData(trainingData);
        ((LMSOffline) trainer.getTrainingMethod()).setSolutionType(solType);
        trainer.update();

    }
*/

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

    public static int getDEFAULT_LAYER_INTERVAL() {
        return DEFAULT_LAYER_INTERVAL;
    }

    public static int getDEFAULT_NEURON_INTERVAL() {
        return DEFAULT_NEURON_INTERVAL;
    }

    public void setNoise(boolean noise) {
        this.noise = noise;
    }

    public boolean hasNoise() {
        return noise;
    }

    public void setNoiseMax(double noiseMax) {
        this.noiseMax = noiseMax;
    }

    public double getNoiseMax() {
        return noiseMax;
    }

    public void setNoiseMin(double noiseMin) {
        this.noiseMin = noiseMin;
    }

    public double getNoiseMin() {
        return noiseMin;
    }

    public int getNumReservoirNodes() {
        return numResNodes;
    }
    public int getNumInputNodes() {
        return numInputs;
    }
    public int getNumOutputNodes() {
        return numOutputs;
    }

    /**
     * @return the inputLayer
     */
    public NeuronGroup getInputLayer() {
        return inputLayer;
    }

    /**
     * @return the reservoirLayer
     */
    public NeuronGroup getReservoirLayer() {
        return reservoirLayer;
    }

    /**
     * @return the outputLayer
     */
    public NeuronGroup getOutputLayer() {
        return outputLayer;
    }

    /**
     * @return the trainer
     */
	public Trainer getTrainer() {
		
		return trainer;
	}

    /*

    public static void main (String args []) {
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

        esn.setSolType(SolutionType.WIENER_HOPF);

        esn.train(ins, outs);

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

    public static double [] [] sineWaveDataGen(
        double [][] inputs, int interval) {
        double [][] outs = new double[inputs.length][1];
        //Currently intervals are 300
        double time = 0;
        for(int i = 0; i < inputs.length; i++){
            outs[i][0] = Math.sin(inputs[i][0] * time);
            time += 1.0;
        }

        return outs;

    }

    

*/


}
