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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.matrix.NeuronArray;
import org.simbrain.network.matrix.WeightMatrix;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.util.Direction;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static org.simbrain.network.util.NetworkLayoutManagerKt.offsetNeuronGroup;

/**
 * A standard feed-forward network, where a succession of neuron groups and
 * synapse groups are organized into layers.
 *
 * @author Jeff Yoshimi
 */
public class FeedForward extends Subnetwork {

    /**
     * Space to put between layers.
     */
    private int betweenLayerInterval = 250;

    /**
     * Ordered reference to underlying array list.
     */
    private List<NeuronArray> layerList = new ArrayList<>();

    /**
     * Reference to input layer.
     */
    private NeuronArray inputLayer;

    /**
     * Reference to output layer.
     */
    private NeuronArray outputLayer;

    /**
     * Construct a feed-forward network.
     *
     * @param network             the parent network to which the layered network is being added
     * @param nodesPerLayer       an array of integers which determines the number of layers and
     *                            neurons in each layer. Integers 1...n in the array correspond
     *                            to the number of nodes in layers 1...n.
     * @param initialPosition     bottom corner where network will be placed.
     * @param inputNeuronTemplate the type of Neuron to use for the input layer
     */
    public FeedForward(final Network network, int[] nodesPerLayer, Point2D initialPosition,
                       final NeuronUpdateRule inputNeuronTemplate) {
        super(network);
        buildNetwork(network, nodesPerLayer, initialPosition, inputNeuronTemplate);
        super.events.onDeleted(nm -> {
            if (nm instanceof NeuronArray) {
                layerList.remove((NeuronArray) nm);
            }
        });
    }

    /**
     * Add the layered network to the specified network, with a specified number
     * of layers and nodes in each layer.
     *
     * @param network         the parent network to which the layered network is being added
     * @param nodesPerLayer   an array of integers which determines the number of layers and
     *                        neurons in each layer. Integers 1...n in the array correspond
     *                        to the number of nodes in layers 1...n.
     * @param initialPosition upper left corner where network will be placed.
     */
    public FeedForward(final Network network, int[] nodesPerLayer, Point2D initialPosition) {
        super(network);
        LinearRule rule = new LinearRule();
        rule.setLowerBound(0);
        buildNetwork(network, nodesPerLayer, initialPosition, rule);
    }

    /**
     * Create the network using the parameters.
     *
     * @param network             the parent network to which the layered network is being added
     * @param nodesPerLayer       an array of integers which determines the number of layers and
     *                            neurons in each layer. Integers 1...n in the array correspond
     *                            to the number of nodes in layers 1...n.
     * @param initialPosition     bottom corner where network will be placed.
     * @param inputNeuronTemplate the type of Neuron to use for the input layer
     */
    private void buildNetwork(final Network network, int[] nodesPerLayer, Point2D initialPosition,
                              final NeuronUpdateRule inputNeuronTemplate) {

        setLabel("Layered Network");

        inputLayer = new NeuronArray(network, nodesPerLayer[0]);
        addModel(inputLayer);
        layerList.add(inputLayer);

        // Memory of last layer created
        NeuronArray lastLayer = inputLayer;

        // Make hidden layers and output layer
        for (int i = 1; i < nodesPerLayer.length; i++) {
            NeuronArray hiddenLayer = new NeuronArray(network, nodesPerLayer[i]);
            addModel(hiddenLayer);
            layerList.add(hiddenLayer);
            offsetNeuronGroup(lastLayer, hiddenLayer, Direction.NORTH, betweenLayerInterval / 2, 100, 200);

            // Add weight matrix
            WeightMatrix wm = new WeightMatrix(getParentNetwork(), lastLayer, hiddenLayer);
            wm.randomize();
            addModel(wm);

            // Reset last layer
            lastLayer = hiddenLayer;
        }
        outputLayer = lastLayer;

    }

    public int getBetweenLayerInterval() {
        return betweenLayerInterval;
    }

    public void setBetweenLayerInterval(int betweenLayerInterval) {
        this.betweenLayerInterval = betweenLayerInterval;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void onCommit() {
    }

    @Override
    public void update() {

        inputLayer.updateInputs();
        inputLayer.update();
        for (int i = 1; i < layerList.size()-1; i++) {
            layerList.get(i).updateInputs();
            layerList.get(i).update();
        }
        outputLayer.updateInputs();
        outputLayer.update();
    }

}
