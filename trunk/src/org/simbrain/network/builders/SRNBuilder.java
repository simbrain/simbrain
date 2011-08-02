package org.simbrain.network.builders;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.groups.NeuronLayer;
import org.simbrain.network.groups.NeuronLayer.LayerType;
import org.simbrain.network.interfaces.CustomUpdateRule;
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
 *
 */

public final class SRNBuilder {

	/** Underlying root network*/
	private final RootNetwork network;
	
	/** Size of the input, hidden, and output layers */
    private int numInputNodes, numHiddenNodes, numOutputNodes;
	
	//TODO: Theoretically unnecessary, way to display in GUI as separate layer without actually making it a separate layer?
	private List<Neuron> contextLayer = new ArrayList<Neuron>();
	
	 /** Reference to input layer. */
    private List<Neuron> inputLayer = new ArrayList<Neuron>();

    /** Reference to input layer. */
    private List<Neuron> hiddenLayer = new ArrayList<Neuron>();
    
    /** Reference to input layer. */
    private List<Neuron> outputLayer = new ArrayList<Neuron>();	

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
	
    /** Custom update rule for SRN "copy-back" of hidden layer values to the
     * context layer at the end of each network iteration. */
	CustomUpdateRule update = new CustomUpdateRule(){
		@Override
		public void update(RootNetwork network) {
			network.updateAllNeurons();
            network.updateAllSynapses();
            network.updateAllNetworks();
            network.updateAllGroups();
            for(Neuron n : hiddenLayer){
            	double act = n.getActivation();
            	int index = hiddenLayer.indexOf(n);
            	contextLayer.get(index).setActivation(act);
            }
		}
	};
    
	/** Default constructor */
	public SRNBuilder(){
		this.network = new RootNetwork();
		network.setCustomUpdateRule(update);
	}
	
	/**
	 * Constructor specifying root network, and number of nodes in each layer.
	 * @param network underlying network
	 * @param numInputNodes	number of nodes in the input layer
	 * @param numHiddenNodes number of nodes in the hidden and context layers
	 * @param numOutputNodes number of output nodes
	 */
	public SRNBuilder(final RootNetwork network, int numInputNodes, int numHiddenNodes, 
			int numOutputNodes){
		this.network = network;
		network.setCustomUpdateRule(update);
		this.numInputNodes = numInputNodes;
		this.numHiddenNodes = numHiddenNodes;
		this.numOutputNodes = numOutputNodes;
	}
	
	/**
	 * Builds a simple recurrent network
	 */
	public void build(){
		
		//Initialize layers
		initializeLayer(inputLayer, new ClampedNeuron(),
                LayerType.Input, numInputNodes);
        initializeLayer(hiddenLayer, hiddenNeuronType,
                LayerType.Hidden, numHiddenNodes);
        initializeLayer(outputLayer, outputNeuronType,
                LayerType.Output, numOutputNodes);
        initializeLayer(contextLayer, new ClampedNeuron(),
                LayerType.Context, numHiddenNodes);
        
        //Initial context layer values set to 0.5 (as in Elman 1991)
        //TODO: way to set this?
        for(Neuron n : contextLayer){
        	n.setActivation(0.5);
        }
        
        //Layout layers in GUI
        LineLayout layerLayout = new LineLayout(betweenNeuronInterval,
                LineOrientation.HORIZONTAL);
        layerLayout.setInitialLocation(new Point((int) initialPosition.getX()
                - getWidth(inputLayer) / 2, (int) initialPosition.getY()));
        layerLayout.layoutNeurons(inputLayer);
        
        int hiddenX = (int)initialPosition.getX();
        int hiddenY = (int)initialPosition.getY() - betweenLayerInterval;
        
        layerLayout.setInitialLocation(new Point(hiddenX, hiddenY));
        layerLayout.layoutNeurons(hiddenLayer);
        
        //Make context layer visible
        //TODO: Best position for arbitrary layer sizes?
        layerLayout.setInitialLocation(new Point(hiddenX + 
        		(int)2*getWidth(hiddenLayer)/3, hiddenY + 
        		(int)2*betweenLayerInterval/3));
        layerLayout.layoutNeurons(contextLayer);
        
        layerLayout.setInitialLocation(new Point(hiddenX,
        		hiddenY - betweenLayerInterval));
        layerLayout.layoutNeurons(outputLayer);
        
        //Connect the laid-out layers
        connect();
        
	}
	
	/**
	 * Connects SRN layers
	 */
	 private void connect(){
		 //Standard all to all connections
		 AllToAll connect = new AllToAll(network);
		 //No self connection
		 AllToAll.setAllowSelfConnection(false);
		 //TODO: Way to set weight ranges and excitatory probability?
	     connect.connectNeurons(inputLayer, hiddenLayer, -1.0, 1.0, 0.5);
	     connect.connectNeurons(contextLayer, hiddenLayer, -1.0, 1.0, 0.5);
	     connect.connectNeurons(hiddenLayer, outputLayer, -1.0, 1.0, 0.5);
	 }
	
	 /**
	  * Initializes a layer by adding the desired number of neurons with the
	  * desired neuron update rule to the List of neurons
	  * @param layer the list of neurons
	  * @param nodeType the desired neuron update rule
	  * @param layerType the type of layer for labeling as a neuron group
	  * @param nodes the desired number of nodes
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
	 * Return the width of the specified layer, in pixels.
	 * @param layer
	 *            layer to "measure"
	 * @return width of layer
	 */
	 private int getWidth(List<Neuron> layer) {
		 return layer.size() * betweenNeuronInterval;
	 }

	 
	//Getters and Setters______________________________________________________ 
	public RootNetwork getNetwork() {
		return network;
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

	public void setContextLayer(List<Neuron> contextLayer) {
		this.contextLayer = contextLayer;
	}

	public List<Neuron> getContextLayer() {
		return contextLayer;
	}

	public void setInputLayer(List<Neuron> inputLayer) {
		this.inputLayer = inputLayer;
	}

	public List<Neuron> getInputLayer() {
		return inputLayer;
	}

	public NeuronUpdateRule getOutputNeuronType() {
		return outputNeuronType;
	}

	public void setOutputNeuronType(NeuronUpdateRule outputNeuronType) {
		this.outputNeuronType = outputNeuronType;
	}

	public void setHiddenLayer(List<Neuron> hiddenLayer) {
		this.hiddenLayer = hiddenLayer;
	}

	public List<Neuron> getHiddenLayer() {
		return hiddenLayer;
	}

	public void setHiddenNeuronType(NeuronUpdateRule hiddenNeuronType) {
		this.hiddenNeuronType = hiddenNeuronType;
	}

	public NeuronUpdateRule getHiddenNeuronType() {
		return hiddenNeuronType;
	}

}
