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
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * Creates a cascading network.
 *
 * @author jeff yoshimi
 */
public final class CascadingNetworkBuilder {

    // TODO: Make synapse type settable
    // TODO: Account for width of neuron
    // TODO: Allow for cascades in different directions
    // TODO: Add getRootNeuron method?

    /** Number of layers in the cascade. */
    private int numLayers = 5;

    /** Number of neurons per branch; sometimes called a "branching parameter". */
    private int numBrachesPerNeuron = 2;

    /** Horizontal spacing between top-most neurons in cascade. */
    private double horizontalSpacing = 50;

    /** Vertical spacing between layers of the cascade. */
    private double verticalSpacing = 100;

    /** Initial position of the root neuron in the cascade. */
    private Point initialPosition = new Point(50, 50);

    /** Type of neuron to use in network. */
    private static Neuron neuronType = new Neuron(new LinearNeuron());

    /** Reference to root network. */
    private RootNetwork network;

    /**
     * Create a cascading network builder with a specific number of layers and
     * branches per neuron.
     * 
     * @param numLayers
     *            number of layers
     * @param numBrachesPerNeuron
     *            brahnces per neuron
     * @param network
     *            parent network
     */
    public CascadingNetworkBuilder(RootNetwork network, int numLayers,
            int numBrachesPerNeuron) {
        this.network = network;
        this.numLayers = numLayers;
        this.numBrachesPerNeuron = numBrachesPerNeuron;
    }

    /**
     * Create a cascading network builder using default values.
     *
     * @param network parent network
     */
    public CascadingNetworkBuilder(RootNetwork network) {
        this.network = network;
    }

    /**
     * Create the cascading network.
     */
    public void buildNetwork() {

        Neuron firstNeuron = neuronType.duplicate();
        List<Neuron> currentLayer = new ArrayList<Neuron>();
        firstNeuron.setPosition(initialPosition);
        currentLayer.add(firstNeuron);

        // Layout values
        int numNeuronsLastLayer = (int) Math
                .pow(numBrachesPerNeuron, numLayers);
        double totalSpace = numNeuronsLastLayer * horizontalSpacing;

        // Iterate through layers
        for (int layerIndex = 1; layerIndex <= numLayers; layerIndex++) {

            // Make a list of neurons for this layer (these will be base neurons
            // for the next layer)
            List<Neuron> tempList = new ArrayList<Neuron>();

            // Layout stuff
            double layerSpacing = totalSpace
                    / (int) Math.pow(numBrachesPerNeuron, layerIndex);
            double branchWidth = layerSpacing * (numBrachesPerNeuron - 1);

            // For each neuron in the current layer, add a branch (a set of
            // target neurons)
            for (Neuron baseNeuron : currentLayer) {
                network.addNeuron(baseNeuron);
                double initialXOffset = branchWidth / 2;
                for (int j = 0; j < numBrachesPerNeuron; j++) {
                    Neuron targetNeuron = neuronType.duplicate();
                    targetNeuron.setLocation(baseNeuron.getX() - initialXOffset
                            + (j * layerSpacing), initialPosition.y
                            - (layerIndex * verticalSpacing));
                    tempList.add(targetNeuron);
                    network.addNeuron(targetNeuron);
                    targetNeuron.setUpdatePriority(layerIndex);
                    ClampedSynapse synapse = new ClampedSynapse(baseNeuron,
                            targetNeuron);
                    network.addSynapse(synapse);
                }
            }
            currentLayer = tempList;
        }

    }

    /**
     * @return the numLayers
     */
    public int getNumLayers() {
        return numLayers;
    }

    /**
     * @param numLayers
     *            the numLayers to set
     */
    public void setNumLayers(int numLayers) {
        this.numLayers = numLayers;
    }

    /**
     * @return the numBrachesPerNeuron
     */
    public int getNumBrachesPerNeuron() {
        return numBrachesPerNeuron;
    }

    /**
     * @param numBrachesPerNeuron
     *            the numBrachesPerNeuron to set
     */
    public void setNumBrachesPerNeuron(int numBrachesPerNeuron) {
        this.numBrachesPerNeuron = numBrachesPerNeuron;
    }

    /**
     * @return the horizontalSpacing
     */
    public double getHorizontalSpacing() {
        return horizontalSpacing;
    }

    /**
     * @param horizontalSpacing
     *            the horizontalSpacing to set
     */
    public void setHorizontalSpacing(double horizontalSpacing) {
        this.horizontalSpacing = horizontalSpacing;
    }

    /**
     * @return the verticalSpacing
     */
    public double getVerticalSpacing() {
        return verticalSpacing;
    }

    /**
     * @param verticalSpacing
     *            the verticalSpacing to set
     */
    public void setVerticalSpacing(double verticalSpacing) {
        this.verticalSpacing = verticalSpacing;
    }

    /**
     * @return the initialPosition
     */
    public Point getInitialPosition() {
        return initialPosition;
    }

    /**
     * @param initialPosition
     *            the initialPosition to set
     */
    public void setInitialPosition(Point initialPosition) {
        this.initialPosition = initialPosition;
    }

    /**
     * @param neuronType the neuronType to set
     */
    public static void setNeuronType(Neuron type) {
        neuronType = type;
    }

}
