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

import org.simbrain.network.LocatableModelKt;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.util.BiasedScalarData;
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Default layout for neuron groups.
     */
    public static final Layout DEFAULT_LAYOUT = new GridLayout();

    /**
     * The layout for the neurons in this group.
     */
    @UserParameter(label = "Layout", tab = "Layout", order = 150)
    private Layout layout = DEFAULT_LAYOUT;

    /**
     * In method setLayoutBasedOnSize, this is used as the threshold number of neurons in the group, above which to use
     * grid layout instead of line layout.
     */
    private int gridThreshold = 9;

    /**
     * Space between neurons within a layer.
     */
    private int betweenNeuronInterval = 50;

    @UserParameter(label = "Update Rule", order = 100)
    private NeuronUpdateRule prototypeRule = new LinearRule();

    /**
     * Data holder for prototype rule.
     */
    private ScalarDataHolder dataHolder;

    /**
     * Create a neuron group without any initial neurons.
     */
    public NeuronGroup(final Network net) {
        super(net);
        setPrototypeRule(prototypeRule);
    }

    /**
     * Construct a new neuron group from a list of neurons.
     *
     * @param net     the network
     * @param neurons the neurons
     */
    public NeuronGroup(final Network net, final List<Neuron> neurons) {
        this(net);
        neurons.forEach(super::addNeuron);
        prototypeRule = neurons.get(0).getUpdateRule();
        setNeuronType(prototypeRule);
        dataHolder = prototypeRule.createScalarData();
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
        setPrototypeRule(toCopy.prototypeRule);
        setLabel(net.getIdManager().getProposedId(this.getClass()));
        this.setLayout(toCopy.getLayout());
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
        super.delete();
        events.getDeleted().fireAndForget(this);
        neuronList.forEach(Neuron::delete);
    }

    /**
     * Updates all the neurons in the neuron group according to their NeuronUpdateRule(s). If the group is in input mode
     * reads in the next set of values from the input table and sets the neuron values accordingly.
     */
    @Override
    public void update() {
        neuronList.forEach(Neuron::updateInputs);
        neuronList.forEach(n -> prototypeRule.apply(n, dataHolder));
        neuronList.forEach(Neuron::clearInput);
        super.update();
    }

    // TODO: Replace with setPrototypeRule or setUpdateRule
    /**
     * Set the update rule for the neurons in this group.
     *
     * @param base the neuron update rule to set.
     */
    public void setNeuronType(NeuronUpdateRule base) {
        prototypeRule = base;
        dataHolder = prototypeRule.createScalarData();
        // Have to also set node rules to support randomization, increment, etc.
        // But they don't then use the settings of the prototype rule
        neuronList.forEach(n -> n.changeUpdateRule(base, dataHolder));
    }

    public void setPrototypeRule(NeuronUpdateRule rule) {
        setNeuronType(rule);
    }

    public NeuronUpdateRule getPrototypeRule() {
        return prototypeRule;
    }

    @Override
    public void clear() {
        super.clear();
        neuronList.forEach(Neuron::clear);
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
            if (neuron.getDataHolder() instanceof BiasedScalarData) {
                retArray[i++] = ((BiasedScalarData) neuron.getDataHolder()).getBias();
            }
        }
        return retArray;
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
        layout.setInitialLocation(getTopLeftLocation());
        layout.layoutNeurons(getNeuronList());
    }

    /**
     * Apply this group's layout to its neurons based on a specified top-left initial position.
     *
     * @param initialPosition the position from which to begin the layout.
     */
    public void applyLayout(Point2D initialPosition) {
        layout.setInitialLocation(initialPosition);
        layout.layoutNeurons(getNeuronList());
    }

    /**
     * Forwards to {@link #applyLayout(Point2D)}
     */
    public void applyLayout(int x, int y) {
        applyLayout(new Point2D.Double(x,y));
    }

    /**
     * Sets a new layout and applies it, using the groups' current location.
     */
    public void applyLayout(Layout newLayout) {
        layout = newLayout;
        applyLayout(getLocation());
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
        @UserParameter(label = "Label", order = 10)
        private String label;

        // Add this once it's possible enable based on group type
        // @UserParameter(label = "Update Rule", order = 20)
        // private NeuronUpdateRule prototype = new LinearRule();

        @UserParameter(label = "Group type", order = 30)
        private GroupEnum groupType = GroupEnum.DEFAULT;

        /**
         * The layout for the neurons in this group.
         */
        @UserParameter(label = "Layout", tab = "Layout", order = 50)
        private Layout layout = DEFAULT_LAYOUT;

        /**
         * Create the template with a proposed label
         */
        public NeuronGroupCreator(String proposedLabel) {
            this.label = proposedLabel;
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
        },
        SOFTMAX {
            @Override
            public String toString() {
                return "Softmax";
            }
        };
    }

    @Override
    public void postOpenInit() {
        super.postOpenInit();
        // NeuronGroup neurons are not free neurons so they are not deserialized
        // when Network.readResolve is called
        getNeuronList().forEach(Neuron::postOpenInit);
        getNeuronList().forEach(this::addListener);
    }

    public Point2D.Double getTopLeftLocation() {
        return LocatableModelKt.getTopLeftLocation(neuronList);
    }
}
