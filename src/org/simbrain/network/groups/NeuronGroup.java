/*
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
package org.simbrain.network.groups;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.RootNetwork;
import org.simbrain.network.core.Synapse;

/**
 * A group of neurons.
 */
public class NeuronGroup extends Group {

    /** The neurons in this group. */
    private final List<Neuron> neuronList = new CopyOnWriteArrayList<Neuron>();
    
    /** @see Group */
    public NeuronGroup(final RootNetwork net, final List<Neuron> neurons) {
        super(net);
        for (Neuron neuron : neurons) {
            addNeuron(neuron);
        }
        //Collections.sort(neuronList, Comparators.X_ORDER);
    }

    /**
     * Create a neuron group without any initial neurons.
     * 
     * @param root parent network
     */
    public NeuronGroup(final RootNetwork root) {
        super(root);
    }
    
    @Override
    public void delete() {
        if (isMarkedForDeletion()) {
            return;
        } else {
            setMarkedForDeletion(true);
        }
        for (Neuron neuron : neuronList) {
            getParentNetwork().removeNeuron(neuron);
        }
        if (hasParentGroup()) {
            if (getParentGroup() instanceof Subnetwork) {
                ((Subnetwork) getParentGroup()).removeNeuronGroup(this);
            }
            if (getParentGroup().isEmpty() && getParentGroup().isDeleteWhenEmpty()) {
                getParentNetwork().removeGroup(getParentGroup());
            }            
        }
    }
    
    @Override
    public void update() {
        getParentNetwork().updateNeurons(neuronList);
    }
    
    /**
     * @return a list of neurons
     */
    public List<Neuron> getNeuronList() {
        return Collections.unmodifiableList(neuronList);
    }

    /**
     * Set the update rule for the neurons in this group.
     *
     * @param base the neuron update rule to set.
     */
    public void setNeuronType(NeuronUpdateRule base) {
        for(Neuron neuron : neuronList) {
            neuron.setUpdateRule(base.deepCopy());
        }
    }

    /**
     * Returns true if the provided synapse is in the fan-in weight vector of
     * some node in this neuron group.
     * 
     * @param synapse the synapse to check
     * @return true if it's attached to a neuron in this group
     */
    public boolean inFanInOfSomeNode(final Synapse synapse) {
        boolean ret = false;
        for(Neuron neuron : neuronList) {
            if (neuron.getFanIn().contains(synapse)) {
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Randomize fan-in for all neurons in group.
     */
    public void randomizeIncomingWeights() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeFanIn();
        }
    }

    /**
     * Randomize all neurons in group.
     */
    public void randomize() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomize();
        }
    }

    /**
     * Randomize bias for all neurons in group.
     *
     * @param lower lower bound for randomization.
     * @param upper upper bound for randomization.
     */
    public void randomizeBiases(double lower, double upper) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeBias(lower, upper);
        }
    }

    /**
     * Add a neuron to group.
     * 
     * @param neuron neuron to add
     * @param fireEvent whether to fire a neuron added event
     */
    public void addNeuron(Neuron neuron, boolean fireEvent) {
        neuron.setId(getParentNetwork().getNeuronIdGenerator().getId());
        neuronList.add(neuron);
        neuron.setParentGroup(this);
        if (fireEvent) {
            getParentNetwork().fireNeuronAdded(neuron);            
        }
    }

    /**
     * Add neuron to group.
     * 
     * @param neuron neuron to add
     */
    public void addNeuron(Neuron neuron) {
        addNeuron(neuron, true);
    }

    /**
     * Delete the provided neuron.
     *
     * @param toDelete the neuron to delete
     */
    public void removeNeuron(Neuron toDelete) {
  
        neuronList.remove(toDelete);
        if (isEmpty() && isDeleteWhenEmpty()) {
            delete();
        }
        //getParent().fireGroupChanged(this, this);
    }

    @Override
    public String toString() {
        String ret = new String();
        ret += ("Neuron Group [" + getLabel() + "] Neuron group with "
                + this.getNeuronList().size() + " neuron(s)\n");
        return ret;
    }

    @Override
    public boolean isEmpty() {
        return neuronList.isEmpty();
    }

    /**
     * True if the group contains the specified neuron.
     *
     * @param n neuron to check for.
     * @return true if the group contains this neuron, false otherwise
     */
    public boolean containsNeuron(final Neuron n) {
        return neuronList.contains(n);
    }
    
    //TODO: Below don't take account of the actual width of neurons themselves.  Treats them as points.
    
    /**
     * Get the central x coordinate of this group, based on the positions of the neurons that
     * comprise it.
     *
     * @return the center x coordinate.
     */
    public double getCenterX() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for(Neuron neuron : neuronList) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
            if (neuron.getX() > max ) {
                max = neuron.getX();
            }
        }
        return min + (max - min) / 2;
    }

    /**
     * Get the central y coordinate of this group, based on the positions of the neurons that
     * comprise it.
     *
     * @return the center y coordinate.
     */
    public double getCenterY() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for(Neuron neuron : neuronList) {
            if (neuron.getY() < min) {
                min = neuron.getY();
            }
            if (neuron.getY() > max ) {
                max = neuron.getY();
            }
        }
        return min + (max - min) / 2;
    }
    
    /**
     * Return the width of this group, based on the positions of the neurons that
     * comprise it.
     *
     * @return the width of the group
     */    
    public double getWidth() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for(Neuron neuron : neuronList) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
            if (neuron.getX() > max ) {
                max = neuron.getX();
            }
        }
        return max - min;
    }

    /**
     * Return the height of this group, based on the positions of the neurons that
     * comprise it.
     *
     * @return the height of the group
     */    
    public double getHeight() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for(Neuron neuron : neuronList) {
            if (neuron.getY() < min) {
                min = neuron.getY();	
            }
            if (neuron.getY() > max ) {
                max = neuron.getY();
            }
        }
        return max - min;
    }
    
    /**
     * 
     * Translate all neurons (the only objects with position information).
     *
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    public void offset(final double offsetX, final double offsetY) {
        for (Neuron neuron : neuronList) {
            neuron.setX(neuron.getX() + offsetX);
            neuron.setY(neuron.getY() + offsetY);
        }
    }

}
