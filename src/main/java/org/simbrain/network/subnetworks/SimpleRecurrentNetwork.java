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

import org.simbrain.network.NetworkModel;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.SynapseGroup2;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.network.util.Direction;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static org.simbrain.network.util.NetworkLayoutManagerKt.offsetNeuronGroup;

/**
 * Implements a simple recurrent network (See, e.g, Elman 1991). While the
 * SRN behavior could be implemented more efficiently by using a recurrent
 * hidden layer and a buffered update, a context layer is here for educational
 * and visualization purposes, as well as to more closely follow Elman's
 * original design.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public final class SimpleRecurrentNetwork extends Subnetwork implements Trainable {

    /**
     * A layer consisting of a copy of the hidden layer from a previous time
     * step
     */
    private NeuronGroup contextLayer;

    /**
     * Reference to the input layer.
     */
    private final NeuronGroup inputLayer;

    /**
     * Reference to the hidden layer.
     */
    private final NeuronGroup hiddenLayer;

    /**
     * Reference to the output layer.
     */
    private final NeuronGroup outputLayer;

    /**
     * space between layers
     */
    private int betweenLayerInterval = 300;

    /**
     * Training set.
     */
    private final TrainingSet trainingSet = new TrainingSet();

    /**
     * Build an SRN with default activation rules and initial position.
     *
     * @param network        underlying network
     * @param numInputNodes  number of nodes in the input layer
     * @param numHiddenNodes number of nodes in the hidden and context layers
     * @param numOutputNodes number of output nodes
     */
    public SimpleRecurrentNetwork(final Network network, int numInputNodes, int numHiddenNodes, int numOutputNodes) {
        this(network, numInputNodes, numHiddenNodes, numOutputNodes, new SigmoidalRule(), new SigmoidalRule(), new Point2D.Double(0, 0));
    }

    /**
     * Constructor specifying root network, and number of nodes in each layer.
     *
     * @param network          underlying network
     * @param numInputNodes    number of nodes in the input layer
     * @param numHiddenNodes   number of nodes in the hidden and context layers
     * @param numOutputNodes   number of output nodes
     * @param hiddenNeuronType update rule for hidden nodes
     * @param outputNeuronType update rule for hidden nodes
     * @param initialPosition  where to position the network (upper left)
     */
    public SimpleRecurrentNetwork(final Network network, int numInputNodes, int numHiddenNodes, int numOutputNodes, NeuronUpdateRule hiddenNeuronType, NeuronUpdateRule outputNeuronType, Point2D initialPosition) {
        super(network);

        setLabel("SRN");
        // Initialize layers and set node types. TODO: Can this be done at group
        // level?
        List<Neuron> inputLayerNeurons = new ArrayList<Neuron>();
        List<Neuron> hiddenLayerNeurons = new ArrayList<Neuron>();
        List<Neuron> outputLayerNeurons = new ArrayList<Neuron>();
        List<Neuron> contextLayerNeurons = new ArrayList<Neuron>();
        initializeLayer(inputLayerNeurons, new LinearRule(), numInputNodes);
        initializeLayer(hiddenLayerNeurons, hiddenNeuronType, numHiddenNodes);
        initializeLayer(contextLayerNeurons, new LinearRule(), numHiddenNodes);
        initializeLayer(outputLayerNeurons, outputNeuronType, numOutputNodes);

        // Input Layer
        inputLayer = new NeuronGroup(getParentNetwork(), inputLayerNeurons);
        inputLayer.setLabel("Input layer");
        inputLayer.setClamped(true);
        inputLayer.setIncrement(1);
        addModel(inputLayer);
        inputLayer.setLayoutBasedOnSize(initialPosition);

        // Hidden Layer
        hiddenLayer = new NeuronGroup(getParentNetwork(), hiddenLayerNeurons);
        //hiddenLayer.setLabel("Hidden layer (x(t))");
        hiddenLayer.setLabel("Hidden layer");
        addModel(hiddenLayer);
        hiddenLayer.setLowerBound(-1);
        hiddenLayer.setUpperBound(1);
        hiddenLayer.setLayoutBasedOnSize();
        offsetNeuronGroup(inputLayer, hiddenLayer, Direction.NORTH, betweenLayerInterval);

        // Context Layer
        // Initial context layer values set to 0.5 (as in Elman 1991). TODO
        contextLayer = new NeuronGroup(getParentNetwork(), contextLayerNeurons);
        //contextLayer.setLabel("Context Layer (x(t - \u0394t))");
        contextLayer.setLabel("Context Layer");
        addModel(contextLayer);
        contextLayer.setLayoutBasedOnSize();
        offsetNeuronGroup(inputLayer, contextLayer, Direction.EAST, betweenLayerInterval);

        // Output layer
        outputLayer = new NeuronGroup(getParentNetwork(), outputLayerNeurons);
        addModel(outputLayer);
        outputLayer.setLabel("Output layer");
        outputLayer.setLayoutBasedOnSize();
        offsetNeuronGroup(hiddenLayer, outputLayer, Direction.NORTH, betweenLayerInterval);

        // Connect the layers
        SynapseGroup2 inToHid = SynapseGroup.createSynapseGroup(inputLayer, hiddenLayer, new AllToAll(false), 0.5);
        SynapseGroup2 contToHid = SynapseGroup.createSynapseGroup(contextLayer, hiddenLayer, new AllToAll(false), 0.5);
        SynapseGroup2 hidToOut = SynapseGroup.createSynapseGroup(hiddenLayer, outputLayer, new AllToAll(false), 0.5);

        // TODO
        // addAndLabelSynapseGroup(inToHid);
        // addAndLabelSynapseGroup(contToHid);
        // addAndLabelSynapseGroup(hidToOut);

        // Initialize activations
        initNetwork();

    }

    /**
     * Helper method to initialize a layer by adding the desired number of
     * neurons with the desired neuron update rule.
     *
     * @param layer    the list of neurons
     * @param nodeType the desired neuron update rule
     * @param nodes    the desired number of nodes
     */
    private void initializeLayer(List<Neuron> layer, NeuronUpdateRule nodeType, int nodes) {

        for (int i = 0; i < nodes; i++) {
            Neuron node = new Neuron(getParentNetwork(), nodeType);
            layer.add(node);
        }
    }

    @Override
    public void initNetwork() {
        // clearActivations();
        hiddenLayer.forceSetActivationLevels(.5);
    }

    @Override
    public void update() {

        // Update input layer
        inputLayer.update();

        // Copy hidden layer activations to context layer
        for (Neuron n : hiddenLayer.getNeuronList()) {
            double act = n.getActivation();
            int index = hiddenLayer.getNeuronList().indexOf(n);
            contextLayer.getNeuronList().get(index).setActivation(act);
        }

        // Update hidden layer
        hiddenLayer.update();

        // Update output layer
        outputLayer.update();

    }

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

    public NeuronGroup getContextLayer() {
        return contextLayer;
    }

    public NeuronGroup getHiddenLayer() {
        return hiddenLayer;
    }

    public NeuronGroup getInputLayer() {
        return inputLayer;
    }

    public NeuronGroup getOutputLayer() {
        return outputLayer;
    }

    // TOODO
    //@Override
    //public String getUpdateMethodDescription() {
    //    return "Hidden layer, copy hidden to context, update layer";
    //}

    @Override
    public NetworkModel getNetwork() {
        return this;
    }
}
