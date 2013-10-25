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
package org.simbrain.network.subnetworks;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.synapse_update_rules.ClampedSynapseRule;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.network.util.NetworkLayoutManager;
import org.simbrain.network.util.NetworkLayoutManager.Direction;

/**
 * Implements a simple recurrent network (See, e.g, Elman 1991).
 * 
 * @author ztosi
 * @author Jeff Yoshimi
 */
public final class SimpleRecurrentNetwork extends Subnetwork implements
		Trainable {

	/**
	 * A layer consisting of a copy of the hidden layer from a previous time
	 * step
	 */
	private NeuronGroup contextLayer;

	/** Reference to the input layer. */
	private final NeuronGroup inputLayer;

	/** Reference to the hidden layer. */
	private final NeuronGroup hiddenLayer;

	/** Reference to the output layer. */
	private final NeuronGroup outputLayer;

	/** Initial position of network (from bottom left). */
	private Point2D initialPosition;

	/** space between layers */
	private int betweenLayerInterval = 150;

	/** space between neurons within layers */
	private int betweenNeuronInterval = 50;

	/**
	 * Training set.
	 */
	private final TrainingSet trainingSet = new TrainingSet();

	// TODO: Add a simpler constructor without neurontypes.

	/**
	 * Constructor specifying root network, and number of nodes in each layer.
	 * 
	 * @param network
	 *            underlying network
	 * @param numInputNodes
	 *            number of nodes in the input layer
	 * @param numHiddenNodes
	 *            number of nodes in the hidden and context layers
	 * @param numOutputNodes
	 *            number of output nodes
	 * @param hiddenNeuronType
	 *            update rule for hidden nodes
	 * @param outputNeuronType
	 *            update rule for hidden nodes
	 * @param initialPosition
	 *            where to position the network (upper left)
	 */
	public SimpleRecurrentNetwork(final Network network,
			int numInputNodes, int numHiddenNodes, int numOutputNodes,
			NeuronUpdateRule hiddenNeuronType,
			NeuronUpdateRule outputNeuronType, Point2D initialPosition) {
		super(network);

		this.initialPosition = initialPosition;

		setLabel("SRN");

		// TODO: implement new connectNeuronGroups method
		// Initialize layers
		List<Neuron> inputLayerNeurons = new ArrayList<Neuron>();
		List<Neuron> hiddenLayerNeurons = new ArrayList<Neuron>();
		List<Neuron> outputLayerNeurons = new ArrayList<Neuron>();
		List<Neuron> contextLayerNeurons = new ArrayList<Neuron>();

		// TODO: Think about these defaults...
		initializeLayer(inputLayerNeurons, new LinearRule(),
				numInputNodes);
		initializeLayer(hiddenLayerNeurons, hiddenNeuronType,
				numHiddenNodes);
		initializeLayer(outputLayerNeurons, outputNeuronType,
				numOutputNodes);
		initializeLayer(contextLayerNeurons, new LinearRule(),
				numHiddenNodes);

		// Input Layer
		LineLayout layerLayout =
				new LineLayout(betweenNeuronInterval,
						LineOrientation.HORIZONTAL);
		layerLayout.setInitialLocation(new Point((int) initialPosition
				.getX(), (int) initialPosition.getY()));
		layerLayout.layoutNeurons(inputLayerNeurons);
		inputLayer =
				new NeuronGroup(getParentNetwork(), inputLayerNeurons);
		inputLayer.setLabel("Inputs");
		addNeuronGroup(inputLayer);

		// Hidden Layer
		layerLayout.layoutNeurons(hiddenLayerNeurons);
		hiddenLayer =
				new NeuronGroup(getParentNetwork(), hiddenLayerNeurons);
		hiddenLayer.setLabel("Hidden layer");
		addNeuronGroup(hiddenLayer);
		NetworkLayoutManager.offsetNeuronGroup(inputLayer, hiddenLayer,
				Direction.NORTH, betweenLayerInterval);

		// Context Layer
		// Initial context layer values set to 0.5 (as in Elman 1991)
		layerLayout.layoutNeurons(contextLayerNeurons);
		contextLayer =
				new NeuronGroup(getParentNetwork(), contextLayerNeurons);
		contextLayer.setLabel("Context nodes");
		addNeuronGroup(contextLayer);
		NetworkLayoutManager.offsetNeuronGroup(inputLayer, contextLayer,
				Direction.EAST, betweenLayerInterval);

		// Output layer
		layerLayout.layoutNeurons(outputLayerNeurons);
		outputLayer =
				new NeuronGroup(getParentNetwork(), outputLayerNeurons);
		addNeuronGroup(outputLayer);
		outputLayer.setLabel("Output layer");
		NetworkLayoutManager.offsetNeuronGroup(hiddenLayer, outputLayer,
				Direction.NORTH, betweenLayerInterval);

		// Connect the laid-out layers
		connect();

	}

	/** Connects SRN layers. */
	private void connect() {
		// Standard all to all connections
		AllToAll connect = new AllToAll(this.getParentNetwork());
		connect.setAllowSelfConnection(false);
		connect.setExcitatoryRatio(.5);
		Synapse synapse =
				Synapse.getTemplateSynapse(new ClampedSynapseRule());
		synapse.setLowerBound(-1);
		synapse.setUpperBound(1);
		connect.setBaseExcitatorySynapse(synapse);
		connect.setBaseInhibitorySynapse(synapse);
		connectNeuronGroups(inputLayer, hiddenLayer, connect);
		connectNeuronGroups(contextLayer, hiddenLayer, connect);
		connectNeuronGroups(hiddenLayer, outputLayer, connect);
	}

	/**
	 * Initializes a layer by adding the desired number of neurons with the
	 * desired neuron update rule to the List of neurons.
	 * 
	 * @param layer
	 *            the list of neurons
	 * @param nodeType
	 *            the desired neuron update rule
	 * @param lowerBound
	 *            lower bound for neurons in this layer
	 * @param nodes
	 *            the desired number of nodes
	 */
	private void initializeLayer(List<Neuron> layer,
			NeuronUpdateRule nodeType, int nodes) {

		for (int i = 0; i < nodes; i++) {
			Neuron node = new Neuron(getParentNetwork(), nodeType);
			nodeType.setIncrement(1);
			layer.add(node);
		}
	}

	@Override
	public void initNetwork() {
		clearActivations();
		// TODO: Do this once there is a GUI hook to make sure this is done
		// when the user tests the network
		// contextLayer.setActivationLevels(.5);
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

	@Override
	public List<List<Neuron>> getNeuronGroupsAsList() {
		List<List<Neuron>> ret = new ArrayList<List<Neuron>>();
		ret.add(getInputNeurons());
		ret.add(getHiddenLayer().getNeuronList());
		ret.add(getOutputNeurons());
		return ret;
	}

	@Override
	public List<Neuron> getInputNeurons() {
		return inputLayer.getNeuronList();
	}

	@Override
	public List<Neuron> getOutputNeurons() {
		return outputLayer.getNeuronList();
	}

	@Override
	public TrainingSet getTrainingSet() {
		return trainingSet;
	}

	/**
	 * @return the contextLayer
	 */
	public NeuronGroup getContextLayer() {
		return contextLayer;
	}

	/**
	 * @return the hiddenLayer
	 */
	public NeuronGroup getHiddenLayer() {
		return hiddenLayer;
	}

	@Override
	public String getUpdateMethodDesecription() {
		return "Hidden layer, copy hidden to context, update layer";
	}

}
