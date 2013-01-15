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
package org.simbrain.network.subnetworks;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neuron_update_rules.ClampedNeuronRule;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule.SigmoidType;
import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.network.util.NetworkLayoutManager;
import org.simbrain.network.util.NetworkLayoutManager.Direction;
import org.simbrain.network.util.RandomSource;
import org.simbrain.network.util.SimnetUtils;

/**
 * Builds an Echo-State Network with options for all valid weight
 * configurations.
 *
 * @author ztosi
 */
public class EchoStateNetwork extends Subnetwork {

    /** Number of input nodes. */
    private int numInputs;

    /** Number of reservoir nodes. */
    private int numResNodes;

    /** Number of output nodes. */
    private int numOutputs;

    /** Reservoir sparsity. */
    private double resSparsity;

    /** Sparsity of weights from input. */
    private double inSparsity;

    /** Sparsity of back weights from output to reservoir (if they exist). */
    private double backSparsity;

    /**
     * Desired spectral radius (max eigenvalue) of the reservoir's weight
     * matrix. Typical range is .8 -1.
     */
    private double spectralRadius;

    /**
     * Whether the network has weights from the output to the reservoir (not
     * trained.)
     */
    private boolean backWeights = true;

    /** Whether the network has recurrent output weights (trained). */
    private boolean recurrentOutWeights;

    /** Whether the network has direct input to output connections (trained). */
    private boolean directInOutWeights;

    /** Reservoir neuron type */
    private NeuronUpdateRule reservoirNeuronType = new SigmoidalRule();

    /** Reference to input layer. */
    private NeuronGroup inputLayer;

    /** Reference to input layer. */
    private NeuronGroup reservoirLayer;

    /** Reference to input layer. */
    private NeuronGroup outputLayer;

    /** Default TANH neurons for the reservoir */
    {
        ((SigmoidalRule) reservoirNeuronType).setType(SigmoidType.TANH);
    }

    /** Default output neuron type */
    private NeuronUpdateRule outputNeuronType = new LinearRule();

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

    /** Whether to use noise or not. */
    private boolean noise;

    /** A noise generator. */
    private RandomSource noiseGenerator = new RandomSource();

    /**
     * Input data. The sequence of inputs to be fed to the ESN's input layer.
     */
    private double[][] inputData;

    /**
     * Training Data. The desired sequence of inputs from the ESN's output
     * layer.
     */
    private double[][] trainingData;

    /**
     * Constructor with size of layers specified.
     *
     * @param network the root network wherein ESNBuilder will build components
     * @param inputNodes number of input nodes
     * @param reservoirNodes number of reservoir nodes
     * @param outputNodes number of output nodes
     */
    public EchoStateNetwork(final Network network, int inputNodes,
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
        List<Neuron> inputLayerNeurons = new ArrayList<Neuron>();
        List<Neuron> reservoirLayerNeurons = new ArrayList<Neuron>();
        List<Neuron> outputLayerNeurons = new ArrayList<Neuron>();
        initializeLayer(inputLayerNeurons, new ClampedNeuronRule(), numInputs);
        initializeLayer(reservoirLayerNeurons, reservoirNeuronType, numResNodes);
        initializeLayer(outputLayerNeurons, outputNeuronType, numOutputs);

        Sparse connector = new Sparse();
        connector.setEnableExcitatoryRandomization(true);
        connector.setEnableInhibitoryRandomization(true);

        // TODO: The user should be able to set these:
        RandomSource exRand = connector.getExcitatoryRandomizer();
        RandomSource inRand = connector.getInhibitoryRandomizer();
        connector.setPercentExcitatory(50);
        exRand.setLowerBound(0.1);
        exRand.setUpperBound(1.0);
        inRand.setLowerBound(-1.0);
        inRand.setUpperBound(-0.1);

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
        reservoirLayer = new NeuronGroup(getParentNetwork(),
                reservoirLayerNeurons);
        addNeuronGroup(reservoirLayer);
        reservoirLayer.setLabel("Reservoir");
        NetworkLayoutManager.offsetNeuronGroup(inputLayer, reservoirLayer,
                Direction.NORTH, betweenLayerInterval);

        // Output Layer
        lineLayout.setInitialLocation(initialPosition);
        lineLayout.layoutNeurons(outputLayerNeurons);
        outputLayer = new NeuronGroup(getParentNetwork(), outputLayerNeurons);
        outputLayer.setLabel("Outputs");
        addNeuronGroup(outputLayer);
        NetworkLayoutManager.offsetNeuronGroup(reservoirLayer, outputLayer,
                Direction.NORTH, betweenLayerInterval);

        // Weights: Input layer to reservoir layer
        connector.setSparsity(inSparsity);
        connectNeuronGroups(inputLayer, reservoirLayer, connector); // This is
                                                                    // where the
                                                                    // groups
                                                                    // are made

        // Weights: reservoir layer to itself
        connector.setSparsity(resSparsity);
        connectNeuronGroups(reservoirLayer, reservoirLayer, connector);

        // Weights: reservoir layer to output layer
        // TODO: These only exist for the as yet unimplemented RLMS algorithm
        AllToAll allToAll = new AllToAll(getParentNetwork(),
                reservoirLayerNeurons, outputLayerNeurons);
        connectNeuronGroups(reservoirLayer, outputLayer, allToAll);

        // Weights: output to reservoir
        if (backWeights) {
            connector.setSparsity(backSparsity);
            connectNeuronGroups(outputLayer, reservoirLayer, connector);
        }

        // If recurrent output weights are on, set up an empty, growing synapse
        // group
        // on the output layer
        if (recurrentOutWeights) {
            addEmptySynapseGroup(outputLayer, outputLayer);
        }

        // If direct in-out weights are on, set up an empty, growing synapse
        // group
        // connecting the input layer directly to the output layer
        if (directInOutWeights) {
            addEmptySynapseGroup(inputLayer, outputLayer);
        }

        // Scale the reservoir's weights to have the desired spectral radius
        SimnetUtils.scaleEigenvalue(reservoirLayerNeurons,
                reservoirLayerNeurons, spectralRadius);

        // Add the group to the network
        getParentNetwork().addGroup(this);

    }

    /**
     * Initializes a layer.
     *
     * @param layer the layer to be initialized
     * @param nodeType type of nodes in the layer
     * @param layerType type of layer
     * @param nodes number of nodes in the layer
     */
    private void initializeLayer(List<Neuron> layer, NeuronUpdateRule nodeType,
            int nodes) {

        for (int i = 0; i < nodes; i++) {
            Neuron node = new Neuron(getParentNetwork(), nodeType);
            node.setIncrement(1); // TODO: Reasonable?
            layer.add(node);
        }
    }

    /**
     * Return a trainer object that can be used to train this ESN. The trainer
     * has harvested state data as "inputs" and the desired outputs as outputs.
     *
     * @return the trainer.
     */
    public Trainer getTrainer() {

        // Exception if training data is not set properly at this point
        if (trainingData[0].length != outputLayer.getNeuronList().size()) {
            throw new IllegalArgumentException("Output data length does not "
                    + "match the number of output nodes");
        }

        // Build the network to be used in state harvesting
        final ArrayList<Neuron> full = new ArrayList<Neuron>();
        if (directInOutWeights) {
            for (Neuron node : getInputLayer().getNeuronList()) {
                full.add(node);
            }
        }
        for (Neuron node : reservoirLayer.getNeuronList()) {
            full.add(node);
        }
        if (recurrentOutWeights) {
            for (Neuron node : this.getOutputLayer().getNeuronList()) {
                full.add(node);
            }
        }

        // Handle non-linearities in the outputs.
        // If output layer neurons are sigmoidal, transform the desired
        // output to the inverse of the sigmoidal, so that when it's put
        // in to the sigmoidal it will produce the desired output.
        for (Neuron n : outputLayer.getNeuronList()) {
            if (n.getUpdateRule() instanceof SigmoidalRule) {
                for (int i = 0; i < trainingData.length; i++) {
                    int col = outputLayer.getNeuronList().indexOf(n);
                    trainingData[i][col] = ((SigmoidalRule) n.getUpdateRule())
                            .getInverse(trainingData[i][col], n);
                }
            }
        }

        // Make Trainable object
        Trainable trainable = new Trainable() {

            @Override
            public List<Neuron> getInputNeurons() {
                return full;
            }

            @Override
            public List<Neuron> getOutputNeurons() {
                return getOutputLayer().getNeuronList();
            }

            @Override
            public double[][] getInputData() {
                // Harvest the reservoir states
                final double[][] harvestedData = harvestData();
                if (harvestedData[0].length != full.size()) {
                    throw new IllegalArgumentException(
                            "Input data length does not "
                                    + "match training node set");
                }
                // System.out.println("-------");
                // System.out.println(Utils.doubleMatrixToString(mainInputData));
                return harvestedData;
            }

            @Override
            public double[][] getTrainingData() {
                return trainingData;
            }

        };
        // Create the offline trainer.
        LMSOffline trainer = new LMSOffline(trainable);
        return trainer;

    }

    /**
     * A general method for harvesting state data for an arbitrary Echo-State
     * Network. This method iterates through each row of input and teacher data
     * (if the network possesses back weights and/or recurrent output weights),
     * and updates the reservoir. Depending on the ESN's un-frozen connectivity
     * the resulting return matrix will have rows consisting of concatenated
     * input, reservoir, and (teacher-forced) output states in that order.
     *
     * @return a matrix of data to be used for training
     */
    private double[][] harvestData() {

        // The minimum number of state matrix columns
        int columnNumber = numResNodes;

        if (directInOutWeights) {
            // Add columns for the input layer states
            columnNumber += numInputs;
        }
        if (recurrentOutWeights) {
            // Add columns for output layer states
            columnNumber += numOutputs;
        }

        // State matrix
        double[][] returnMatrix = new double[inputData.length][columnNumber];

        boolean harvest = false;

        // Two full passes over the data, one where states are being harvested
        // and one where internal dynamics are being allowed to settle.
        for (int t = 0; t < 2; t++) {

            // Iterate through each row of input data
            for (int row = 0; row < inputData.length; row++) {

                int col = 0;

                // Clamp input neurons based on input data
                for (Neuron neuron : getInputLayer().getNeuronList()) {
                    double clampValue = inputData[row][col];
                    neuron.setActivation(clampValue);
                    if (directInOutWeights && harvest) {
                        // Add input states to state matrix if direct in to out
                        // connections are desired
                        returnMatrix[row][col] = neuron.getActivation();
                        col++;
                    }

                }

                if (backWeights) {
                    int count = 0;
                    double clampValue = 0.5;
                    for (Neuron neuron : getOutputLayer().getNeuronList()) {
                        // Teacher forcing
                        if (row > 0) {
                            clampValue = trainingData[row - 1][count];
                        }
                        neuron.setActivation(clampValue);
                        count++;
                    }
                }

                // Update the reservoir: handles teacher-forced back-weights
                for (Neuron n : getReservoirLayer().getNeuronList()) {
                    n.update();
                }
                for (Neuron n : getReservoirLayer().getNeuronList()) {
                    double val = n.getBuffer();

                    if (noise) {
                        n.setActivation(val + reservoirNoise());
                    } else {
                        n.setActivation(val);
                    }

                    if (harvest) {
                        returnMatrix[row][col] = n.getActivation();
                    }
                    col++;
                }

                // Add output states to state matrix if there are recurrent
                // outputs
                if (recurrentOutWeights && harvest) {
                    for (int i = 0; i < trainingData[0].length; i++) {
                        // Teacher-forcing
                        returnMatrix[row][col] = trainingData[row][i];
                        col++;
                    }
                }
            }
            harvest = true;
        }

        return returnMatrix;
    }

    /**
     * Compute the reservoir noise.
     *
     * @return noise reservoir noise.
     */
    private double reservoirNoise() {
        return noiseGenerator.getRandom();
    }

    /**
     * Set the reservoir sparsity.
     *
     * @param resSparsity reservoir sparsity.
     */
    public void setResSparsity(double resSparsity) {
        this.resSparsity = resSparsity;
    }

    /**
     * Set the input sparsity.
     *
     * @param inSparsity the input sparsity
     */
    public void setInSparsity(double inSparsity) {
        this.inSparsity = inSparsity;
    }

    /**
     * Set the back sparsity.
     *
     * @param backSparsity
     */
    public void setBackSparsity(double backSparsity) {
        this.backSparsity = backSparsity;
    }

    /**
     * Set spectral radius
     *
     * @param spectralRadius the spectral radius
     */
    public void setSpectralRadius(double spectralRadius) {
        this.spectralRadius = spectralRadius;
    }

    /**
     * Set type of reservoir neurons.
     *
     * @param reservoirNeuronType
     */
    public void setReservoirNeuronType(NeuronUpdateRule reservoirNeuronType) {
        this.reservoirNeuronType = reservoirNeuronType;
    }

    /**
     * Set type of output neurons.
     *
     * @param outputNeuronType
     */
    public void setOutputNeuronType(NeuronUpdateRule outputNeuronType) {
        this.outputNeuronType = outputNeuronType;
    }

    /**
     * Set to true for weights from output to reservoir.
     *
     * @param backWeights
     */
    public void setBackWeights(boolean backWeights) {
        this.backWeights = backWeights;
    }

    /**
     * Set to true for (TODO).
     *
     * @param recurrentWeights
     */
    public void setRecurrentOutWeights(boolean recurrentWeights) {
        this.recurrentOutWeights = recurrentWeights;
    }

    /**
     * Set to true for weights directly from input to output.
     *
     * @param directInOutWeights weights directly from input to output.
     */
    public void setDirectInOutWeights(boolean directInOutWeights) {
        this.directInOutWeights = directInOutWeights;
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
     * @return the trainingData
     */
    public double[][] getTrainingData() {
        return trainingData;
    }

    /**
     * @param trainingData the trainingData to set
     */
    public void setTrainingData(double[][] trainingData) {
        this.trainingData = trainingData;
    }

    // TODO: Integrate random panel

    /**
     * @return the noise
     */
    public boolean getUseNoise() {
        return noise;
    }

    /**
     * @param noise the noise to set
     */
    public void setUseNoise(boolean noise) {
        this.noise = noise;
    }

    public RandomSource getNoiseGenerator() {
        return noiseGenerator;
    }

    public void setNoiseGenerator(RandomSource noiseGenerator) {
        this.noiseGenerator = noiseGenerator;
    }

    public void setNoise(boolean noise) {
        this.noise = noise;
    }

}
