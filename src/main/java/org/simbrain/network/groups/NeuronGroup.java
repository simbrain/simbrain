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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.nodes.NeuronGroupNode;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.UpdateRuleEnum;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.network.subnetworks.CompetitiveGroup;
import org.simbrain.network.subnetworks.SOMGroup;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * A group of neurons. A primary abstraction for larger network structures.
 * Layers in feed-forward networks are neuron groups. Self-organizing-maps
 * subclass this class. Etc.
 *<br>
 * Updating is done using a collection of neurons, but in a NeuronGroup they must all be the same type. This allows
 * the group to be characterized as spiking vs. non-spiking, for example, and serves other functions.
 * Because this constraint was added after neuron groups were first introduced, it is not yet rigorously enforced
 * in the code.
 *
 */
public class NeuronGroup extends AbstractNeuronCollection {

    /**
     * The number of neurons in the group by default.
     */
    public static final int DEFAULT_GROUP_SIZE = 10;

    /**
     * "Prototype" update rule.
     */
    @UserParameter(label = "Group Update Rule", useSetter = true, order = 20)
    private UpdateRuleEnum groupUpdateRule;

    /**
     * Default layout for neuron groups.
     */
    public static final Layout DEFAULT_LAYOUT = new GridLayout();

    /**
     * The layout for the neurons in this group.
     */
    private Layout.LayoutObject layout = new Layout.LayoutObject();

    /**
     * Set of incoming synapse groups.
     */
    private final HashSet<SynapseGroup> incomingSgs = new HashSet<SynapseGroup>();

    /**
     * Set of outgoing synapse groups.
     */
    private final HashSet<SynapseGroup> outgoingSgs = new HashSet<SynapseGroup>();

    /**
     * In method setLayoutBasedOnSize, this is used as the threshold number of
     * neurons in the group, above which to use grid layout instead of line
     * layout.
     */
    private int gridThreshold = 9;

    /**
     * Space between neurons within a layer.
     */
    private int betweenNeuronInterval = 50;

    /**
     * Construct a new neuron group from a list of neurons.
     *
     * @param net     the network
     * @param neurons the neurons
     */
    public NeuronGroup(final Network net, final List<Neuron> neurons) {
        super(net);
        addNeurons(neurons);
        subsamplingManager.resetIndices();
    }

    /**
     * Construct a new neuron group with a specified number of neurons.
     *
     * @param net parent network
     * @param numNeurons how many neurons it will have
     */
    public NeuronGroup(final Network net, final int numNeurons) {
        this(net, new Point2D.Double(0, 0), numNeurons);
    }

    /**
     * Construct a new neuron group with a specified number of neurons.
     *
     * @param net             parent network
     * @param initialPosition initial location of the group
     * @param numNeurons      how many neurons it will have
     */
    public NeuronGroup(final Network net, Point2D initialPosition, final int numNeurons) {
        super(net);
        List<Neuron> newNeurons = new ArrayList<>();
        for (int i = 0; i < numNeurons; i++) {
            Neuron newNeuron = new Neuron(net);
            newNeurons.add(newNeuron);
        }
        // Very slow to add to a copy on write array list so do it this way
        addNeurons(newNeurons);
        layout.getLayout().setInitialLocation(initialPosition);
        layout.getLayout().layoutNeurons(this.getNeuronList());
        firePositionChanged();
        subsamplingManager.resetIndices();
    }

    /**
     * Create a neuron group without any initial neurons and an initial
     * position.
     *
     * @param network         parent network
     * @param initialPosition the starting position from which to lay-out the neurons in the
     *                        group whenever they are added.
     */
    public NeuronGroup(final Network network, Point2D initialPosition) {
        super(network);
        layout.getLayout().setInitialLocation(initialPosition);
        subsamplingManager.resetIndices();
    }

    /**
     * Create a neuron group without any initial neurons.
     *
     * @param network parent network
     */
    public NeuronGroup(final Network network) {
        super(network);
    }

    /**
     * Copy constructor. pass in network for cases where a group is pasted from
     * one network to another
     *
     * @param network parent network
     * @param toCopy  the neuron group this will become a (deep) copy of.
     */
    public NeuronGroup(final Network network, final NeuronGroup toCopy) {
        super(network);
        List<Neuron> newNeurons = new ArrayList<>();
        for (Neuron neuron : toCopy.getNeuronList()) {
            newNeurons.add(new Neuron(network, neuron));
        }
        addNeurons(newNeurons);
        this.setLayout(toCopy.getLayout());
        this.setGroupUpdateRule(toCopy.groupUpdateRule);
        subsamplingManager.resetIndices();
    }

    /**
     * Construct a neuron group with a specified number of neurons and id.
     */
    public NeuronGroup(Network network, int numNeurons, String id) {
        super(network);
        for (int i = 0; i < numNeurons ; i++) {
            addNeuron(new Neuron(network, new LinearRule()));
        }
        super.id = id;
    }

    /**
     * Returns a deep copy of the neuron group with a new network parent
     *
     * @param newParent the new parent network for this group, potentially different from the original (used when
     *                  copying and pasting from one network to another)
     */
    public NeuronGroup deepCopy(Network newParent) {
        return new NeuronGroup(newParent, this);
    }

    public void delete() {

        for (Neuron neuron : getNeuronList()) {
            neuron.getNetwork().removeNeuron(neuron, true);
        }

        // TODO:
        //if (hasParentGroup()) {
        //    if (getParentGroup() instanceof Subnetwork) {
        //        ((Subnetwork) getParentGroup()).removeNeuronGroup(this);
        //    }
        //    if (getParentGroup().isEmpty()) {
        //        getParentNetwork().removeGroup(getParentGroup());
        //    }
        //}

        activationRecorder.stopRecording();
        removeAllNeurons();
        changeSupport.firePropertyChange("delete", this, null);
    }

    /**
     * Updates all the neurons in the neuron group according to their
     * NeuronUpdateRule(s). If the group is in input mode reads in the next
     * set of values from the input table and sets the neuron values
     * accordingly.
     */
    @Override
    public void update() {
        super.update();
        if (!inputMode) {
            Network.updateNeurons(getNeuronList());
        }
        //fireLabelUpdated(); // TODO
    }

    /**
     * Set the update rule for the neurons in this group.
     *
     * @param base the neuron update rule to set.
     */
    public void setNeuronType(NeuronUpdateRule base) {
        inputManager.setInputSpikes(base.isSpikingNeuron());
        for (Neuron neuron : getNeuronList()) {
            neuron.setUpdateRule(base.deepCopy());
        }
    }

    /**
     * Set the update rule using {@link UpdateRuleEnum}.
     */
    public void setGroupUpdateRule(UpdateRuleEnum rule) {
        groupUpdateRule = rule;
        try {
            setNeuronType(rule.getRule().getConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the string update rule for the neurons in this group.
     *
     * @param rule the neuron update rule to set.
     */
    public void setNeuronType(String rule) {
        try {
            NeuronUpdateRule newRule = (NeuronUpdateRule) Class.forName("org.simbrain.network.neuron_update_rules." + rule).newInstance();
            inputManager.setInputSpikes(newRule.isSpikingNeuron());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        for (Neuron neuron : getNeuronList()) {
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
     * @param synapse the synapse to check
     * @return true if it's attached to a neuron in this group
     */
    public boolean inFanInOfSomeNode(final Synapse synapse) {
        boolean ret = false;
        for (Neuron neuron : getNeuronList()) {
            if (neuron.getFanIn().contains(synapse)) {
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Add a neuron to group.
     *
     * @param neuron    neuron to add
     * @param fireEvent whether to fire a neuron added event
     */
    public void addNeuron(Neuron neuron, boolean fireEvent) {
        super.addNeuron(neuron);
        neuron.setParentGroup(this);
        if (getParentNetwork() != null) {
            neuron.setId(getParentNetwork().getNeuronIdGenerator().getId());
            if (fireEvent) {
//                getParentNetwork().fireNeuronAdded(neuron); // TODO: [event] let synapse handle this
            }
        }
        if (fireEvent) {
            subsamplingManager.resetIndices();
        }
    }

    @Override
    public void addNeurons(Collection<Neuron> neurons) {
        groupUpdateRule = UpdateRuleEnum.get(neurons.iterator().next().getUpdateRule());
        // TODO: Throw exception if not same type
        super.addNeurons(neurons);
        neurons.forEach(n -> n.setParentGroup(this));
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
     * Removes all neurons with no incoming or outgoing synapses from the group.
     */
    public void prune() {
        Iterator<Neuron> reaper = getNeuronList().iterator();
        while (reaper.hasNext()) {
            Neuron n = reaper.next();
            if (n.getFanIn().size() == 0 && n.getFanOut().size() == 0) {
                reaper.remove();
            }
        }
    }

    @Override
    public String toString() {
        String ret = new String();
        ret += ("Neuron Group [" + getLabel() + "]. Neuron group with " + this.getNeuronList().size() + " neuron(s)" + ". Located at (" + Utils.round(this.getPosition().x, 2) + "," + Utils.round(this.getPosition().y, 2) + ").\n");
        //ret += layout.toString();
        return ret;
    }

    /**
     * Returns an array of spike indices used in couplings, (e.g. to a raster
     * plot). For example, if a neuron group has 9 neurons, and neurons 1 and 4
     * just spiked, the producer will send the list (1,0,0,4,0,0,0,0,0).
     *
     * @return the spike index array
     */
    @Producible()
    public double[] getSpikeIndexes() {
        List<Double> inds = new ArrayList<Double>(size());
        int i = 0;
        for (Neuron n : getNeuronList()) {
            if (n.isSpike()) {
                inds.add((double) i);
            }
            i++;
        }
        double[] vals = new double[inds.size()];
        int j = 0;
        for (Double d : inds) {
            vals[j++] = d;
        }
        return vals;
    }

    /**
     * Return biases as a double array.
     *
     * @return the bias array
     */
    public double[] getBiases() {
        double[] retArray = new double[getNeuronList().size()];
        int i = 0;
        for (Neuron neuron : getNeuronList()) {
            if (neuron.getUpdateRule() instanceof BiasedUpdateRule) {
                retArray[i++] = ((BiasedUpdateRule) neuron.getUpdateRule()).getBias();
            }
        }
        return retArray;
    }

    /**
     * @return the spatial positions of the corners of the neuron group
     * (X and Y only)
     */
    public List<Point2D> getFourCorners() {
        return Arrays.asList(
                new Point2D.Double(getMaxX(), getMaxY()),
                new Point2D.Double(getMaxX(), getMinY()),
                new Point2D.Double(getMinX(), getMinY()),
                new Point2D.Double(getMinX(), getMaxY())
        );
    }

    public Map<NeuronGroupNode.Port, Point2D> getMidPointOfFourEdges() {
        List<Point2D> c = getFourCorners();
        Map<NeuronGroupNode.Port, Point2D> portPointMap = new HashMap<>();

        portPointMap.put(NeuronGroupNode.Port.EAST, SimbrainMath.midpoint(c.get(0), c.get(1)));
        portPointMap.put(NeuronGroupNode.Port.SOUTH, SimbrainMath.midpoint(c.get(1), c.get(2)));
        portPointMap.put(NeuronGroupNode.Port.WEST, SimbrainMath.midpoint(c.get(2), c.get(3)));
        portPointMap.put(NeuronGroupNode.Port.NORTH, SimbrainMath.midpoint(c.get(3), c.get(0)));

        return portPointMap;

    }

    /**
     * @param x x coordinate for neuron group
     * @param y y coordinate for neuron group
     */
    public void setLocation(final double x, final double y) {
        offset(-this.getMinX(), -this.getMinY());
        offset(x, y);
    }

    @Override
    public Point2D getLocation() {
        return new Point2D.Double(getMinX(), getMinY());
    }

    public void setLocation(Point2D point) {
        setLocation(point.getX(), point.getY());
    }

    /**
     * Apply any input values to the activations of the neurons in this group.
     */
    public void applyInputs() {
        for (Neuron neuron : getNeuronList()) {
            neuron.setActivation(neuron.getActivation() + neuron.getInputValue());
        }
    }

    public Layout getLayout() {
        return layout.getLayout();
    }

    public Layout.LayoutObject getLayoutObject() {
        return layout;
    }

    public void setLayoutObject(Layout.LayoutObject layoutObject) {
        this.layout = layoutObject;
    }

    public void setLayout(Layout layout) {
        this.layout.setLayout(layout);
    }

    /**
     * Apply this group's layout to its neurons.
     */
    public void applyLayout() {
        layout.getLayout().layoutNeurons(getNeuronList());
        firePositionChanged();
    }

    /**
     * Apply this group's layout to its neurons based on a specified initial
     * position.
     *
     * @param initialPosition the position from which to begin the layout.
     */
    public void applyLayout(Point2D initialPosition) {
        layout.getLayout().setInitialLocation(initialPosition);
        layout.getLayout().layoutNeurons(getNeuronList());
        firePositionChanged();
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
     * @param initialPosition the initial Position for the layout
     */
    public void setLayoutBasedOnSize(Point2D initialPosition) {
        if (initialPosition == null) {
            initialPosition = new Point2D.Double(0, 0);
        }
        LineLayout lineLayout = new LineLayout(betweenNeuronInterval, LineOrientation.HORIZONTAL);
        GridLayout gridLayout = new GridLayout(betweenNeuronInterval, betweenNeuronInterval);
        if (getNeuronList().size() < gridThreshold) {
            lineLayout.setInitialLocation(initialPosition);
            setLayout(lineLayout);
        } else {
            gridLayout.setInitialLocation(initialPosition);
            setLayout(gridLayout);
        }
        // Used rather than apply layout to make sure initial position is used.
        getLayout().layoutNeurons(getNeuronList());
    }

    public int getBetweenNeuronInterval() {
        return betweenNeuronInterval;
    }

    public void setBetweenNeuronInterval(int betweenNeuronInterval) {
        this.betweenNeuronInterval = betweenNeuronInterval;
    }

    public int getGridThreshold() {
        return gridThreshold;
    }

    public void setGridThreshold(int gridThreshold) {
        this.gridThreshold = gridThreshold;
    }

    public boolean isInputMode() {
        return inputMode;
    }

    // TODO
    public boolean isSpikingNeuronGroup() {
        return inputManager.isInputSpikes();
    }

    @Override
    public EditableObject copy() {
        return this.deepCopy(this.getParentNetwork());
    }

    /**
     * Helper class for creating new neuron groups using {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     */
    public static class NeuronGroupCreator implements EditableObject {

        @UserParameter(label = "Number of neurons", description = "How many neurons this neuron group should have", order = -1)
        int numNeurons = 20;

        /**
         * A label for this Neuron Group for display purposes.
         */
        @UserParameter(
                label = "Label",
                initialValueMethod = "getLabel"
        )
        private String label;

        /**
         * Initial update rule
         */
        @UserParameter(label = "Update Rule")
        private UpdateRuleEnum updateRule = UpdateRuleEnum.LINEAR;
        //todo conditional enable based on group type

        @UserParameter(label = "Group type")
        private GroupEnum groupType = GroupEnum.DEFAULT;

        /**
         * Create the template with a proposed label
         */
        public NeuronGroupCreator(String proposedLabel) {
            this.label = proposedLabel;
        }

        /**
         * Add a neuron array to network created from field values which should be setup by an Annotated Property
         * Editor.
         *
         * @param network the network this neuron array adds to
         *
         * @return the created neuron array
         */
        public NeuronGroup create(Network network) {
            NeuronGroup ng = null;
            if (groupType == GroupEnum.DEFAULT) {
                ng = new NeuronGroup(network, numNeurons,
                        network.getArrayIdGenerator().getId());
                ng.setGroupUpdateRule(updateRule);
            } else if (groupType == GroupEnum.WTA) {
                ng = new WinnerTakeAll(network, numNeurons);
            } else if (groupType == GroupEnum.COMPETITIVE) {
                ng = new CompetitiveGroup(network, numNeurons);
            } else if (groupType == GroupEnum.SOM) {
                ng = new SOMGroup(network, numNeurons);
            }
            return ng;
        }

        /**
         * Getter called by reflection by {@link UserParameter#initialValueMethod}
         */
        public String getLabel() {
            return label;
        }

        @Override
        public String getName() {
            return "Neuron Group";
        }
    }

    // TODO
    public enum GroupEnum {
        DEFAULT, WTA, COMPETITIVE, SOM;

    }

}
