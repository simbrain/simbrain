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

import org.simbrain.network.core.*;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neuron_update_rules.UpdateRuleEnum;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.network.subnetworks.CompetitiveGroup;
import org.simbrain.network.subnetworks.SOMGroup;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.simbrain.network.LocatableModelKt.getTopLeftLocation;

/**
 * A group of neurons using a common {@link NeuronUpdateRule}. After creation the update rule may be changed but
 * neurons should not be added. Intermediate between a {@link NeuronCollection} which is just an
 * assemblage of potentially heterogeneous neurons that can be treated as a group, and a
 * {@link org.simbrain.network.matrix.NeuronArray} which is an array that can be updated using static update methods.
 * <p></p>
 * A primary abstraction for larger network structures. Layers in feed-forward networks are neuron
 * groups. Self-organizing-maps subclass this class. Etc. Since all update rules are the same groups can be characterized
 * as spiking vs. non-spiking.
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
    private UpdateRuleEnum groupUpdateRule = UpdateRuleEnum.LINEAR;

    /**
     * Default layout for neuron groups.
     */
    public static final Layout DEFAULT_LAYOUT = new GridLayout();

    /**
     * The layout for the neurons in this group.
     */
    @UserParameter(label = "Layout", isObjectType = true, tab = "Layout", order = 150)
    private Layout layout = DEFAULT_LAYOUT;

    /**
     * Set of incoming synapse groups.
     */
    private final HashSet<SynapseGroup> incomingSgs = new HashSet<SynapseGroup>();

    /**
     * Set of outgoing synapse groups.
     */
    private final HashSet<SynapseGroup> outgoingSgs = new HashSet<SynapseGroup>();

    /**
     * In method setLayoutBasedOnSize, this is used as the threshold number of neurons in the group, above which to use
     * grid layout instead of line layout.
     */
    private int gridThreshold = 9;

    /**
     * Space between neurons within a layer.
     */
    private int betweenNeuronInterval = 50;

    /**
     * Create a neuron group without any initial neurons.
     */
    public NeuronGroup(final Network net) {
        super(net);
    }

    /**
     * Construct a new neuron group from a list of neurons.
     *
     * @param net     the network
     * @param neurons the neurons
     */
    public NeuronGroup(final Network net, final List<Neuron> neurons) {
        this(net);
        neurons.forEach(n -> {
            super.addNeuron(n);
            n.setParentGroup(this);
        });
    }

    /**
     * Construct a new neuron group with a specified number of neurons.
     *
     * @param net        parent network
     * @param numNeurons how many neurons it will have
     */
    public NeuronGroup(final Network net, final int numNeurons) {
        this(net, Stream.generate(() -> new Neuron(net)).limit(numNeurons).collect(Collectors.toList()));
    }

    /**
     * Copy constructor. pass in network for cases where a group is pasted from one network to another
     *
     * @param net    parent network
     * @param toCopy the neuron group this will become a (deep) copy of.
     */
    public NeuronGroup(final Network net, final NeuronGroup toCopy) {
        this(net, toCopy.getNeuronList().stream().map(Neuron::deepCopy).collect(Collectors.toList()));
        setLabel(net.getIdManager().getProposedId(this.getClass()));
        this.setLayout(toCopy.getLayout());
        this.setGroupUpdateRule(toCopy.groupUpdateRule);
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

    /**
     * Delete this neuron group.
     */
    public void delete() {
        events.fireDeleted();
        neuronList.forEach(Neuron::delete);
        outgoingSgs.forEach(SynapseGroup::delete);
        incomingSgs.forEach(SynapseGroup::delete);
        activationRecorder.stopRecording();
    }

    /**
     * Updates all the neurons in the neuron group according to their NeuronUpdateRule(s). If the group is in input mode
     * reads in the next set of values from the input table and sets the neuron values accordingly.
     */
    @Override
    public void update() {
        super.update();
        // if (!inputMode) {
            NetworkKt.updateNeurons(getNeuronList());
            invalidateCachedActivations();
        // }
    }

    /**
     * Set the update rule for the neurons in this group.
     *
     * @param base the neuron update rule to set.
     */
    public void setNeuronType(NeuronUpdateRule base) {
        inputManager.setInputSpikes(base.isSpikingNeuron());
        groupUpdateRule = UpdateRuleEnum.get(base);
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
            NeuronUpdateRule newRule =
                    (NeuronUpdateRule) Class.forName("org.simbrain.network.neuron_update_rules." + rule).newInstance();
            inputManager.setInputSpikes(newRule.isSpikingNeuron());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        for (Neuron neuron : getNeuronList()) {
            neuron.setUpdateRule(rule);
        }
    }

    /**
     * Return a human-readable name for this type of neuron group. Subclasses should override this. Used in the Gui for
     * various purposes.
     *
     * @return the name of this type of neuron group.
     */
    public String getTypeDescription() {
        return "Neuron Group";
    }

    /**
     * Returns true if the provided synapse is in the fan-in weight vector of some node in this neuron group.
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

    /**
     * Returns an array of spike indices used in couplings, (e.g. to a raster plot). For example, if a neuron group has
     * 9 neurons, and neurons 1 and 4 just spiked, the producer will send the list (1,0,0,4,0,0,0,0,0).
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
     * @param x x coordinate for neuron group
     * @param y y coordinate for neuron group
     */
    public void setLocation(final double x, final double y) {
        super.setLocation(new Point2D.Double(x, y));
    }

    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }


    /**
     * Apply this group's layout to its neurons.
     */
    public void applyLayout() {
        layout.setInitialLocation(getTopLeftLocation(getNeuronList()));
        layout.layoutNeurons(getNeuronList());
    }

    /**
     * Apply this group's layout to its neurons based on a specified initial position.
     *
     * @param initialPosition the position from which to begin the layout.
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
     * If more than gridThreshold neurons use a grid layout, else a horizontal line layout.
     */
    public void setLayoutBasedOnSize() {
        setLayoutBasedOnSize(new Point2D.Double(0, 0));
    }

    /**
     * If more than gridThreshold neurons use a grid layout, else a horizontal line layout.
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

    // TODO
    public boolean isSpikingNeuronGroup() {
        return inputManager.isInputSpikes();
    }

    @Override
    public NeuronGroup copy() {
        return this.deepCopy(this.getParentNetwork());
    }

    /**
     * Helper class for creating new neuron groups using
     * {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     */
    public static class NeuronGroupCreator implements EditableObject {

        @UserParameter(label = "Number of neurons", description = "How many neurons this neuron group should have",
                order = -1)
        int numNeurons = 20;

        /**
         * A label for this Neuron Group for display purposes.
         */
        @UserParameter(label = "Label", initialValueMethod = "getLabel", order = 10)
        private String label;

        /**
         * Initial update rule
         */
        @UserParameter(label = "Update Rule", order = 20)
        private UpdateRuleEnum updateRule = UpdateRuleEnum.LINEAR;
        //todo conditional enable based on group type

        @UserParameter(label = "Group type", order = 30)
        private GroupEnum groupType = GroupEnum.DEFAULT;

        /**
         * The layout for the neurons in this group.
         */
        @UserParameter(label = "Layout", isObjectType = true, tab = "Layout", order = 50)
        private Layout layout = DEFAULT_LAYOUT;

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
         * @return the created neuron array
         */
        public NeuronGroup create(Network network) {
            NeuronGroup ng = null;
            if (groupType == GroupEnum.DEFAULT) {
                ng = new NeuronGroup(network, numNeurons);
                ng.setGroupUpdateRule(updateRule);
                ng.setLabel(label);
            } else if (groupType == GroupEnum.WTA) {
                ng = new WinnerTakeAll(network, numNeurons);
            } else if (groupType == GroupEnum.COMPETITIVE) {
                ng = new CompetitiveGroup(network, numNeurons);
            } else if (groupType == GroupEnum.SOM) {
                ng = new SOMGroup(network, numNeurons);
            }
            ng.setLayout(layout);
            ng.applyLayout();
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

    /**
     * Enum for creation dialog.
     */
    public enum GroupEnum {
        DEFAULT  {
            @Override
            public String toString() {
                return "Default";
            }
        },
        WTA {
            @Override
            public String toString() {
                return "Winner take all";
            }
        },
        COMPETITIVE {
            @Override
            public String toString() {
                return "Competitive";
            }
        },
        SOM {
            @Override
            public String toString() {
                return "Self organizing map";
            }
        };
    }

    @Override
    public void postUnmarshallingInit() {
        super.postUnmarshallingInit();
        // NeuronGroup neurons are not loose neurons so they are not deserialized
        // when Network.readResolve is called
        getNeuronList().forEach(Neuron::postUnmarshallingInit);
        getNeuronList().forEach(this::addListener);
    }
}
