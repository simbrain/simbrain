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

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.simbrain.network.core.*;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collection of loose neurons (neurons in a {@link NeuronGroup} can be added to a collection).
 * Allows them to be labelled, moved around as a unit, coupled to, etc.   However no special processing
 * occurs in neuron collections.  They are a convenience.  NeuronCollections can overlap each other.
 */
public class NeuronCollection implements AttributeContainer, ArrayConnectable {

    /**
     * Reference to the network this group is a part of.
     */
    private final Network parentNetwork;

    //TODO: Consider a hash set instead.
    /**
     * References to neurons in this collection
     */
    private final List<Neuron> neuronList = new ArrayList<Neuron>(500);

    /**
     * Name of this group.
     */
    @UserParameter(label = "ID", description = "Id of this group", order = -1, editable = false)
    private String id;

    /**
     * Name of this group. Null strings lead to default labeling conventions.
     */
    @UserParameter(label = "Label", description = "Group label", useSetter = true,
            order = 10)
    private String label = "";

    /**
     * If true, when the group is added to the network its id will not be used as its label.
     */
    private boolean useCustomLabel = false;

    /**
     * Array to hold activation values for any caller that needs the activation values for this group in array form.
     * Lazy... activations are only written (and this array is only initialized) when {@link #getActivations()} is
     * called.
     */
    private double [] activations;

    /**
     * Support for property change events.
     */
    protected transient PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    private PropertyChangeListener networkListener;

    /**
     * Construct a new neuron group from a list of neurons.
     *
     * @param net     the network
     * @param neurons the neurons
     */
    public NeuronCollection(final Network net, final List<? extends Neuron> neurons) {
        parentNetwork = net;
        neuronList.addAll(neurons);
        initializeId();
        PropertyChangeListener networkListener = evt -> {
            if ("neuronRemoved".equals(evt.getPropertyName())) {
                Neuron removed = (Neuron) evt.getOldValue();
                neuronList.remove(removed);
                // If that was the last neuron in the list, remove the neuron collection
                if (neuronList.size() == 0) {
                    delete();
                }
            }
        };
        net.addPropertyChangeListener(networkListener);
    }

    /**
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
        firePositionChanged();
    }

    public void addNeuron(Neuron neuron, boolean fireEvent) {
        neuronList.add(neuron);
        if (fireEvent) {
            changeSupport.firePropertyChange("add", this, null);
        }
    }

    /**
     * Call after deleting neuron collection from parent network.
     */
    public void delete() {
        changeSupport.firePropertyChange("delete", this, null);
        parentNetwork.removePropertyChangeListener(networkListener);
    }

    /**
     * Set the update rule for the neurons in this group.
     *
     * @param base the neuron update rule to set.
     */
    public void setNeuronType(NeuronUpdateRule base) {
        neuronList.forEach(n -> n.setUpdateRule(base.deepCopy()));
    }

    public List<Neuron> getNeuronList() {
        return Collections.unmodifiableList(neuronList);
    }

    /**
     * Set the string update rule for the neurons in this group.
     *
     * @param rule the neuron update rule to set.
     */
    public void setNeuronType(String rule) {
        try {
            NeuronUpdateRule newRule = (NeuronUpdateRule) Class.forName("org.simbrain.network.neuron_update_rules." + rule).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        setNeuronType(rule);
    }

    /**
     * Randomize fan-in for all neurons in group.
     */
    public void randomizeIncomingWeights() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeFanIn();
        }
        getParentNetwork().fireSynapsesUpdated(getIncomingWeights());
    }

    /**
     * Randomize fan-out for all neurons in group.
     */
    public void randomizeOutgoingWeights() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeFanOut();
        }
        getParentNetwork().fireSynapsesUpdated(getOutgoingWeights());
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
            retList.addAll(neuron.getFanOut().values());
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
     * Initialize the id for this group. A default label based on the id is also set. This is overridden by {@link
     * Subnetwork} so that sub-groups also are given ids.
     */
    public void initializeId() {
        id = getParentNetwork().getCollectionIdGenerator().getId();
        if (!useCustomLabel) {
            label = id.replaceAll("_", " ");
        }
    }

    @Producible(defaultVisibility = false)
    public String getLabel() {
        return label;
    }

    /**
     * Set the label. This prevents the group id being used as the label for new groups.  If null or empty labels are
     * sent in then the group label is used.
     */
    @Consumable(defaultVisibility = false)
    public void setLabel(String label) {
        if (label == null || label.isEmpty()) {
            useCustomLabel = false;
        } else {
            useCustomLabel = true;
        }
        String oldLabel = this.label;
        this.label = label;
        changeSupport.firePropertyChange("label", oldLabel, label);
    }

    /**
     * Node positions within group changed and GUI should be notified of this change.
     */
    public void firePositionChanged() {
        changeSupport.firePropertyChange("moved", null, null);
    }

    @Override
    public String toString() {
        String ret = new String();
        ret += ("Neuron Collection [" + getLabel() + "]. Neuron collection with " + this.getNeuronList().size() + " neuron(s)" + ". Located at (" + Utils.round(this.getPosition().x, 2) + "," + Utils.round(this.getPosition().y, 2) + ").\n");
        return ret;
    }

    public boolean isEmpty() {
        return neuronList.isEmpty();
    }

    /**
     * Force set activations of neurons using an array of doubles. Assumes the order of the items in the array should
     * match the order of items in the neuronlist.
     * <p>
     * Does not throw an exception if the provided input array and neuron list do not match in size.
     *
     * @param inputs the input vector as a double array.
     */
    @Consumable()
    public void forceSetActivations(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).forceSetActivation(inputs[i]);
        }
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

    /**
     * @return the number of neurons in the group
     */
    public int size() {
        return neuronList.size();
    }

    /**
     * Returns all the neurons in this group within a certain radius of the given neuron. This method will never return
     * the given neuron as part of the list of neurons within the given radius, nor will it return neurons with the
     * exact same position as the given neuron as a part of the returned list.
     *
     * @param n      the neurons
     * @param radius the radius to search within.
     * @return neurons in the group within a certain radius
     */
    public List<Neuron> getNeuronsInRadius(Neuron n, int radius) {
        ArrayList<Neuron> ret = new ArrayList<Neuron>((int) (size() / 0.75f));
        for (Neuron potN : neuronList) {
            double dist = Network.getEuclideanDist(n, potN);
            if (dist <= radius && dist != 0) {
                ret.add(potN);
            }
        }
        return ret;
    }

    // TODO: Below don't take account of the actual width of neurons themselves.
    // Treats them as points.

    /**
     * Get the central x coordinate of this group, based on the positions of the neurons that comprise it.
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
     * Get the central y coordinate of this group, based on the positions of the neurons that comprise it.
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
     * Returns the maximum X position of this group based on the neurons that comprise it.
     *
     * @return the x position of the farthest right neuron in the group.
     */
    public double getMaxX() {
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getX() > max) {
                max = neuron.getX();
            }
        }
        return max;
    }

    /**
     * Returns the minimum X position of this group based on the neurons that comprise it.
     *
     * @return the x position of the farthest left neuron in the group.
     */
    public double getMinX() {
        double min = Double.POSITIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
        }
        return min;
    }

    /**
     * Returns the maximum Y position of this group based on the neurons that comprise it.
     *
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
     * Returns the minimum Y position of this group based on the neurons that comprise it.
     *
     * @return the y position of the farthest south neuron in the group.
     */
    public double getMinY() {
        double min = Double.POSITIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getY() < min) {
                min = neuron.getY();
            }
        }
        return min;
    }

    /**
     * Return the width of this group, based on the positions of the neurons that comprise it.
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
     * Return the height of this group, based on the positions of the neurons that comprise it.
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
     * @return the longest dimensions upon which neurons are laid out.
     */
    public double getMaxDim() {
        if (getWidth() > getHeight()) {
            return getWidth();
        } else {
            return getHeight();
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
     * Set clamping on all neurons in this group.
     *
     * @param clamp true to clamp them, false otherwise
     */
    public void setClamped(final boolean clamp) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setClamped(clamp);
        }
    }

    /**
     * Set all activations to a specified value.
     *
     * @param value the value to set the neurons to
     */
    public void setActivationLevels(final double value) {
        for (Neuron n : getNeuronList()) {
            n.setActivation(value);
        }
    }

    /**
     * Force set all activations to a specified value.
     *
     * @param value the value to set the neurons to
     */
    public void forceSetActivationLevels(final double value) {
        for (Neuron n : getNeuronList()) {
            n.forceSetActivation(value);
        }
    }

    /**
     * Copy activations from one neuron group to this one.
     *
     * @param toCopy the group to copy activations from.
     */
    public void copyActivations(NeuronCollection toCopy) {
        int i = 0;
        for (Neuron neuron : toCopy.getNeuronList()) {
            if (i < neuronList.size()) {
                neuronList.get(i).setActivation(neuron.getInputValue() + neuron.getActivation());
                neuronList.get(i++).setSpike(neuron.isSpike());

            }
        }
    }

    /**
     * Print activations as a vector.
     */
    public void printActivations() {
        System.out.println(Utils.doubleArrayToString(Network.getActivationVector(neuronList)));
    }

    /**
     * Return current position (upper left corner of neuron in the farthest north-west position.
     *
     * @return position upper left position of group
     */
    public Point2D.Double getPosition() {
        return new Point2D.Double(getMinX(), getMinY());
    }

    /**
     * Returns true if all the neurons in this group are clamped.
     *
     * @return true if all neurons are clamped, false otherwise
     */
    public boolean isAllClamped() {
        boolean ret = true;
        for (Neuron n : getNeuronList()) {
            if (!n.isClamped()) {
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Returns true if all the neurons in this group are unclamped.
     *
     * @return true if all neurons are unclamped, false otherwise
     */
    public boolean isAllUnclamped() {
        boolean ret = true;
        for (Neuron n : getNeuronList()) {
            if (n.isClamped()) {
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Set the lower bound on all neurons in this group.
     *
     * @param lb the lower bound to set.
     */
    public void setLowerBound(double lb) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setLowerBound(lb);
        }
    }

    /**
     * Set the upper bound on all neurons in this group.
     *
     * @param ub the upper bound to set.
     */
    public void setUpperBound(double ub) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setUpperBound(ub);
        }
    }

    /**
     * Set the increment on all neurons in this group.
     *
     * @param increment the increment to set.
     */
    public void setIncrement(double increment) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setIncrement(increment);
        }
    }

    /**
     * Utility to method (used in couplings) to get a string showing the labels of all "active" neurons (neurons with
     * activation above a threshold).
     *
     * @param threshold threshold above which to consider a neuron "active"
     * @return the "active labels"
     */
    public String getLabelsOfActiveNeurons(double threshold) {
        StringBuilder strBuilder = new StringBuilder("");
        for (Neuron neuron : neuronList) {
            if ((neuron.getActivation() > threshold) && (!neuron.getLabel().isEmpty())) {
                strBuilder.append(neuron.getLabel() + " ");
            }
        }
        return strBuilder.toString();
    }

    /**
     * Returns the label of the most active neuron.
     *
     * @return the label of the most active neuron
     */
    public String getMostActiveNeuron() {
        double min = Double.MIN_VALUE;
        String result = "";
        for (Neuron neuron : neuronList) {
            if ((neuron.getActivation() > min) && (!neuron.getLabel().isEmpty())) {
                result = neuron.getLabel();
                min = neuron.getActivation();
            }
        }
        return result + " ";
    }

    /**
     * Sets the polarities of every neuron in the group.
     *
     * @param p
     */
    public void setPolarity(SimbrainConstants.Polarity p) {
        for (Neuron n : neuronList) {
            n.setPolarity(p);
        }
    }

    /**
     * Get the neuron with the specified label, or null if none found.
     *
     * @param label label to search for
     * @return the associated neuron
     */
    public Neuron getNeuronByLabel(String label) {
        for (Neuron neuron : this.getNeuronList()) {
            if (neuron.getLabel().equalsIgnoreCase(label)) {
                return neuron;
            }
        }
        return null;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public String getId() {
        return id;
    }


    /**
     * Set input values of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     * <p>
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     *
     * @param inputs the input vector as a double array.
     */
    @Consumable()
    public void setInputValues(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).setInputValue(inputs[i]);
        }
    }

    /**
     * Set activations of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     * <p>
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     *
     * @param inputs the input vector as a double array.
     */
    @Consumable()
    public void setActivations(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).setActivation(inputs[i]);
        }
    }

    /**
     * Return activations as a double array in place.
     *
     * @return the activation array
     */
    @Producible(arrayDescriptionMethod = "getLabelArray")
    public double[] getActivations() {
        if (activations == null) {
            activations = new double[size()];
        }
        for (int ii=0; ii<size(); ++ii) {
            activations[ii] = neuronList.get(ii).getActivation();
        }
        return activations;
    }


    /**
     * Returns an array of labels, one for each neuron this group.
     * Called by reflection for some coupling related events.
     *
     * @return the label array
     */
    public String[] getLabelArray() {
        String[] retArray = new String[getNeuronList().size()];
        int i = 0;
        for(Neuron neuron : getNeuronList()) {
            if (neuron.getLabel().isEmpty()) {
                retArray[i++] = neuron.getId();
            } else {
                retArray[i++] = neuron.getLabel();
            }
        }
        return retArray;
    }

    public Network getParentNetwork() {
        return parentNetwork;
    }

    /**
     * Returns the summed hash codes of contained neurons.  Used to prevent creating neuron collections
     * from identical neurons.
     *
     * @return summed hash
     */
    public int getSummedNeuronHash() {
        return neuronList.stream().mapToInt(n -> n.hashCode()).sum();
    }

    @Override
    public INDArray getActivationArray() {
        float[] floatActivation = new float[getActivations().length];
        // Potential performance cost, but no clear way around this
        for (int i = 0; i < getActivations().length; i++) {
            floatActivation[i] = (float) getActivations()[i];
        }
        return Nd4j.create(new int[]{floatActivation.length}, floatActivation);

    }

    @Override
    public void setActivationArray(INDArray activations) {
        setActivations(activations.toDoubleVector());
    }

    @Override
    public long arraySize() {
        return neuronList.size();
    }
}
