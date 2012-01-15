package org.simbrain.network.groups;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.groups.FeedForward.LayerType;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.NeuronUpdateRule;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neurons.ClampedNeuron;

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
    private Point2D.Double initialPosition = new Point2D.Double(0, 0);

    /** space between layers */
    private int betweenLayerInterval = 150;

    /** space between neurons within layers */
    private int betweenNeuronInterval = 50;

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

    /** Default constructor. */
    public SimpleRecurrentNetwork() {
        super(new RootNetwork());
        // network.setCustomUpdateRule(update);
    }

    /**
     * Constructor specifying root network, and number of nodes in each layer.
     * 
     * @param network underlying network
     * @param numInputNodes number of nodes in the input layer
     * @param numHiddenNodes number of nodes in the hidden and context layers
     * @param numOutputNodes number of output nodes
     */
    public SimpleRecurrentNetwork(final RootNetwork network, int numInputNodes,
            int numHiddenNodes, int numOutputNodes) {
        super(network);

            setLabel("SRN");

        this.numInputNodes = numInputNodes;
        this.numHiddenNodes = numHiddenNodes;
        this.numOutputNodes = numOutputNodes;
    }

    /** Builds a simple recurrent network. */
    public void build() {

        // Initialize layers
        List<Neuron> inputLayerNeurons  = new ArrayList<Neuron>();
        List<Neuron> hiddenLayerNeurons  = new ArrayList<Neuron>();
        List<Neuron> outputLayerNeurons  = new ArrayList<Neuron>();
        List<Neuron> contextLayerNeurons  = new ArrayList<Neuron>();
        
        initializeLayer(inputLayerNeurons, new ClampedNeuron(), numInputNodes);
        initializeLayer(hiddenLayerNeurons, hiddenNeuronType, numHiddenNodes);
        initializeLayer(outputLayerNeurons, outputNeuronType, numOutputNodes);
        initializeLayer(contextLayerNeurons, new ClampedNeuron(), numHiddenNodes);

        // Context Layer
        // Initial context layer values set to 0.5 (as in Elman 1991)
        // TODO: way to set this?
        contextLayer = new NeuronGroup(getParentNetwork(),
                contextLayerNeurons);
        addNeuronGroup(contextLayer);
        for (Neuron n : contextLayer.getNeuronList()) {
            n.setActivation(0.5);
        }

        
        //REDO: 
                //Use offset functions
                //Just use line layouts
        
        // Input Layer
        LineLayout layerLayout = new LineLayout(betweenNeuronInterval,
                LineOrientation.HORIZONTAL);
        layerLayout.setInitialLocation(new Point((int) initialPosition.getX()
                - getWidth(inputLayerNeurons) / 2, (int) initialPosition.getY()));
        layerLayout.layoutNeurons(inputLayerNeurons);
        inputLayer= new NeuronGroup(getParentNetwork(),
                inputLayerNeurons);
        addNeuronGroup(inputLayer);

        // Hidden Layer
        int hiddenX = (int) initialPosition.getX();
        int hiddenY = (int) initialPosition.getY() - betweenLayerInterval;
        layerLayout.setInitialLocation(new Point(hiddenX, hiddenY));
        layerLayout.layoutNeurons(hiddenLayerNeurons);
        hiddenLayer= new NeuronGroup(getParentNetwork(),
                hiddenLayerNeurons);
        addNeuronGroup(hiddenLayer);

        // Make context layer visible
        // TODO: Best position for arbitrary layer sizes?
        layerLayout.setInitialLocation(new Point(hiddenX + (int) 2
                * getWidth(contextLayerNeurons) / 3, hiddenY + (int) 2
                * betweenLayerInterval / 3));
        layerLayout.layoutNeurons(contextLayerNeurons);

        // Output layer
        layerLayout.setInitialLocation(new Point(hiddenX, hiddenY
                - betweenLayerInterval));
        layerLayout.layoutNeurons(outputLayerNeurons);
        outputLayer= new NeuronGroup(getParentNetwork(),
                outputLayerNeurons);
        addNeuronGroup(outputLayer);

        // Connect the laid-out layers
        connect();

        getParentNetwork().addGroup(this);

    }

    /** Connects SRN layers. */
    private void connect() {
        // Standard all to all connections
        AllToAll connect = new AllToAll(this.getParentNetwork());
        // No self connection
        connect.setAllowSelfConnection(false);
        // TODO: Way to set weight ranges and excitatory probability?
        //REDO: Add as a group
        connect.connectNeurons(inputLayer.getNeuronList(), hiddenLayer.getNeuronList(), -1.0, 1.0, 0.5);
        connect.connectNeurons(contextLayer.getNeuronList(), hiddenLayer.getNeuronList(), -1.0, 1.0, 0.5);
        connect.connectNeurons(hiddenLayer.getNeuronList(), outputLayer.getNeuronList(), -1.0, 1.0, 0.5);
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

    /**
     * Return the width of the specified layer, in pixels.
     * 
     * @param layer layer to "measure"
     * @return width of layer
     */
    private int getWidth(List<Neuron> layer) {
        return layer.size() * betweenNeuronInterval;
    }

    // Getters and Setters______________________________________________________
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

    public Point2D.Double getInitialPosition() {
        return initialPosition;
    }

    public void setInitialPosition(Point2D.Double initialPosition) {
        this.initialPosition = initialPosition;
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
