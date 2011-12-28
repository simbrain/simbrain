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

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.interfaces.Group;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.interfaces.UpdatableGroup;
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
 * @author Jeff Yoshimi
 */
public class LayeredNetwork extends Group implements UpdatableGroup {

    // TODO: Default neurons, synapses (input / output special?). upper /lower
    // bounds
    // TODO: Possibly add "justification" (right, left, center) field
    // TODO: Allow Horizontal vs. vertical layout
    // TODO: Option: within layer recurrence?

    // /**
    // * Array of integers which determines the number of layers and nodes in
    // each
    // * layer. Integers 1...n in the array correspond to the number of nodes in
    // * layers L1...Ln, and where L1 is the input layer and Ln is the output
    // * layer.
    // */
    // private int[] nodesPerLayer = { 5, 3, 5 };

    private final List<NeuronGroup> layers = new ArrayList<NeuronGroup>();

    private final List<SynapseGroup> connections = new ArrayList<SynapseGroup>();

    /** Enumeration of layer types. */
    public enum LayerType {
        Input, Hidden, Output;
    }

    /** Space to put between layers. */
    private int betweenLayerInterval = 100;

    /** Space between neurons within a layer. */
    private int betweenNeuronInterval = 50;

    /** Initial position of network (from bottom left). */
    Point2D.Double initialPosition = new Point2D.Double(0, 0);

    /**
     * Add the layered network to the specified network.
     * 
     * @param network the parent network to which the layered network is being
     *            added
     */
    public LayeredNetwork(final RootNetwork network, int[] nodesPerLayer,
            Point2D.Double initialPosition) {
        super(network);
        
        this.initialPosition = initialPosition;
        setLabel("Layered Network");

        // Layout
        LineLayout layout = new LineLayout(betweenNeuronInterval,
                LineOrientation.HORIZONTAL);

        // Set up input layer
        List<Neuron> inputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < nodesPerLayer[0]; i++) {
            Neuron neuron = new Neuron(network, new ClampedNeuron());
            neuron.setIncrement(1); // For easier testing
            neuron.setLowerBound(0);
            inputLayer.add(neuron);
        }
        NeuronGroup layer = new NeuronGroup(network, inputLayer);
        addLayer(layer);
        layout.setInitialLocation(new Point((int) initialPosition.getX()
                - getWidth(inputLayer) / 2, (int) initialPosition.getY()));
        layout.layoutNeurons(inputLayer);

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
                hiddenLayer.add(neuron);
            }

            int layerWidth = getWidth(hiddenLayer);
            layout.setInitialLocation(new Point((int) initialPosition.getX()
                    - layerWidth / 2, (int) initialPosition.getY()
                    - (betweenLayerInterval * i)));
            layout.layoutNeurons(hiddenLayer);
            addLayer(new NeuronGroup(network, hiddenLayer));

            // Connect input layer to hidden layer
            List<Synapse> synapseList = new ArrayList<Synapse>();
            for (Neuron source : lastLayer) {
                for (Neuron target : hiddenLayer) {
                    Synapse newSynapse = synapse.instantiateTemplateSynapse(
                            source, target, network);
                    //network.addSynapse(newSynapse); 
                    //Redo: think about what's lost by not calling the above. E.g. id generation.
                    newSynapse.randomize();
                    synapseList.add(newSynapse);
                }
            }

            // Create synapse group
            addSynapseLayer(new SynapseGroup(this.getParentNetwork(),
                    synapseList));

            // Reset last layer
            lastLayer = hiddenLayer;
        }

    }
    
    private void addSynapseLayer(SynapseGroup group) {
        group.setLabel("Weights " + (connections.size() + 1) + " > "
                + (connections.size() + 2));
        connections.add(group);
        group.setParentGroup(this);
    }
    
    
    /**
     * Add a layer.
     *
     * @param group the layer to add
     */
    private void addLayer(NeuronGroup group) {
        group.setLabel("Layer " + (layers.size() + 1));
        layers.add(group);
        group.setParentGroup(this);
    }
    
    //REDO: Don't like this being public...
    public void removeLayer(NeuronGroup layer) {
        layers.remove(layer);
        // REDO: group changed?
    }
    
    //REDO: Naming... weight layer / neuron layer
    public void removeWeightLayer(SynapseGroup weightLayer) {
        connections.remove(weightLayer);
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
    
    @Override
    public String toString() {
        String ret = new String();
        ret += ("Layered network with " + layers.size() + " layers.");
        return ret;
    }

    /**
     * @return the layers
     */
    public List<NeuronGroup> getLayers() {
        return layers;
    }

    public void update() {
        for (NeuronGroup layer : layers) {
            layer.updateNeurons();
        }
    }

    public boolean getEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setEnabled(boolean enabled) {
        // TODO Auto-generated method stub
        
    }
    
    

    //TODO
    public List <Neuron> getFlatNeuronList() {
        List<Neuron> ret = new ArrayList<Neuron>();
        for (NeuronGroup layer : layers) {
            ret.addAll(layer.getNeuronList());            
        }
        return ret;
    }

    @Override
    public boolean isEmpty() {
        return layers.isEmpty();
    }

    /**
     * @return the connections
     */
    public List<SynapseGroup> getWeightLayer() {
        return connections;
    }

}
