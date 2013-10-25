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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.util.SimbrainMath;
import org.simbrain.util.Utils;

/**
 * A group of neurons.
 */
public class NeuronGroup extends Group {

    /** Space between neurons within a layer. */
    private int betweenNeuronInterval = 50;

    /** The neurons in this group. */
    private final List<Neuron> neuronList = new CopyOnWriteArrayList<Neuron>();

    /** @see Group */
    public NeuronGroup(final Network net, final List<Neuron> neurons) {
        super(net);
        for (Neuron neuron : neurons) {
            addNeuron(neuron);
        }
        // Collections.sort(neuronList, Comparators.X_ORDER);
    }

    /**
     *
     * @param net
     * @param initialPosition
     * @param numNeurons
     */
    public NeuronGroup(final Network net, Point2D initialPosition,
            final int numNeurons) {
        super(net);
        // Layout
        LineLayout layout = new LineLayout(betweenNeuronInterval,
                LineOrientation.HORIZONTAL);

        for (int i = 0; i < numNeurons; i++) {
            addNeuron(new Neuron(net, new LinearRule()));
        }

        layout.setInitialLocation(initialPosition);
        layout.layoutNeurons(this.getNeuronList());
        // Collections.sort(neuronList, Comparators.X_ORDER);
    }

    /**
     * Create a neuron group without any initial neurons.
     *
     * @param root parent network
     */
    public NeuronGroup(final Network root) {
        super(root);
    }
    //TODO: Rename root
    /**
     * Copy constructor.
     * pass in network for cases where a group is pasted from one network to another
     *
     * @param root parent network
     */
    public NeuronGroup(final Network root, final NeuronGroup toCopy) {
        super(root);
        for(Neuron neuron : toCopy.getNeuronList()) {
            this.addNeuron(new Neuron(root, neuron));
        }
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
            if (getParentGroup().isEmpty()) {
                getParentNetwork().removeGroup(getParentGroup());
            }
        }
    }

    @Override
    public void update() {
        Network.updateNeurons(neuronList);
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
        for (Neuron neuron : neuronList) {
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
        for (Neuron neuron : neuronList) {
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
        getParentNetwork().fireNetworkChanged();
    }

    /**
     * Randomize fan-out for all neurons in group.
     */
    public void randomizeOutgoingWeights() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeFanOut();
        }
        getParentNetwork().fireNetworkChanged();
    }

    /**
     * Return flat list of fanins for all neurons in group.
     *
     * @return incoming weights
     */
    public List<Synapse> getIncomingWeights() {
        List<Synapse> retList = new ArrayList<Synapse>();
        for (Neuron neuron : this.getNeuronList()) {
            retList.addAll(neuron.getFanIn());
        }
        return retList;
    }

    /**
     * Return flat list of fanouts for all neurons in group.
     *
     * @return outgoing weights
     */
    public List<Synapse> getOutgoingWeights() {
        List<Synapse> retList = new ArrayList<Synapse>();
        for (Neuron neuron : this.getNeuronList()) {
            retList.addAll(neuron.getFanOut());
        }
        return retList;
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
        neuronList.add(neuron);
        neuron.setParentGroup(this);
        if (getParentNetwork() != null) {
            neuron.setId(getParentNetwork().getNeuronIdGenerator().getId());
            if (fireEvent) {
                getParentNetwork().fireNeuronAdded(neuron);
            }
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
        //System.out.println("NeuronGroup.removeNeuron" + toDelete);
        if (isEmpty()) {
            delete();
        }
        // getParent().fireGroupChanged(this, this);
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
     * Set activations of neurons using an array of doubles. Assumes the order
     * of the items in the array should match the order of items in the
     * neuronlist.
     *
     * Does not throw an exception if the provided input array and neuron
     * list do not match in size.
     *
     * @param inputs the input vector as a double array.
     */
    public void setActivations(double[] inputs) {
        int i = 0;
        for (Neuron neuron : neuronList) {
            if (i >= inputs.length) {
                break;
            }
            neuron.setInputValue(inputs[i++]);
        }
    }

    /**
     * Return activations as a double vector.
     *
     * @return activations
     */
    public double[] getActivations() {
        double[] retArray = new double[neuronList.size()];
        int i = 0;
        for (Neuron neuron : neuronList) {
            retArray[i++] = neuron.getActivation();
        }
        return retArray;
    }

    public double[] getBiases() {
        double[] retArray = SimbrainMath.zeroVector(neuronList.size());
        int i = 0;
        for (Neuron neuron : neuronList) {
            if (neuron.getUpdateRule() instanceof BiasedUpdateRule) {
                retArray[i++] = ((BiasedUpdateRule)neuron.getUpdateRule()).getBias();
            }
        }
        return retArray;
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

    // TODO: Below don't take account of the actual width of neurons themselves.
    // Treats them as points.

    /**
     * Get the central x coordinate of this group, based on the positions of the
     * neurons that comprise it.
     *
     * @return the center x coordinate.
     */
    public double getCenterX() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
            if (neuron.getX() > max) {
                max = neuron.getX();
            }
        }
        return min + (max - min) / 2;
    }

    /**
     * Get the central y coordinate of this group, based on the positions of the
     * neurons that comprise it.
     *
     * @return the center y coordinate.
     */
    public double getCenterY() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getY() < min) {
                min = neuron.getY();
            }
            if (neuron.getY() > max) {
                max = neuron.getY();
            }
        }
        return min + (max - min) / 2;
    }

    /**
     * Return the width of this group, based on the positions of the neurons
     * that comprise it.
     *
     * @return the width of the group
     */
    public double getWidth() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
            if (neuron.getX() > max) {
                max = neuron.getX();
            }
        }
        return max - min;
    }

    /**
     * Returns the maximum X position of this group based on the neurons that
     * comprise it.
     * @return the x position of the farthest right neuron in the group.
     */
    public double getMaxX(){
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getX() > max) {
                max = neuron.getX();
            }
        }
        return max;
    }

    /**
     * Returns the minimum X position of this group based on the neurons that
     * comprise it.
     * @return the x position of the farthest left neuron in the group.
     */
    public double getMinX(){
        double min = Double.POSITIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
        }
        return min;
    }

    /**
     * Returns the maximum Y position of this group based on the neurons that
     * comprise it.
     * @return the y position of the farthest north neuron in the group.
     */
    public double getMaxY() {
    	  double max = Double.NEGATIVE_INFINITY;
          for (Neuron neuron : neuronList) {
              if (neuron.getY() > max) {
                  max = neuron.getY();
              }
          }
          return max;
    }

    /**
     * Returns the minimum Y position of this group based on the neurons that
     * comprise it.
     * @return the y position of the farthest south neuron in the group.
     */
    public double getMinY(){
        double min = Double.POSITIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getY() < min) {
                min = neuron.getY();
            }
        }
        return min;
    }

    /**
     * Return the height of this group, based on the positions of the neurons
     * that comprise it.
     *
     * @return the height of the group
     */
    public double getHeight() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getY() < min) {
                min = neuron.getY();
            }
            if (neuron.getY() > max) {
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

    /**
     * Set all activations to 0.
     */
    public void clearActivations() {
        for (Neuron n : this.getNeuronList()) {
            n.clear();
        }
    }

    /**
     * Set all activations to specified values.
     *
     * @value the value to set neurons to
     */
    public void setActivationLevels(double value) {
        for (Neuron n : this.getNeuronList()) {
            n.setActivation(value);
        }
    }

    /**
     * Copy activations from one neuron group to this one.
     *
     * @param toCopy the group to copy activations from.
     */
    public void copyActivations(NeuronGroup toCopy) {
        int i = 0;
        for (Neuron neuron : toCopy.getNeuronList()) {
            if (i < neuronList.size()) {
                neuronList.get(i++).setActivation(neuron.getInputValue() + neuron.getActivation());
            }
        }
    }

    /**
     * Print activations as a vector.
     */
    public void printActivations() {
        System.out.println(Utils.doubleArrayToString(Network
                .getActivationVector(neuronList)));
    }

    @Override
    public String getUpdateMethodDesecription() {
        return "Update neurons";
    }

    /**
     * Apply any input values to the activations of the neurons in this group.
     */
    public void applyInputs() {
        for (Neuron neuron : getNeuronList()) {
            neuron.setActivation(neuron.getActivation()
                    + neuron.getInputValue());
        }
    }





}
