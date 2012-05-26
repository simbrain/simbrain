package org.simbrain.network.subnetworks;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neuron_update_rules.ClampedNeuronRule;
import org.simbrain.network.util.NetworkLayoutManager;
import org.simbrain.network.util.NetworkLayoutManager.Direction;

/**
 * Builds a simple recurrent network from the specified parameters.
 * 
 * @author ztosi
 */

public final class SimpleRecurrentNetwork extends Subnetwork {

    /** Size of the input, hidden, and output layers */
    private int numInputNodes, numHiddenNodes, numOutputNodes;

    /**
     * A layer consisting of a copy of the hidden layer from a previous time
     * step
     */
    private NeuronGroup contextLayer;

    /** Reference to the input layer. */
    private NeuronGroup inputLayer;

    /** Reference to the hidden layer. */
    private NeuronGroup hiddenLayer;

    /** Reference to the output layer. */
    private NeuronGroup outputLayer;

    /** Neuron update rule for the hidden layer */
    private NeuronUpdateRule hiddenNeuronType;

    /** Neuron update rule for the output layer */
    private NeuronUpdateRule outputNeuronType;

    /** Initial position of network (from bottom left). */
    private Point2D initialPosition;

    /** space between layers */
    private int betweenLayerInterval = 150;

    /** space between neurons within layers */
    private int betweenNeuronInterval = 50;

    /**
     * Constructor specifying root network, and number of nodes in each layer.
     * 
     * @param network underlying network
     * @param numInputNodes number of nodes in the input layer
     * @param numHiddenNodes number of nodes in the hidden and context layers
     * @param numOutputNodes number of output nodes
     */
    public SimpleRecurrentNetwork(final Network network, int numInputNodes,
            int numHiddenNodes, int numOutputNode, Point2D initialPosition) {
        super(network);

        this.initialPosition = initialPosition;

        setLabel("SRN");

        this.numInputNodes = numInputNodes;
        this.numHiddenNodes = numHiddenNodes;
        this.numOutputNodes = numOutputNode;
    }

    /** Builds a simple recurrent network. */
    public void build() {
    	//TODO: implement new connectNeuronGroups method
        // Initialize layers
        List<Neuron> inputLayerNeurons  = new ArrayList<Neuron>();
        List<Neuron> hiddenLayerNeurons  = new ArrayList<Neuron>();
        List<Neuron> outputLayerNeurons  = new ArrayList<Neuron>();
        List<Neuron> contextLayerNeurons  = new ArrayList<Neuron>();
        
        initializeLayer(inputLayerNeurons, new ClampedNeuronRule(), numInputNodes);
        initializeLayer(hiddenLayerNeurons, hiddenNeuronType, numHiddenNodes);
        initializeLayer(outputLayerNeurons, outputNeuronType, numOutputNodes);
        initializeLayer(contextLayerNeurons, new ClampedNeuronRule(), numHiddenNodes);

        // Input Layer
        LineLayout layerLayout = new LineLayout(betweenNeuronInterval,
                LineOrientation.HORIZONTAL);
        layerLayout.setInitialLocation(new Point((int) initialPosition.getX(),
                (int) initialPosition.getY()));
        layerLayout.layoutNeurons(inputLayerNeurons);
        inputLayer= new NeuronGroup(getParentNetwork(),
                inputLayerNeurons);
        inputLayer.setLabel("Inputs");
        addNeuronGroup(inputLayer);
        
        // Hidden Layer
        layerLayout.layoutNeurons(hiddenLayerNeurons);
        hiddenLayer= new NeuronGroup(getParentNetwork(),
                hiddenLayerNeurons);
        hiddenLayer.setLabel("Hidden layer");
        addNeuronGroup(hiddenLayer);
        NetworkLayoutManager.offsetNeuronGroup(inputLayer, hiddenLayer, Direction.NORTH, betweenLayerInterval);
        
        // Context Layer
        // Initial context layer values set to 0.5 (as in Elman 1991)
        layerLayout.layoutNeurons(contextLayerNeurons);
        contextLayer = new NeuronGroup(getParentNetwork(),
                contextLayerNeurons);
        contextLayer.setLabel("Context nodes");
        addNeuronGroup(contextLayer);
        for (Neuron n : contextLayer.getNeuronList()) {
            n.setActivation(0.5); //TODO: Make helper method
        }
        NetworkLayoutManager.offsetNeuronGroup(inputLayer, contextLayer, Direction.EAST, betweenLayerInterval);

        // Output layer
        layerLayout.layoutNeurons(outputLayerNeurons);
        outputLayer= new NeuronGroup(getParentNetwork(),
                outputLayerNeurons);
        addNeuronGroup(outputLayer);
        outputLayer.setLabel("Output layer");
        NetworkLayoutManager.offsetNeuronGroup(hiddenLayer, outputLayer, Direction.NORTH, betweenLayerInterval);

        // Connect the laid-out layers
        connect();

        getParentNetwork().addGroup(this);

    }

    /** Connects SRN layers. */
    private void connect() {
        // Standard all to all connections
        AllToAll connect = new AllToAll(this.getParentNetwork());
        connect.setAllowSelfConnection(false);
        connect.setPercentExcitatory(.5);
        //connect.setBaseExcitatorySynapse(null);
        connectNeuronGroups(inputLayer, hiddenLayer, connect);
        connectNeuronGroups(contextLayer, hiddenLayer, connect);
        connectNeuronGroups(hiddenLayer, outputLayer, connect);
    }

    /**
     * Initializes a layer by adding the desired number of neurons with the
     * desired neuron update rule to the List of neurons
     * 
     * @param layer the list of neurons
     * @param nodeType the desired neuron update rule
     * @param layerType the type of layer for labeling as a neuron group
     * @param nodes the desired number of nodes
     */
    private void initializeLayer(List<Neuron> layer, NeuronUpdateRule nodeType,
            int nodes) {

        for (int i = 0; i < nodes; i++) {
            Neuron node = new Neuron(getParentNetwork(), nodeType);
            node.setIncrement(1); // TODO: Reasonable?
            layer.add(node);
        }
    }

    @Override
    public void update() {
        
        inputLayer.update();
        hiddenLayer.update();

        // Update context Layer
        for (Neuron n : hiddenLayer.getNeuronList()) {
            double act = n.getActivation();
            int index = hiddenLayer.getNeuronList().indexOf(n);
            contextLayer.getNeuronList().get(index).setActivation(act);
        }
        
        outputLayer.update();
    }

    public void setNumInputNodes(int numInputNodes) {
        this.numInputNodes = numInputNodes;
    }

    public int getNumInputNodes() {
        return numInputNodes;
    }

    public void setNumHiddenNodes(int numHiddenNodes) {
        this.numHiddenNodes = numHiddenNodes;
    }

    public int getNumHiddenNodes() {
        return numHiddenNodes;
    }

    public void setNumOutputNodes(int numOutputNodes) {
        this.numOutputNodes = numOutputNodes;
    }

    public int getNumOutputNodes() {
        return numOutputNodes;
    }

    public NeuronUpdateRule getOutputNeuronType() {
        return outputNeuronType;
    }

    public void setOutputNeuronType(NeuronUpdateRule outputNeuronType) {
        this.outputNeuronType = outputNeuronType;
    }


    public void setHiddenNeuronType(NeuronUpdateRule hiddenNeuronType) {
        this.hiddenNeuronType = hiddenNeuronType;
    }

    public NeuronUpdateRule getHiddenNeuronType() {
        return hiddenNeuronType;
    }

    public int getBetweenLayerInterval() {
        return betweenLayerInterval;
    }

    public void setBetweenLayerInterval(int betweenLayerInterval) {
        this.betweenLayerInterval = betweenLayerInterval;
    }

    public int getBetweenNeuronInterval() {
        return betweenNeuronInterval;
    }

    public void setBetweenNeuronInterval(int betweenNeuronInterval) {
        this.betweenNeuronInterval = betweenNeuronInterval;
    }


}
