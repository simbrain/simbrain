/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.subnetworks;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.network.util.NetworkLayoutManager;
import org.simbrain.network.util.NetworkLayoutManager.Direction;

/**
 * A standard feed-forward network, where a succession of neuron groups and
 * synapse groups are organized into layers.
 *
 * @author Jeff Yoshimi
 */
public class FeedForward extends Subnetwork {

    /** Space to put between layers. */
    private int betweenLayerInterval = 150;

    /**
     * Construct a feed-forward network.
     *
     * @param network
     *            the parent network to which the layered network is being added
     * @param nodesPerLayer
     *            an array of integers which determines the number of layers and
     *            neurons in each layer. Integers 1...n in the array correspond
     *            to the number of nodes in layers 1...n.
     * @param initialPosition
     *            bottom corner where network will be placed.
     * @param inputNeuronTemplate
     *            the type of Neuron to use for the input layer
     */
    public FeedForward(final Network network, int[] nodesPerLayer,
        Point2D initialPosition, final Neuron inputNeuronTemplate) {
        super(network);
        buildNetwork(network, nodesPerLayer, initialPosition,
            inputNeuronTemplate);
    }

    /**
     * Add the layered network to the specified network, with a specified number
     * of layers and nodes in each layer.
     *
     * @param network
     *            the parent network to which the layered network is being added
     * @param nodesPerLayer
     *            an array of integers which determines the number of layers and
     *            neurons in each layer. Integers 1...n in the array correspond
     *            to the number of nodes in layers 1...n.
     * @param initialPosition
     *            upper left corner where network will be placed.
     */
    public FeedForward(final Network network, int[] nodesPerLayer,
        Point2D initialPosition) {
        super(network);
        LinearRule rule = new LinearRule();
        Neuron neuron = new Neuron(network, rule);
        rule.setIncrement(1); // For easier testing
        rule.setLowerBound(0);
        buildNetwork(network, nodesPerLayer, initialPosition, neuron);
    }

    /**
     * Create the network using the parameters.
     *
     * @param network
     *            the parent network to which the layered network is being added
     * @param nodesPerLayer
     *            an array of integers which determines the number of layers and
     *            neurons in each layer. Integers 1...n in the array correspond
     *            to the number of nodes in layers 1...n.
     * @param initialPosition
     *            bottom corner where network will be placed.
     * @param inputNeuronTemplate
     *            the type of Neuron to use for the input layer
     */
    private void buildNetwork(final Network network, int[] nodesPerLayer,
        Point2D initialPosition, final Neuron inputNeuronTemplate) {

        setLabel("Layered Network");

        // Set up input layer
        List<Neuron> inputLayerNeurons = new ArrayList<Neuron>();
        for (int i = 0; i < nodesPerLayer[0]; i++) {
            inputLayerNeurons.add(new Neuron(network, inputNeuronTemplate));
        }
        NeuronGroup inputLayer = new NeuronGroup(network, inputLayerNeurons);
        inputLayer.setClamped(true); // Clamping makes everything easier in the
                                     // GUI. The trainer uses forceset.
        addNeuronGroup(inputLayer);
        inputLayer.setLayoutBasedOnSize(initialPosition);

        // Prepare base synapse for connecting layers
        Synapse synapse = Synapse.getTemplateSynapse(new StaticSynapseRule());
        synapse.setLowerBound(-1);
        synapse.setUpperBound(1);

        // Memory of last layer created
        NeuronGroup lastLayer = inputLayer;

        // Make hidden layers and output layer
        for (int i = 1; i < nodesPerLayer.length; i++) {
            List<Neuron> hiddenLayerNeurons = new ArrayList<Neuron>();
            for (int j = 0; j < nodesPerLayer[i]; j++) {
                SigmoidalRule rule = new SigmoidalRule();
                Neuron neuron = new Neuron(network, rule);
                rule.setLowerBound(0);
                neuron.setUpdatePriority(i);
                hiddenLayerNeurons.add(neuron);
            }

            NeuronGroup hiddenLayer = new NeuronGroup(network,
                hiddenLayerNeurons);
            hiddenLayer.setLayoutBasedOnSize();
            addNeuronGroup(hiddenLayer);
            NetworkLayoutManager.offsetNeuronGroup(lastLayer, hiddenLayer,
                Direction.NORTH, betweenLayerInterval);

            AllToAll connection = new AllToAll();
            SynapseGroup lh = connectNeuronGroups(lastLayer, hiddenLayer,
                connection);
            lh.randomizeConnectionWeights();

            // Reset last layer
            lastLayer = hiddenLayer;
        }
    }

    /**
     * @return the betweenLayerInterval
     */
    public int getBetweenLayerInterval() {
        return betweenLayerInterval;
    }

    /**
     * @param betweenLayerInterval
     *            the betweenLayerInterval to set
     */
    public void setBetweenLayerInterval(int betweenLayerInterval) {
        this.betweenLayerInterval = betweenLayerInterval;
    }

    @Override
    public void addNeuronGroup(NeuronGroup group) {
        super.addNeuronGroup(group);
        group.setLabel("Layer " + getNeuronGroupCount());
    }

    /**
     * Returns the input layer.
     *
     * @return the input layer
     */
    public NeuronGroup getInputLayer() {
        return getNeuronGroup(0);
    }

    /**
     * Returns the output layer.
     *
     * @return the output layer
     */
    public NeuronGroup getOutputLayer() {
        return getNeuronGroup(getNeuronGroupCount() - 1);
    }

    /**
     * Convenience method for getting the neurons associated with the input
     * group. Also allows all feed-forward networks to implement Trainable.
     *
     * @return the input layer neurons as a list.
     */
    public List<Neuron> getInputNeurons() {
        return getInputLayer().getNeuronList();
    }

    /**
     * Convenience method for getting the neurons associated with the output
     * group. Also allows all feed-forward networks to implement Trainable.
     *
     * @return the output layer neurons as a list.
     */
    public List<Neuron> getOutputNeurons() {
        return getOutputLayer().getNeuronList();
    }

    @Override
    public String getUpdateMethodDesecription() {
        return "Layered update";
    }

}
