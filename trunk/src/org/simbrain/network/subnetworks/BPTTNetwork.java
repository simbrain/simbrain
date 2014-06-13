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

import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.network.trainers.TrainingSet;

/**
 * Implements a network to be trained using backprop through time.
 *
 * @author Jeff Yoshimi
 */
public final class BPTTNetwork extends FeedForward implements Trainable {

    /** Number of steps in each trained sequences. */
    private int stepsPerSequences = 5;

    /**
     * Training set.
     */
    private final TrainingSet trainingSet = new TrainingSet();

    /**
     * Constructor specifying root network, and number of nodes in each layer.
     *
     * @param network underlying network
     * @param numInputNodes number of nodes in the input layer
     * @param numHiddenNodes number of nodes in the hidden and context layers
     * @param numOutputNodes number of output nodes
     * @param initialPosition where to position the network (upper left)
     */
    public BPTTNetwork(final Network network, int numInputNodes,
            int numHiddenNodes, int numOutputNodes, Point2D initialPosition) {
        super(network, new int[] { numInputNodes, numHiddenNodes,
                numOutputNodes }, initialPosition);

        this.getInputLayer().setNeuronType(new LinearRule());
        setLabel("BPTT");

    }

    @Override
    public void initNetwork() {
        clearActivations();
    }

    @Override
    public void update() {
        getHiddenLayer().update();
        getOutputLayer().update();
        getInputLayer().copyActivations(getOutputLayer());
        getInputLayer().applyInputs();
    }

    /**
     * Returns the hidden layer.
     *
     * @return the hidden layer
     */
    public NeuronGroup getHiddenLayer() {
        return this.getNeuronGroup(1);
    }

    @Override
    public TrainingSet getTrainingSet() {
        return trainingSet;
    }

    /**
     * @return the stepsPerSequences
     */
    public int getStepsPerSequences() {
        return stepsPerSequences;
    }

    /**
     * @param stepsPerSequences the stepsPerSequences to set
     */
    public void setStepsPerSequences(int stepsPerSequences) {
        this.stepsPerSequences = stepsPerSequences;
    }

    @Override
    public String getUpdateMethodDesecription() {
        return "Hidden layer, output layer, copy output to input";
    }

}
