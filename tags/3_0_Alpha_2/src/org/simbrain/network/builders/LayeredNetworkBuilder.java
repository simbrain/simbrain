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
package org.simbrain.network.builders;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.groups.NeuronLayer;
import org.simbrain.network.groups.NeuronLayer.LayerType;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neurons.ClampedNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * Adds a layered object to a network. The topology is specified by an array of
 * integers I1...In which correspond to the number of nodes in layers L1...Ln,
 * and where L1 is the input layer and Ln is the output layer.
 *
 * @author jeff yoshimi
 */
public final class LayeredNetworkBuilder {

    // TODO: Possibly abstract core concepts to a superclass
    // TODO: Default neurons, synapses (input / output special?). upper /lower
    // bounds
    // TODO: Possibly add "justification" (right, left, center) field
    // TODO: Allow Horizontal vs. vertical layout
    // TODO: Option: within layer recurrence?

    /**
     * Array of integers which determines the number of layers and nodes in each
     * layer. Integers 1...n in the array correspond to the number of nodes in
     * layers L1...Ln, and where L1 is the input layer and Ln is the output
     * layer.
     */
    private int[] nodesPerLayer = { 5, 3, 5 };

    /** Space to put between layers. */
    private int betweenLayerInterval = 100;

    /** Space between neurons within a layer. */
    private int betweenNeuronInterval = 50;

    /** Initial position of network (from bottom left). */
    Point2D.Double initialPosition = new Point2D.Double(0, 0);

    /** Whether to put each layer in a neuron group. */
    private boolean addGroups = true;

    /**
     * Construct the builder.
     */
    public LayeredNetworkBuilder() {
    }

    /**
     * Add the layered network to the specified network.
     *
     * @param network the parent network to which the layered network is being
     *            added
     */
    public void buildNetwork(final RootNetwork network) {

        // Layout
        LineLayout layout = new LineLayout(betweenNeuronInterval,
                LineOrientation.HORIZONTAL);

        // Set up input layer
        List<Neuron> inputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < nodesPerLayer[0]; i++) {
            Neuron neuron = new Neuron(network, new ClampedNeuron());
            neuron.setIncrement(1); // For easier testing
            neuron.setLowerBound(0);
            network.addNeuron(neuron);
            inputLayer.add(neuron);
        }
        layout.setInitialLocation(new Point((int) initialPosition.getX()
                - getWidth(inputLayer) / 2, (int) initialPosition.getY()));
        layout.layoutNeurons(inputLayer);
        if (addGroups) {
            NeuronLayer group = new NeuronLayer(network, inputLayer,
                    LayerType.Input);
            network.addGroup(group);
        }

        // Prepare base synapse for connecting layers
        Synapse synapse = Synapse.getTemplateSynapse(new ClampedSynapse());
        synapse.setLowerBound(-10);
        synapse.setUpperBound(10);

        // Memory of last layer created
        List<Neuron> lastLayer = inputLayer;

        // Make hidden layers and output layer
        for (int i = 1; i < nodesPerLayer.length; i++) {
            List<Neuron> hiddenLayer = new ArrayList<Neuron>();
            for (int j = 0; j < nodesPerLayer[i]; j++) {
                Neuron neuron = new Neuron(network, new SigmoidalNeuron());
                neuron.setLowerBound(0);
                neuron.setUpdatePriority(i);
                network.addNeuron(neuron);
                hiddenLayer.add(neuron);
            }

            int layerWidth = getWidth(hiddenLayer);
            layout.setInitialLocation(new Point((int) initialPosition.getX()
                    - layerWidth / 2, (int) initialPosition.getY()
                    - (betweenLayerInterval * i)));
            layout.layoutNeurons(hiddenLayer);
            if (addGroups) {
                if (i == nodesPerLayer.length - 1) {
                    NeuronLayer group = new NeuronLayer(network, hiddenLayer,
                            LayerType.Output);
                    network.addGroup(group);
                } else {
                    NeuronLayer group = new NeuronLayer(network, hiddenLayer,
                            LayerType.Hidden);
                    network.addGroup(group);
                }
            }

            // Connect input layer to hidden layer
            AllToAll connection = new AllToAll(network, lastLayer, hiddenLayer);
            connection.setBaseExcitatorySynapse(synapse);
            connection.setBaseInhibitorySynapse(synapse);
            connection.connectNeurons();

            // Reset last layer
            lastLayer = hiddenLayer;
        }

        // Randomize weights
        network.randomizeWeights();

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

    /**
     * @return the nodesPerLayer
     */
    public int[] getNodesPerLayer() {
        return nodesPerLayer;
    }

    /**
     * @param nodesPerLayer the nodesPerLayer to set
     */
    public void setNodesPerLayer(int[] nodesPerLayer) {
        this.nodesPerLayer = nodesPerLayer;
    }

    /**
     * @return the betweenLayerInterval
     */
    public int getBetweenLayerInterval() {
        return betweenLayerInterval;
    }

    /**
     * @param betweenLayerInterval the betweenLayerInterval to set
     */
    public void setBetweenLayerInterval(int betweenLayerInterval) {
        this.betweenLayerInterval = betweenLayerInterval;
    }

    /**
     * @return the betweenNeuronInterval
     */
    public int getBetweenNeuronInterval() {
        return betweenNeuronInterval;
    }

    /**
     * @param betweenNeuronInterval the betweenNeuronInterval to set
     */
    public void setBetweenNeuronInterval(int betweenNeuronInterval) {
        this.betweenNeuronInterval = betweenNeuronInterval;
    }

    /**
     * @return the initialPosition
     */
    public Point2D.Double getInitialPosition() {
        return initialPosition;
    }

    /**
     * @param initialPosition the initialPosition to set
     */
    public void setInitialPosition(Point2D.Double initialPosition) {
        this.initialPosition = initialPosition;
    }

    /**
     * @return the addGroups
     */
    public boolean isAddGroups() {
        return addGroups;
    }

    /**
     * @param addGroups the addGroups to set
     */
    public void setAddGroups(boolean addGroups) {
        this.addGroups = addGroups;
    }

}
