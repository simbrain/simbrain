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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.util.Utils;

/**
 * A group of neurons. A primary abstraction for larger network structures.
 * Layers in feed-forward networks are neuron groups. Self-organizing-maps
 * subclass this class. Etc.
 */
public class NeuronGroup extends Group {

    public static final int DEFAULT_GROUP_SIZE = 10;

    /** The neurons in this group. */
    private List<Neuron> neuronList = new ArrayList<Neuron>(500);

    /** Default layout for neuron groups. */
    public static final Layout DEFAULT_LAYOUT = new LineLayout(50,
        LineOrientation.HORIZONTAL);

    /** The layout for the neurons in this group. */
    private Layout layout = DEFAULT_LAYOUT;

    /** Set of incoming synapse groups. */
    private final HashSet<SynapseGroup> incomingSgs =
        new HashSet<SynapseGroup>();

    /** Set of outgoing synapse groups. */
    private final HashSet<SynapseGroup> outgoingSgs =
        new HashSet<SynapseGroup>();

    /**
     * In method setLayoutBasedOnSize, this is used as the threshold number of
     * neurons in the group, above which to use grid layout instead of line
     * layout.
     */
    private int gridThreshold = 30;

    /** Space between neurons within a layer. */
    private int betweenNeuronInterval = 50;

    /** Data (input vectors) for testing the network. */
    private double[][] testData;

    /**
     * Construct a new neuron group from a list of neurons.
     * 
     * @param net
     *            the network
     * @param neurons
     *            the neurons
     */
    public NeuronGroup(final Network net, final List<Neuron> neurons) {
        super(net);
        neuronList = new ArrayList<Neuron>(neurons.size());
        for (Neuron neuron : neurons) {
            addNeuron(neuron);
        }
        neuronList = new CopyOnWriteArrayList<Neuron>(neuronList);
    }

    /**
     * Construct a new neuron group with a specified number of neurons.
     * 
     * @param net
     *            parent network
     * @param numNeurons
     *            how many neurons it will have
     */
    public NeuronGroup(final Network net, final int numNeurons) {
        this(net, new Point2D.Double(0, 0), numNeurons);
    }

    /**
     * Construct a new neuron group with a specified number of neurons.
     * 
     * @param net
     *            parent network
     * @param initialPosition
     *            initial location of the group
     * @param numNeurons
     *            how many neurons it will have
     */
    public NeuronGroup(final Network net, Point2D initialPosition,
        final int numNeurons) {
        super(net);
        neuronList = new ArrayList<Neuron>(numNeurons);
        for (int i = 0; i < numNeurons; i++) {
            addNeuron(new Neuron(net), false);
        }
        neuronList = new CopyOnWriteArrayList<Neuron>(neuronList);
        layout.setInitialLocation(initialPosition);
        layout.layoutNeurons(this.getNeuronList());
    }

    /**
     * Create a neuron group without any initial neurons and an initial
     * position.
     * 
     * @param network
     *            parent network
     * @param initialPosition
     *            the starting position from which to lay-out the neurons in the
     *            group whenever they are added.
     */
    public NeuronGroup(final Network network, Point2D initialPosition) {
        super(network);
        layout.setInitialLocation(initialPosition);
    }

    /**
     * Create a neuron group without any initial neurons.
     * 
     * @param network
     *            parent network
     */
    public NeuronGroup(final Network network) {
        super(network);
    }

    /**
     * Copy constructor. pass in network for cases where a group is pasted from
     * one network to another
     * 
     * @param network
     *            parent network
     * @param toCopy
     *            the neuron group this will become a (deep) copy of.
     */
    public NeuronGroup(final Network network, final NeuronGroup toCopy) {
        super(network);
        for (Neuron neuron : toCopy.getNeuronList()) {
            this.addNeuron(new Neuron(network, neuron));
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
            neuron.setParentGroup(null);
            neuron.getNetwork().removeNeuron(neuron);
        }
        if (hasParentGroup()) {
            if (getParentGroup() instanceof Subnetwork) {
                ((Subnetwork) getParentGroup()).removeNeuronGroup(this);
            }
            if (getParentGroup().isEmpty()) {
                getParentNetwork().removeGroup(getParentGroup());
            }
        }
        neuronList.clear();
        Runtime.getRuntime().gc();
    }

    @Override
    public void update() {
        Network.updateNeurons(neuronList);
    }

    /**
     * @return the neurons in this group.
     */
    public List<Neuron> getNeuronList() {
        return Collections.unmodifiableList(neuronList);
    }

    /**
     * Set the update rule for the neurons in this group.
     * 
     * @param base
     *            the neuron update rule to set.
     */
    public void setNeuronType(NeuronUpdateRule base) {
        for (Neuron neuron : neuronList) {
            neuron.setUpdateRule(base.deepCopy());
        }
    }

    /**
     * Set the string update rule for the neurons in this group.
     * 
     * @param rule
     *            the neuron update rule to set.
     */
    public void setNeuronType(String rule) {
        for (Neuron neuron : neuronList) {
            neuron.setUpdateRule(rule);
        }
    }

    /**
     * Return a human-readable name for this type of neuron group. Subclasses
     * should override this. Used in the Gui for various purposes.
     * 
     * @return the name of this type of neuron group.
     */
    public String getTypeDescription() {
        return "Neuron Group";
    }

    /**
     * Returns true if the provided synapse is in the fan-in weight vector of
     * some node in this neuron group.
     * 
     * @param synapse
     *            the synapse to check
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
     * @param lower
     *            lower bound for randomization.
     * @param upper
     *            upper bound for randomization.
     */
    public void randomizeBiases(double lower, double upper) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeBias(lower, upper);
        }
    }

    /**
     * Add a neuron to group.
     * 
     * @param neuron
     *            neuron to add
     * @param fireEvent
     *            whether to fire a neuron added event
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
     * @param neuron
     *            neuron to add
     */
    public void addNeuron(Neuron neuron) {
        addNeuron(neuron, true);
    }

    /**
     * Delete the provided neuron.
     * 
     * @param toDelete
     *            the neuron to delete
     */
    public void removeNeuron(Neuron toDelete) {
        neuronList.remove(toDelete);
        if (isEmpty()) {
            delete();
        }
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
     * of the items in the array matches the order of items in the neuronlist.
     * 
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     * 
     * @param inputs
     *            the input vector as a double array.
     */
    public void setActivations(double[] inputs) {
        int i = 0;
        for (Neuron neuron : neuronList) {
            if (i >= inputs.length) {
                break;
            }
            neuron.setActivation(inputs[i++]);
        }
    }

    /**
     * Set input values of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     * 
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     * 
     * @param inputs
     *            the input vector as a double array.
     */
    public void setInputValues(double[] inputs) {
        int i = 0;
        for (Neuron neuron : neuronList) {
            if (i >= inputs.length) {
                break;
            }
            neuron.setInputValue(inputs[i++]);
        }
    }

    /**
     * Force set activations of neurons using an array of doubles. Assumes the
     * order of the items in the array should match the order of items in the
     * neuronlist.
     * 
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     * 
     * @param inputs
     *            the input vector as a double array.
     */
    public void forceSetActivations(double[] inputs) {
        int i = 0;
        for (Neuron neuron : neuronList) {
            if (i >= inputs.length) {
                break;
            }
            neuron.forceSetActivation(inputs[i++]);
        }
    }

    /**
     * Return activations as a double array.
     * 
     * @return the activation array
     */
    public double[] getActivations() {
        double[] retArray = new double[neuronList.size()];
        int i = 0;
        for (Neuron neuron : neuronList) {
            retArray[i++] = neuron.getActivation();
        }
        return retArray;
    }

    /**
     * Return biases as a double array.
     * 
     * @return the bias array
     */
    public double[] getBiases() {
        double[] retArray = new double[neuronList.size()];
        int i = 0;
        for (Neuron neuron : neuronList) {
            if (neuron.getUpdateRule() instanceof BiasedUpdateRule) {
                retArray[i++] = ((BiasedUpdateRule) neuron.getUpdateRule())
                    .getBias();
            }
        }
        return retArray;
    }

    /**
     * True if the group contains the specified neuron.
     * 
     * @param n
     *            neuron to check for.
     * @return true if the group contains this neuron, false otherwise
     */
    public boolean containsNeuron(final Neuron n) {
        return neuronList.contains(n);
    }

    /**
     * @return the number of neurons in the group
     */
    @Override
    public int size() {
        return neuronList.size();
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
     * Returns the maximum X position of this group based on the neurons that
     * comprise it.
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
     * Returns the minimum X position of this group based on the neurons that
     * comprise it.
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
     * Returns the maximum Y position of this group based on the neurons that
     * comprise it.
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
     * Returns the minimum Y position of this group based on the neurons that
     * comprise it.
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

    public double getMaxDim() {
        if (getWidth() > getHeight()) {
            return getWidth();
        } else {
            return getHeight();
        }
    }

    /**
     * 
     * @return
     */
    public Point2D[] getFourCorners() {
        double centerX = getCenterX();
        double centerY = getCenterY();

        Point2D[] corners = new Point2D[4];

        corners[0] = new Point2D.Double(getMaxX() - centerX, getMaxY()
            - centerY);
        corners[3] = new Point2D.Double(getMaxX() - centerX, getMinY()
            - centerY);
        corners[2] = new Point2D.Double(getMinX() - centerX, getMinY()
            - centerY);
        corners[1] = new Point2D.Double(getMinX() - centerX, getMaxY()
            - centerY);

        return corners;
    }

    /**
     * 
     * @param x
     *            x coordinate for neuron group
     * @param y
     *            y coordinate for neuron group
     */
    public void setLocation(final double x, final double y) {
        offset(-this.getMinX(), -this.getMinY());
        offset(x, y);
    }

    /**
     * Translate all neurons (the only objects with position information).
     * 
     * @param offsetX
     *            x offset for translation.
     * @param offsetY
     *            y offset for translation.
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
     * Set clamping on all neurons in this group.
     * 
     * @param clamp
     *            true to clamp them, false otherwise
     */
    public void setClamped(final boolean clamp) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setClamped(clamp);
        }
    }

    /**
     * Set all activations to a specified value.
     * 
     * @param value
     *            the value to set the neurons to
     */
    public void setActivationLevels(final double value) {
        for (Neuron n : getNeuronList()) {
            n.setActivation(value);
        }
    }

    /**
     * Force set all activations to a specified value.
     * 
     * @param value
     *            the value to set the neurons to
     */
    public void forceSetActivationLevels(final double value) {
        for (Neuron n : getNeuronList()) {
            n.forceSetActivation(value);
        }
    }

    /**
     * Copy activations from one neuron group to this one.
     * 
     * @param toCopy
     *            the group to copy activations from.
     */
    public void copyActivations(NeuronGroup toCopy) {
        int i = 0;
        for (Neuron neuron : toCopy.getNeuronList()) {
            if (i < neuronList.size()) {
                neuronList.get(i++).setActivation(
                    neuron.getInputValue() + neuron.getActivation());
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

    /**
     * @return the layout
     */
    public Layout getLayout() {
        return layout;
    }

    /**
     * Set the layout. Does not apply it. Call apply layout for that.
     * 
     * @param layout
     *            the layout to set
     */
    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    /**
     * Return current position (upper left corner of neuron in the farthest
     * north-west position.
     * 
     * @return position upper left position of group
     */
    public Point2D.Double getPosition() {
        return new Point2D.Double(getMinX(), getMinY());
    }

    /**
     * Apply this group's layout to its neurons.
     */
    public void applyLayout() {
        layout.setInitialLocation(getPosition());
        layout.layoutNeurons(getNeuronList());
    }

    /**
     * Apply this group's layout to its neurons based on a specified initial
     * position.
     * 
     * @param initialPosition
     *            the position from which to begin the layout.
     */
    public void applyLayout(Point2D initialPosition) {
        layout.setInitialLocation(initialPosition);
        layout.layoutNeurons(getNeuronList());
    }

    public HashSet<SynapseGroup> getIncomingSgs() {
        return new HashSet<SynapseGroup>(incomingSgs);
    }

    public HashSet<SynapseGroup> getOutgoingSg() {
        return new HashSet<SynapseGroup>(outgoingSgs);
    }

    public boolean containsAsIncoming(SynapseGroup sg) {
        return incomingSgs.contains(sg);
    }

    public boolean containsAsOutgoing(SynapseGroup sg) {
        return outgoingSgs.contains(sg);
    }

    public void addIncomingSg(SynapseGroup sg) {
        incomingSgs.add(sg);
    }

    public void addOutgoingSg(SynapseGroup sg) {
        outgoingSgs.add(sg);
    }

    public boolean removeIncomingSg(SynapseGroup sg) {
        return incomingSgs.remove(sg);
    }

    public boolean removeOutgoingSg(SynapseGroup sg) {
        return outgoingSgs.remove(sg);
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
     * @param lb
     *            the lower bound to set.
     */
    public void setLowerBound(double lb) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setLowerBound(lb);
        }
    }

    /**
     * Set the upper bound on all neurons in this group.
     * 
     * @param ub
     *            the upper bound to set.
     */
    public void setUpperBound(double ub) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setUpperBound(ub);
        }
    }

    /**
     * Set the increment on all neurons in this group.
     * 
     * @param increment
     *            the increment to set.
     */
    public void setIncrement(double increment) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setIncrement(increment);
        }
    }

    /**
     * @return the testData
     */
    public double[][] getTestData() {
        return testData;
    }

    /**
     * @param testData
     *            the testData to set
     */
    public void setTestData(double[][] testData) {
        this.testData = testData;
    }

    /**
     * If more than gridThreshold neurons use a grid layout, else a horizontal
     * line layout.
     */
    public void setLayoutBasedOnSize() {
        setLayoutBasedOnSize(new Point2D.Double(0, 0));
    }

    /**
     * If more than gridThreshold neurons use a grid layout, else a horizontal
     * line layout.
     * 
     * @param initialPosition
     *            the initial Position for the layout
     */
    public void setLayoutBasedOnSize(Point2D initialPosition) {
        if (initialPosition == null) {
            initialPosition = new Point2D.Double(0, 0);
        }
        LineLayout lineLayout = new LineLayout(betweenNeuronInterval,
            LineOrientation.HORIZONTAL);
        GridLayout gridLayout = new GridLayout(betweenNeuronInterval,
            betweenNeuronInterval, 10);
        if (neuronList.size() < gridThreshold) {
            lineLayout.setInitialLocation(initialPosition);
            setLayout(lineLayout);
        } else {
            gridLayout.setInitialLocation(initialPosition);
            setLayout(gridLayout);
        }
        // Used rather than apply layout to make sure initial position is used.
        getLayout().layoutNeurons(neuronList);
    }

    /**
     * @return the betweenNeuronInterval
     */
    public int getBetweenNeuronInterval() {
        return betweenNeuronInterval;
    }

    /**
     * @param betweenNeuronInterval
     *            the betweenNeuronInterval to set
     */
    public void setBetweenNeuronInterval(int betweenNeuronInterval) {
        this.betweenNeuronInterval = betweenNeuronInterval;
    }

    /**
     * @return the gridThreshold
     */
    public int getGridThreshold() {
        return gridThreshold;
    }

    /**
     * @param gridThreshold
     *            the gridThreshold to set
     */
    public void setGridThreshold(int gridThreshold) {
        this.gridThreshold = gridThreshold;
    }

    /**
     * Clear the neuron list.
     */
    public void clearNeuronList() {
        neuronList.clear();
    }

}
