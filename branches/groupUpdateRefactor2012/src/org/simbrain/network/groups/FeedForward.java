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
package org.simbrain.network.groups;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neurons.ClampedNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * A standard feed-forward network, where a succession of neuron groups and
 * synapse groups are organized into layers.
 * 
 * @author Jeff Yoshimi
 */
public class FeedForward extends Subnetwork {

    /** Enumeration of layer types. */
    public enum LayerType {
        Input, Hidden, Output;
    }

    /** Space to put between layers. */
    private int betweenLayerInterval = 100;

    /** Space between neurons within a layer. */
    private int betweenNeuronInterval = 50;

    /** Initial position of network (from bottom left). */
    Point2D initialPosition = new Point2D.Double(0, 0);

    /**
     * Add the layered network to the specified network, with a specified number
     * of layers and nodes in each layer.
     * 
     * @param network the parent network to which the layered network is being
     *            added
     * @param nodesPerLayer an array of integers which determines the number of
     *            layers and neurons in each layer. Integers 1...n in the array
     *            correspond to the number of nodes in layers 1...n.
     * @param initialPosition upper left corner where network will be placed.
     */
    public FeedForward(final RootNetwork network, int[] nodesPerLayer,
            Point2D initialPosition) {
        super(network);
        
        this.initialPosition = initialPosition;
        setLabel("Layered Network");

        // Layout
        LineLayout layout = new LineLayout(betweenNeuronInterval,
                LineOrientation.HORIZONTAL);

        // Set up input layer
        List<Neuron> inputLayerNeurons = new ArrayList<Neuron>();
        for (int i = 0; i < nodesPerLayer[0]; i++) {
            Neuron neuron = new Neuron(network, new ClampedNeuron());
            neuron.setIncrement(1); // For easier testing
            neuron.setLowerBound(0);
            inputLayerNeurons.add(neuron);
        }
        NeuronGroup inputLayer = new NeuronGroup(network, inputLayerNeurons);
        addNeuronGroup(inputLayer);
        if (initialPosition == null) {
            initialPosition = new Point2D.Double(0,0);
            
        }
        layout.setInitialLocation(new Point((int) initialPosition.getX(), (int)
                initialPosition.getY()));
        layout.layoutNeurons(inputLayerNeurons);

        // Prepare base synapse for connecting layers
        Synapse synapse = Synapse.getTemplateSynapse(new ClampedSynapse());
        synapse.setLowerBound(-10);
        synapse.setUpperBound(10);

        // Memory of last layer created
        NeuronGroup lastLayer = inputLayer;

        // Make hidden layers and output layer
        for (int i = 1; i < nodesPerLayer.length; i++) {
            List<Neuron> hiddenLayerNeurons = new ArrayList<Neuron>();
            for (int j = 0; j < nodesPerLayer[i]; j++) {
                Neuron neuron = new Neuron(network, new SigmoidalNeuron());
                neuron.setLowerBound(0);
                neuron.setUpdatePriority(i);
                hiddenLayerNeurons.add(neuron);
            }

            layout.layoutNeurons(hiddenLayerNeurons);
            NeuronGroup hiddenLayer = new NeuronGroup(network, hiddenLayerNeurons); 
            addNeuronGroup(hiddenLayer);
            offsetNeuronGroup(lastLayer, hiddenLayer, "North", betweenLayerInterval);

            // Connect input layer to hidden layer
            List<Synapse> synapseList = new ArrayList<Synapse>();
            for (Neuron source : lastLayer.getNeuronList()) {
                for (Neuron target : hiddenLayerNeurons) {
                    Synapse newSynapse = synapse.instantiateTemplateSynapse(
                            source, target, network);
                    newSynapse.randomize();
                    synapseList.add(newSynapse);
                }
            }

            // Create synapse group
            addSynapseGroup(new SynapseGroup(this.getParentNetwork(), synapseList));

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

    public boolean getEnabled() {
        return false;
    }

    public void setEnabled(boolean enabled) {
    }

    @Override
    public void addSynapseGroup(SynapseGroup group) {
        super.addSynapseGroup(group);
        group.setLabel("Weights " + (getSynapseGroupCount()) + " > "
                + (getSynapseGroupCount() + 1));
    }

    @Override
    public void addNeuronGroup(NeuronGroup group) {
        super.addNeuronGroup(group);
        group.setLabel("Layer " + getNeuronGroupCount());
    }


}
