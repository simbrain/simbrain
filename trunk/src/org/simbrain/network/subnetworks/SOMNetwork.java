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

import java.awt.geom.Point2D;
import java.util.List;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.network.util.NetworkLayoutManager;
import org.simbrain.network.util.NetworkLayoutManager.Direction;

/**
 * <b>SOMNetwork</b> is a small network encompassing an SOM group. An input
 * layer and input data have been added so that the SOM can be easily trained
 * using existing Simbrain GUI tools
 *
 * @author Jeff Yoshimi
 */
public class SOMNetwork  extends Subnetwork implements Trainable {

    /** The self organizing map. */
    private final SOMGroup som;

    /** The input layer. */
    private final NeuronGroup inputLayer;

    /** Training set. */
    private final TrainingSet trainingSet = new TrainingSet();

    /**
     * Construct an SOM Network.
     *
     * @param net parent network. Set to null when this is used simply as a
     *            holder for param values.
     * @param numSOMNeurons number of neurons in the SOM layer
     * @param numInputNeurons number of neurons in the input layer
     * @param initialPosition bottom corner where network will be placed.
     */
    public SOMNetwork(Network net, int numSOMNeurons, int numInputNeurons,
            Point2D initialPosition) {
        super(net);
        this.setLabel("SOM Network");
        som = new SOMGroup(net, numSOMNeurons);
        inputLayer = new NeuronGroup(net, initialPosition, numInputNeurons);
        inputLayer.setLayoutBasedOnSize();
        if (net == null) {
            return;
        }
        this.addNeuronGroup(som);
        this.addNeuronGroup(inputLayer);
        for (Neuron neuron : inputLayer.getNeuronList()) {
            neuron.setLowerBound(0);
        }
        inputLayer.setLabel("Input layer");
        inputLayer.setClamped(true);
        this.connectNeuronGroups(inputLayer, som);
        layoutNetwork();
    }

    /**
     * Set the layout of the network.
     */
    public void layoutNetwork() {
        // TODO: Would be easy to set the layout and redo it...
        NetworkLayoutManager.offsetNeuronGroup(inputLayer, som,
                Direction.NORTH, 150);
    }

    @Override
    public List<Neuron> getInputNeurons() {
        return inputLayer.getNeuronList();
    }

    @Override
    public List<Neuron> getOutputNeurons() {
        return som.getNeuronList();
    }

    @Override
    public TrainingSet getTrainingSet() {
        return trainingSet;
    }

    @Override
    public void initNetwork() {
        // No implementation
    }

    /**
     * @return the som
     */
    public SOMGroup getSom() {
        return som;
    }

    /**
     * @return the inputLayer
     */
    public NeuronGroup getInputLayer() {
        return inputLayer;
    }

}
