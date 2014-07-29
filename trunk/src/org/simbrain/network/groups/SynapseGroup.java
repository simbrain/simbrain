/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.randomizer.PolarizedRandomizer;

/**
 *
 * A group of synapses. Must connect a source and target neuron group.
 *
 * @author Zach Tosi
 *
 */
public class SynapseGroup extends Group {

    /** 
     * The <b>default>/b> polarized randomizer associated with excitatory
     * synapse strengths for all synapse groups.
     */
    private static final PolarizedRandomizer DEFAULT_EX_RANDOMIZER =
        new PolarizedRandomizer(
            Polarity.EXCITATORY);

    /** 
     * The <b>default>/b> polarized randomizer associated with inhibitory
     * synapse strengths for all synapse groups.
     */
    private static final PolarizedRandomizer DEFAULT_IN_RANDOMIZER =
        new PolarizedRandomizer(
            Polarity.INHIBITORY);

    /**
     * The default ratio (all excitatory) for all synapse groups.
     */
    public static final double DEFAULT_EXCITATORY_RATIO = 1.0;

    /** All to All. */
    public static final ConnectNeurons DEFAULT_CONNECTION_MANAGER =
        new AllToAll();

    /** A set containing all the excitatory (wt > 0) synapses in the group. */
    private Set<Synapse> exSynapseSet = new HashSet<Synapse>();

    /** A set containing all the inhibitory (wt < 0) synapses in the group. */
    private Set<Synapse> inSynapseSet = new HashSet<Synapse>();

    /** Reference to source neuron group. */
    private final NeuronGroup sourceNeuronGroup;

    /** Reference to target neuron group. */
    private final NeuronGroup targetNeuronGroup;

    /**
     * The connect neurons object associated with this group.
     */
    private ConnectNeurons connectionManager;

    /**
     * The percent of synapses that are excitatory. This parameter represents
     * the ideal value of {@link #exSynapseSet}.size() / {@link #size()}
     * (the <b>actual</b> excitatory ratio).
     * This value is a mutable parameter the changing of which will cause the
     * synapse group to attempt to make:
     * {@link #exSynapseSet}.size() / {@link #size()}
     * as close to the excitatoryRatio value as possible
     * (see {@link #setExcitatoryRatio(double)}). This means that this value
     * is an ideal that (usually) the actual excitatory ratio is near, but
     * seldom exactly equal to.
     * Because of the way polarity is chosen for new synapses added to the group
     * this value represents a central limit that the synapse group would
     * absolutely reach exactly given an infinite number of synapses.
     * If the source neurons of this group are themselves polarized the
     * actual excitatory ratio of the synapses in this group will reflect
     * the excitatory ratio of the source neurons <b>not<b> this variable. The
     * group will however attempt to get as close to this ratio as possible
     * without ever assigning a synapse to a source neuron of opposing polarity.
     */
    private double excitatoryRatio = DEFAULT_EXCITATORY_RATIO;

    /**
     * A template synapse which can be edited to inform the group as to what
     * parameters a new "blank" excitatory synapse should be given.
     */
    private Synapse excitatoryPrototype = Synapse.getTemplateSynapse();

    /**
     * A template synapse which can be edited to inform the group as to what
     * parameters a new "blank" inhibitory synapse should be given.
     */
    private Synapse inhibitoryPrototype = Synapse.getTemplateSynapse();

    /**
     * The randomizer governing excitatory synapses. If null new synapses are
     * not randomized.
     */
    private PolarizedRandomizer exciteRand;

    /**
     * The randomizer governing inhibitory synapses. If null new synapses are
     * not randomized.
     */
    private PolarizedRandomizer inhibRand;

    /**
     * Flag for whether synapses should be displayed in a GUI representation of
     * this object.
     */
    private boolean displaySynapses;

    /** Whether or not this synapse group is recurrent. */
    private boolean recurrent;

    /**
     * A boolean flag set based on if all the inhibitory synapses in this group
     * are static. Technically this flag can be set to true even if all the
     * inhibitory synapses are not static. It is used as an optimization
     * along with {@link #useGroupLevelSettings} to determine whether or not
     * all the inhibitory synapses should be iterated over during update. This
     * flag will have no effect on panels or update procedure if 
     * {@link #useGroupLevelSettings} is <b>false</b>, however if it is
     * <b>true</b> this flag will be dominant over the actual state of synapses
     * for the purpose of updating.
     */
    private boolean inStatic = true;

    /**
     * A boolean flag set based on if all the excitatory synapses in this group
     * are static. Technically this flag can be set to true even if all the
     * excitatory synapses are not static. It is used as an optimization
     * along with {@link #useGroupLevelSettings} to determine whether or not
     * all the excitatory synapses should be iterated over during update. This
     * flag will have no effect on panels or update procedure if 
     * {@link #useGroupLevelSettings} is <b>false</b>, however if it is
     * <b>true</b> this flag will be dominant over the actual state of synapses
     * for the purpose of updating.
     */
    private boolean exStatic = false;

    /**
     * Whether or not group level settings i.e. : those stored in
     * {@link #excitatoryPrototype} and {@link #inhibitoryPrototype} and the
     * flags {@link #inStatic} and {@link #exStatic} should be dominant over
     * the actual values stored in the individual synapses for the purposes of
     * updating and queries made to this group. For instance:
     * If this setting is <b>true</b>, and some other class calls
     * {@link #isExcitatoryFrozen()}, then instead of iterating over all the
     * synapses in the group to supply an answer, the method will return the
     * result of {@link #excitatoryPrototype}.isFrozen(). This is useful for
     * cases where synapses within synapse groups are entirely governed by 
     * group level attributes and it is known to the user that individual 
     * synapse settings will/should not be changed apart from the group.
     */
    private boolean useGroupLevelSettings = true;

    /**
     * Completely creates a synapse group between the two neuron groups with
     * all default parameters. This method creates the individual connections.
     * @param source
     * @param target
     * @return
     */
    public static SynapseGroup createSynapseGroup(final NeuronGroup source,
        final NeuronGroup target) {
        return createSynapseGroup(source, target, DEFAULT_CONNECTION_MANAGER);
    }

    /**
     * Completely creates a synapse group with the desired parameters. That is
     * the connections (individual synapses) are created along with the group.
     * @param source
     * @param target
     * @param connectionManager
     * @param excitatoryRatio
     * @return
     */
    public static SynapseGroup createSynapseGroup(final NeuronGroup source,
        final NeuronGroup target, final double excitatoryRatio) {
        return createSynapseGroup(source, target, DEFAULT_CONNECTION_MANAGER,
            excitatoryRatio, DEFAULT_EX_RANDOMIZER, DEFAULT_IN_RANDOMIZER);
    }

    /**
     * Completely creates a synapse group with the desired parameters. That is
     * the connections (individual synapses) are created along with the group.
     * @param source
     * @param target
     * @param connectionManager
     * @return
     */
    public static SynapseGroup createSynapseGroup(final NeuronGroup source,
        final NeuronGroup target, final ConnectNeurons connectionManager) {
        return createSynapseGroup(source, target, connectionManager,
            DEFAULT_EXCITATORY_RATIO);
    }

    /**
     * Completely creates a synapse group with the desired parameters. That is
     * the connections (individual synapses) are created along with the group.
     * @param source
     * @param target
     * @param connectionManager
     * @param excitatoryRatio
     * @return
     */
    public static SynapseGroup createSynapseGroup(final NeuronGroup source,
        final NeuronGroup target, final ConnectNeurons connectionManager,
        final double excitatoryRatio) {
        return createSynapseGroup(source, target, connectionManager,
            excitatoryRatio, DEFAULT_EX_RANDOMIZER, DEFAULT_IN_RANDOMIZER);
    }

    /**
     * Completely creates a synapse group with the desired parameters. That is
     * the connections (individual synapses) are created along with the group.
     * @param source
     * @param target
     * @param connectionManager
     * @param excitatoryRatio
     * @param exciteRand
     * @param inhibRand
     * @return
     */
    public static SynapseGroup createSynapseGroup(final NeuronGroup source,
        final NeuronGroup target, final ConnectNeurons connectionManager,
        double excitatoryRatio, final PolarizedRandomizer exciteRand,
        final PolarizedRandomizer inhibRand) {
        SynapseGroup synGroup = new SynapseGroup(source, target,
            connectionManager);
        synGroup.setExcitatoryRatio(excitatoryRatio);
        synGroup.setRandomizers(exciteRand, inhibRand);
        synGroup.makeConnections();
        return synGroup;
    }

    /**
     * Creates a blank synapse group between a source and target neuron group
     * using the default connection manager.
     * Until {@link #makeConnections()} is called this group will be empty and
     * will not be added to the source or target neuron groups' respective
     * outgoing and incoming synapse group sets.
     * @param source
     * @param target
     */
    public SynapseGroup(final NeuronGroup source, final NeuronGroup target) {
        super(source.getParentNetwork());
        this.sourceNeuronGroup = source;
        this.targetNeuronGroup = target;
        recurrent = testRecurrent();
        initializeSynapseVisibility();
    }

    /**
     * Creates a blank synapse group between a source and target neuron group.
     * Until {@link #makeConnections()} is called this group will be empty and
     * will not be added to the source or target neuron groups' respective
     * outgoing and incoming synapse group sets.
     *
     * @param source
     *            source neuron group
     * @param target
     *            target neuron group
     * @param connectionManager
     *            a connection object which builds this group
     */
    public SynapseGroup(final NeuronGroup source, final NeuronGroup target,
        final ConnectNeurons connectionManager) {
        super(source.getParentNetwork());
        this.sourceNeuronGroup = source;
        this.targetNeuronGroup = target;
        this.connectionManager = connectionManager;
        recurrent = testRecurrent();
        initializeSynapseVisibility();
    }

    /**
     * The "build" method which actually constructs the group in terms of
     * populating it with synapses. If called on a synapse group which has
     * already been made (i.e. already has synapses populating it) those
     * synapses will be destroyed and the current connection manager and
     * parameters used to create new connections. This method adds the
     * current synapse group the the source neuron group's outgoing synapse
     * set and the target neuron group's incoming synapse set.
     */
    public void makeConnections() {
        clear();
        sourceNeuronGroup.addOutgoingSg(this);
        targetNeuronGroup.addIncomingSg(this);
        connectionManager.connectNeurons(this);
    }

    /**
     * Pre-allocates, that is sets the initial capacity of the arraylist
     * containing this synapse group's synapses. This allows expectedNumber of
     * synapses to be added to this synapse group without the synapse list
     * having to perform any operations related to expanding the list size.
     *
     * @param expectedNumSynapses
     * @throws IllegalStateException
     */
    public void preAllocateSynapses(int expectedNumSynapses)
        throws IllegalStateException {
        if (!exSynapseSet.isEmpty() || !inSynapseSet.isEmpty()) {
            throw new IllegalArgumentException("Cannot pre-allocate space"
                + " for some expected number of synapses when the synapse"
                + " when one or both synapse sets are already populated."
                + " Pre-allocations can only occur before connections"
                + " have been initialized.");
        }
        // Using /0.8 instead of /0.75 because expected number is _expected_
        // but not precisely known.
        exSynapseSet = new HashSet<Synapse>((int) (expectedNumSynapses
            * excitatoryRatio / 0.8));
        inSynapseSet = new HashSet<Synapse>((int) (expectedNumSynapses
            * (1 - excitatoryRatio) / 0.8));
    }

    /**
     * If an algorithm (like training) extensively changes the polarity of the
     * synapses in this group, it's impractical to check every time a change is
     * made. Thus after the bulk of the algorithm is completed this method can
     * be called to sort synapses into their appropriate sets.
     */
    public void revalidateSynapseSets() {
        Iterator<Synapse> exIterator = exSynapseSet.iterator();
        ArrayList<Synapse> exSwitches = new ArrayList<Synapse>(
            exSynapseSet.size());
        while (exIterator.hasNext()) {
            Synapse s = exIterator.next();
            if (s.getStrength() < 0) {
                exSwitches.add(s);
                exIterator.remove();
            }
        }
        Iterator<Synapse> inIterator = inSynapseSet.iterator();
        ArrayList<Synapse> inSwitches = new ArrayList<Synapse>(
            inSynapseSet.size());
        while (inIterator.hasNext()) {
            Synapse s = exIterator.next();
            if (s.getStrength() > 0) {
                inSwitches.add(s);
                inIterator.remove();
            }
        }
        exSynapseSet.addAll(inSwitches);
        inSynapseSet.addAll(exSwitches);
        excitatoryRatio = exSynapseSet.size() / (double) (size());

    }

    /**
     * Update group. Override for special updating.
     */
    public void update() {
        updateAllSynapses();
    }

    /**
     * Update all synapses.
     */
    public void updateAllSynapses() {
        // Will skip updating all synapses if we are using group level settings
        // and we know that either all the synapses are static or all the
        // synapses are frozen.
        if (!((exStatic || isExcitatoryFrozen()) && useGroupLevelSettings)) {
            for (Synapse synapse : exSynapseSet) {
                synapse.update();
            }
        }
        if (!((inStatic || isInhibitoryFrozen()) && useGroupLevelSettings)) {
            for (Synapse synapse : inSynapseSet) {
                synapse.update();
            }
        }
    }

    /** {@inheritDoc} */
    public int size() {
        return exSynapseSet.size() + inSynapseSet.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return exSynapseSet.isEmpty() && inSynapseSet.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete() {
        if (isMarkedForDeletion()) {
            return;
        } else {
            setMarkedForDeletion(true);
        }
        clear();
        getParentNetwork().removeGroup(this);
        if (hasParentGroup()) {
            if (getParentGroup() instanceof Subnetwork) {
                ((Subnetwork) getParentGroup()).removeSynapseGroup(this);
            }
            if (getParentGroup().isEmpty()) {
                getParentNetwork().removeGroup(getParentGroup());
            }
        }
        if (!targetNeuronGroup.isMarkedForDeletion()) {
            targetNeuronGroup.removeIncomingSg(this);
        }
        if (!sourceNeuronGroup.isMarkedForDeletion()) {
            sourceNeuronGroup.removeOutgoingSg(this);
        }
        Runtime.getRuntime().gc();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String ret = new String();
        ret += ("Synapse Group [" + getLabel() + "]. Contains " + this.size()
            + " synapse(s)." + " Connects "
            + getSourceNeuronGroup().getId() + " ("
            + getSourceNeuronGroup().getLabel() + ")" + " to "
            + getTargetNeuronGroup().getId() + " ("
            + getTargetNeuronGroup().getLabel() + ")\n");

        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUpdateMethodDesecription() {
        return "Update synapses";
    }

    /**
     * Determine whether this synpasegroup should initially have its synapses
     * displayed. For isolated synapse groups check its number of synapses. If
     * the maximum number of possible connections exceeds a the network's
     * synapse visibility threshold, then individual synapses will not be
     * displayed.
     */
    public void initializeSynapseVisibility() {
        int threshold = Network.getSynapseVisibilityThreshold();
        if (sourceNeuronGroup.size() * targetNeuronGroup.size() > threshold) {
            displaySynapses = false;
        } else {
            displaySynapses = true;
        }
    }

    /**
     * @param displaySynapses
     *            the displaySynapses to set
     */
    public void setDisplaySynapses(boolean displaySynapses) {
        this.displaySynapses = displaySynapses;
    }

    /**
     * @return the displaySynapses
     */
    public boolean isDisplaySynapses() {
        return displaySynapses;
    }

    /**
     * Remove the provided synapse from the group, but not the network.
     *
     * @param toDelete
     *            the synapse to delete
     */
    public Synapse removeSynapse(Synapse toDelete) {
        if (toDelete.getStrength() > 0) {
            exSynapseSet.remove(toDelete);
        } else {
            inSynapseSet.remove(toDelete);
        }
        if (isDisplaySynapses()) {
            toDelete.getNetwork().fireSynapseRemoved(toDelete);
        }
        getParentNetwork().fireGroupChanged(this, this, "synapseRemoved");
        if (isEmpty()) {
            delete();
        }
        return toDelete;
    }

    /**
     * Removes all synapses from this synapse group and the network. Deletes all
     * synapses in this group.
     */
    public void clear() {
        for (Synapse toDelete : exSynapseSet) {
            // Remove references to this synapse from parent neurons
            toDelete.getSource().removeEfferent(toDelete);
            toDelete.getTarget().removeAfferent(toDelete);
            if (isDisplaySynapses()) {
                toDelete.getNetwork().fireSynapseRemoved(toDelete);
            }

        }
        for (Synapse toDelete : inSynapseSet) {
            toDelete.getSource().removeEfferent(toDelete);
            toDelete.getTarget().removeAfferent(toDelete);
            if (isDisplaySynapses()) {
                toDelete.getNetwork().fireSynapseRemoved(toDelete);
            }
        }
        exSynapseSet.clear();
        inSynapseSet.clear();
    }

    /**
     * Adds a new synapse (one which is "blank") to the synapse group. This is
     * the <b>preferred</b> method to use for adding synapses to the synapse
     * group over {@link #addSynapseUnsafe(Synapse)} because it makes the added
     * synapse conform to the global parameters of this synapse group.
     * @param synapse
     */
    public void addNewSynapse(final Synapse synapse) {
        if (synapse.getSource().isPolarized()) {
            if (Polarity.EXCITATORY.equals(synapse.getSource().getPolarity())) {
                addNewExcitatorySynapse(synapse);
            } else if (Polarity.INHIBITORY.equals(synapse.getSource()
                .getPolarity())) {
                addNewInhibitorySynapse(synapse);
            }
        } else {
            double rand = Math.random();
            double correctionTerm = size() == 0 ? 0
                : excitatoryRatio - (exSynapseSet.size() / (double) size());
            if (rand < (excitatoryRatio + correctionTerm)) {
                addNewExcitatorySynapse(synapse);
            } else {
                addNewInhibitorySynapse(synapse);
            }
        }
    }

    /**
     *
     * @param synapse
     */
    public void addNewExcitatorySynapse(final Synapse synapse) {
        synapse.setId(getParentNetwork().getSynapseIdGenerator().getId());
        synapse.setParentGroup(this);
        if (exciteRand != null) {
            synapse.setStrength(exciteRand.getRandom());
        } else {
            synapse.setStrength(ConnectionUtilities
                .DEFAULT_EXCITATORY_STRENGTH);
        }
        synapse.setLearningRule(excitatoryPrototype.getLearningRule()
            .deepCopy());
        synapse.setFrozen(excitatoryPrototype.isFrozen());
        synapse.setEnabled(excitatoryPrototype.isEnabled());
        synapse.setDelay(excitatoryPrototype.getDelay());
        synapse.setIncrement(excitatoryPrototype.getIncrement());
        synapse.setUpperBound(excitatoryPrototype.getUpperBound());
        synapse.setLowerBound(excitatoryPrototype.getLowerBound());
        synapse.setSpikeResponder(excitatoryPrototype
            .getSpikeResponder());
        exSynapseSet.add(synapse);
    }

    /**
     *
     * @param synapse
     */
    public void addNewInhibitorySynapse(final Synapse synapse) {
        synapse.setId(getParentNetwork().getSynapseIdGenerator().getId());
        synapse.setParentGroup(this);
        if (inhibRand != null) {
            synapse.setStrength(inhibRand.getRandom());
        } else {
            synapse.setStrength(ConnectionUtilities
                .DEFAULT_INHIBITORY_STRENGTH);
        }
        synapse.setLearningRule(inhibitoryPrototype.getLearningRule()
            .deepCopy());
        synapse.setFrozen(inhibitoryPrototype.isFrozen());
        synapse.setEnabled(inhibitoryPrototype.isEnabled());
        synapse.setDelay(inhibitoryPrototype.getDelay());
        synapse.setIncrement(inhibitoryPrototype.getIncrement());
        synapse.setUpperBound(inhibitoryPrototype.getUpperBound());
        synapse.setLowerBound(inhibitoryPrototype.getLowerBound());
        synapse.setSpikeResponder(inhibitoryPrototype
            .getSpikeResponder());
        inSynapseSet.add(synapse);
    }

    /**
     * Add a synapse to this synapse group adding it to the appropriate
     * synapse set (excitatory or inhibitory if it's weight is above or below
     * zero respecively). Using this method is <b>NOT RECOMMENDED</b> under most
     * circumstances. This is because no checks are performed on the synapse to
     * ensure that it can sensibly be added to this group. The synapse can also
     * have parameters which will make the global parameters of the synapse
     * group no longer accurately apply to the whole group. This is not as
     * problematic as say not connecting neurons which are in this synapse
     * group's source and/or target neuron groups, but it does undermine the
     * purpose of having synapse groups.
     *
     * Possible Use Case: When it is known beforehand that the synapse(s) being
     * added all conform to the parameters of this synapse group.
     *
     * @param synapse
     *            synapse to add
     */
    public void addSynapseUnsafe(final Synapse synapse) {
        if (synapse.getStrength() > 0) {
            addExcitatorySynapseUnsafe(synapse);
        }
        if (synapse.getStrength() < 0) {
            addInhibitorySynapseUnsafe(synapse);
        }
    }

    /**
     * See: {@link #addSynapseUnsafe(Synapse)}. Same but specific to
     * excitatory synapses. This is even less safe however because an inhibitory
     * synapse could potentially be added to the excitatory set.
     * @param synapse the synapse to add.
     */
    public void addExcitatorySynapseUnsafe(final Synapse synapse) {
        exSynapseSet.add(synapse);
        excitatoryRatio = exSynapseSet.size() / (double) size();
        if (getParentNetwork() != null) {
            synapse.setId(getParentNetwork().getSynapseIdGenerator().getId());
            synapse.setParentGroup(this);
        }
    }

    /**
     * See: {@link #addSynapseUnsafe(Synapse)}. Same but specific to
     * inhibitory synapses. This is even less safe however because an excitatory
     * synapse could potentially be added to the inhibitory set.
     * @param synapse the synapse to add.
     */
    public void addInhibitorySynapseUnsafe(final Synapse synapse) {
        inSynapseSet.add(synapse);
        excitatoryRatio = exSynapseSet.size() / (double) size();
        if (getParentNetwork() != null) {
            synapse.setId(getParentNetwork().getSynapseIdGenerator().getId());
            synapse.setParentGroup(this);
        }
    }

    /**
     * @return a flat list representation of all the synapses in this synapse
     *         group. This list is a defensive copy.
     */
    public List<Synapse> getAllSynapses() {
        ArrayList<Synapse> flatList = new ArrayList<Synapse>(size());
        flatList.addAll(getExcitatorySynapses());
        flatList.addAll(getInhibitorySynapses());
        return flatList;
    }

    /**
     * @return the set of excitatory synapses
     */
    public Set<Synapse> getExcitatorySynapses() {
        return Collections.unmodifiableSet(exSynapseSet);
    }

    /**
     * @return the set of inhibitory synapses
     */
    public Set<Synapse> getInhibitorySynapses() {
        return Collections.unmodifiableSet(inSynapseSet);
    }

    /**
     * Return weight strengths as a double vector.
     *
     * @return weights
     */
    public double[] getWeightVector() {
        double[] retArray = new double[size()];
        int i = 0;
        for (Synapse synapse : exSynapseSet) {
            retArray[i++] = synapse.getStrength();
        }
        for (Synapse synapse : inSynapseSet) {
            retArray[i++] = synapse.getStrength();
        }
        return retArray;
    }

    /**
     * @return the strengths of all the inhibitory synapses as a double array
     */
    public double[] getInhibitoryStrengths() {
        double[] retArray = new double[inSynapseSet.size()];
        int i = 0;
        for (Synapse synapse : inSynapseSet) {
            retArray[i++] = synapse.getStrength();
        }
        return retArray;
    }

    /**
     * @return the strengths of all the excitatory synapses as a double array
     */
    public double[] getExcitatoryStrengths() {
        double[] retArray = new double[exSynapseSet.size()];
        int i = 0;
        for (Synapse synapse : exSynapseSet) {
            retArray[i++] = synapse.getStrength();
        }
        return retArray;
    }

    /**
     * Sets the strength of a single synapse in the group specified as a
     * paremeter. If the synapse does not exist in this group returns false.
     * If the this makes the synapse change polarity it will be removed from
     * its current set and added to the appropriate set.
     * @param synapse
     * @param newWeight
     * @return
     */
    public boolean setSynapseStrength(Synapse synapse, double newWeight) {
        if (synapse.getStrength() >= 0 && exSynapseSet.contains(synapse)) {
            synapse.setStrength(newWeight);
            if (newWeight < 0) {
                exSynapseSet.remove(synapse);
                inSynapseSet.add(synapse);
            }
            return true;
        }
        if (synapse.getStrength() <= 0 && inSynapseSet.contains(synapse)) {
            synapse.setStrength(newWeight);
            if (newWeight > 0) {
                inSynapseSet.remove(synapse);
                exSynapseSet.add(synapse);
            }
            return true;
        }
        // The Synapse group does not contain the given synapse or the
        // synapse group is in a bad state.
        return false;
    }

    /**
     * Randomizes all the synapses according to their corresponding randomizers.
     * {@link #randomizeExcitatoryConnections()},
     *  {@link #randomizeInhibitoryConnections()}
     */
    public void randomizeConnectionWeights() {
        randomizeExcitatoryConnections();
        randomizeInhibitoryConnections();
    }

    /**
     * Randomizes the weights of the excitatory connections in this group
     * based on the parameters of {@link #exciteRand}. Assumes that all
     * synapses in {@link #exSynapseSet} are--in fact--excitatory. If some
     * action on the synapses may have corrupted that assumption call
     * {@link #revalidateSynapseSets()} first.
     */
    public void randomizeExcitatoryConnections() {
        ConnectionUtilities.randomizeExcitatorySynapsesUnsafe(exSynapseSet,
            exciteRand);
    }

    /**
     * Randomizes the weights of the inhibitory connections in this group
     * based on the parameters of {@link #inhibRand}. Assumes that all
     * synapses in {@link #inSynapseSet} are--in fact--inhibitory. If some
     * action on the synapses may have corrupted that assumption call
     * {@link #revalidateSynapseSets()} first.
     */
    public void randomizeInhibitoryConnections() {
        ConnectionUtilities.randomizeInhibitorySynapsesUnsafe(inSynapseSet,
            inhibRand);
    }

    /**
     * Changes the ratio of synapses in this group that are excitatory subject
     * to two constraints: <b>1)</b> If neurons in the source neuron group have
     * their own polarity ratio, the desired excitatoryRatio may not be
     * possible. In this case, this class will <b>NOT</b> add or remove synapses
     * to achieve this number. If not all source neurons are polarized, however
     * this class will attempt to get as close as possible to the desired
     * excitatoryRatio. <b>2)</b> Changes in weights performed to achieve the
     * desired excitatoryRatio will result from sign changes to synapses. The
     * absolute value of synapse strengths will not be changed.
     *
     * @param excitatoryRatio
     *            the ratio of synapses which will be made excitatory, value
     *            must be in the range [0, 1]
     */
    public void setExcitatoryRatio(double excitatoryRatio)
        throws IllegalArgumentException {
        if (excitatoryRatio > 1 || excitatoryRatio < 0) {
            throw new IllegalArgumentException("The parameter"
                + " 'excitatoryRatio' passed to setExcitatoryRatio"
                + " must be on [0, 1]");
        }

        // Return if there is no change or the group is empty.
        if (excitatoryRatio == this.excitatoryRatio)
            return;
        if (isEmpty()) {
            this.excitatoryRatio = excitatoryRatio;
            return;
        }

        if (excitatoryRatio < this.excitatoryRatio) {
            int numSwitch =
                (int) ((this.excitatoryRatio * size()) - (excitatoryRatio
                * size()));
            Iterator<Synapse> setIterator = exSynapseSet.iterator();
            while (setIterator.hasNext()) {
                Synapse s = setIterator.next();
                if (!s.getSource().isPolarized() && numSwitch > 0) {
                    setIterator.remove();
                    if (inhibRand != null) {
                        s.setStrength(inhibRand.getRandom());
                    } else {
                        s.setStrength(ConnectionUtilities
                            .DEFAULT_INHIBITORY_STRENGTH);
                    }
                    s.setLearningRule(inhibitoryPrototype.getLearningRule()
                        .deepCopy());
                    s.setFrozen(inhibitoryPrototype.isFrozen());
                    s.setEnabled(inhibitoryPrototype.isEnabled());
                    s.setDelay(inhibitoryPrototype.getDelay());
                    s.setIncrement(inhibitoryPrototype.getIncrement());
                    s.setUpperBound(inhibitoryPrototype.getUpperBound());
                    s.setLowerBound(inhibitoryPrototype.getLowerBound());
                    s.setSpikeResponder(inhibitoryPrototype
                        .getSpikeResponder());
                    inSynapseSet.add(s);
                    numSwitch--;
                }
            }
        } else {
            int numSwitch =
                (int) ((excitatoryRatio * size()) - (this.excitatoryRatio
                * size()));
            Iterator<Synapse> setIterator = inSynapseSet.iterator();
            while (setIterator.hasNext()) {
                Synapse s = setIterator.next();
                if (!s.getSource().isPolarized() && numSwitch > 0) {
                    setIterator.remove();
                    if (exciteRand != null) {
                        s.setStrength(exciteRand.getRandom());
                    } else {
                        s.setStrength(ConnectionUtilities
                            .DEFAULT_EXCITATORY_STRENGTH);
                    }
                    s.setLearningRule(excitatoryPrototype.getLearningRule()
                        .deepCopy());
                    s.setFrozen(excitatoryPrototype.isFrozen());
                    s.setEnabled(excitatoryPrototype.isEnabled());
                    s.setDelay(excitatoryPrototype.getDelay());
                    s.setIncrement(excitatoryPrototype.getIncrement());
                    s.setUpperBound(excitatoryPrototype.getUpperBound());
                    s.setLowerBound(excitatoryPrototype.getLowerBound());
                    s.setSpikeResponder(excitatoryPrototype
                        .getSpikeResponder());
                    exSynapseSet.add(s);
                    numSwitch--;
                }
            }
        }
        this.excitatoryRatio = excitatoryRatio;
    }

    /**
     * Returns the excitatory ratio <b>parameter</b>. For the <i>actual</i>
     * value use {@link #getExcitatoryRatioPrecise()}.
     *
     *
     * @return the ration of excitatory synapses in this group
     */
    public double getExcitatoryRatioParameter() {
        return excitatoryRatio;
    }

    /**
     * @return the <b>actual</b> excitatory ratio measured as the number
     * of excitatory synapses divided by the total.
     */
    public double getExcitatoryRatioPrecise() {
        return exSynapseSet.size() / (double) size();
    }

    /**
     * Sets the connection manager for this synapse group once and only once.
     * Subsequent attempts to modify the connection manager will fail, as
     * changing the connection manager amounts to (and should be implemented as)
     * creating an entirely new synapse group.
     *
     * @param connection
     */
    public void setConnectionManager(ConnectNeurons connection) {
        if (this.connectionManager == null) {
            this.connectionManager = connection;
        } else {
            throw new UnsupportedOperationException("The connection object" +
                " of a synapse group can be set only once. If a new" +
                " connection scheme is desired a new synapse group must" +
                " be constructed.");
        }
    }

    /**
     * @return the connection manager for this synapse group.
     */
    public ConnectNeurons getConnectionManager() {
        return connectionManager;
    }

    /**
     *
     * @param excitatoryRandomizer
     */
    public void
        setExcitatoryRandomizer(PolarizedRandomizer excitatoryRandomizer) {
        this.exciteRand = excitatoryRandomizer;
    }

    /**
     *
     * @param inhibitoryRandomizer
     */
    public void
        setInhibitoryRandomizer(PolarizedRandomizer inhibitoryRandomizer) {
        this.inhibRand = inhibitoryRandomizer;
    }

    /**
     *
     * @param excitatoryRandomizer
     * @param inhibitoryRandomizer
     */
    public void setRandomizers(PolarizedRandomizer excitatoryRandomizer,
        PolarizedRandomizer inhibitoryRandomizer) {
        setExcitatoryRandomizer(excitatoryRandomizer);
        setInhibitoryRandomizer(inhibitoryRandomizer);
    }

    /**
     *
     * @return
     */
    public PolarizedRandomizer getExcitatoryRandomizer() {
        return exciteRand;
    }

    /**
     *
     * @return
     */
    public PolarizedRandomizer getInhibitoryRandomizer() {
        return inhibRand;
    }

    /**
     * If a randomize operation changes the ratio of excitatory to inhibitory
     * synapses, this method can be called to change the excitatoryRatio to
     * reflect this value. This happens rarely, and generally speaking is not
     * recommended outside prototyping.
     *
     * @return the ratio of synapses in this group that are excitatory.
     */
    public double calculateExcitatoryRatio() {
        excitatoryRatio = exSynapseSet.size() / (double) size();
        if (Double.isNaN(excitatoryRatio)) {
            return 0;
        }
        return excitatoryRatio;
    }

    /**
     * Check whether this synapse group connects a neuron group to itself.
     *
     * @return true if this connects a neuron group to itself, false otherwise.
     */
    public boolean isRecurrent() {
        return recurrent;
    }

    /**
     * Tests if this synapse group is in fact recurrent (it's target and source
     * neuron groups are the same).
     *
     * @return
     */
    private boolean testRecurrent() {
        return sourceNeuronGroup == targetNeuronGroup;
    }

    /**
     * Return a list of source neurons associated with the synapses in this
     * group.
     *
     * @return the source neuron list.
     */
    public List<Neuron> getSourceNeurons() {
        return getSourceNeuronGroup().getNeuronList();
    }

    /**
     * Return a list of target neurons associated with the synapses in this
     * group.
     *
     * @return the target neuron list.
     */
    public List<Neuron> getTargetNeurons() {
        return getTargetNeuronGroup().getNeuronList();
    }

    public boolean hasExcitatory() {
        return !exSynapseSet.isEmpty();
    }

    public boolean hasInhibitory() {
        return !inSynapseSet.isEmpty();
    }

    /**
     * @return the sourceNeuronGroup
     */
    public NeuronGroup getSourceNeuronGroup() {
        return sourceNeuronGroup;
    }

    /**
     * @return the targetNeuronGroup
     */
    public NeuronGroup getTargetNeuronGroup() {
        return targetNeuronGroup;
    }

    /**
     * Set all weight strengths to a specified value.
     *
     * @param value
     *            the value to set the synapses to
     */
    public void setStrengths(final double value) {
        if (value > 0) {
            exSynapseSet.addAll(inSynapseSet);
            inSynapseSet.clear();
            excitatoryRatio = 1;
            for (Synapse s : exSynapseSet) {
                s.setStrength(value);
            }
        } else {
            inSynapseSet.addAll(exSynapseSet);
            exSynapseSet.clear();
            excitatoryRatio = 0;
            for (Synapse s : inSynapseSet) {
                s.setStrength(value);
            }
        }
    }

    /**
     * Sets all excitatory synapses to a certain value. If value is negative,
     * uses abs(value) instead.
     *
     * @param value
     */
    public void setAllExcitatoryStrengths(double value) {
        value = Math.abs(value);
        for (Synapse s : exSynapseSet) {
            s.setStrength(value);
        }
    }

    /**
     * Sets all inhibitory synapses to a certain value. If value is positive
     * uses -abs(value) instead.
     *
     * @param value
     */
    public void setAllInhibitoryStrengths(double value) {
        value = -Math.abs(value);
        for (Synapse s : inSynapseSet) {
            s.setStrength(value);
        }
    }

    /**
     * Set delay on all synapses.
     *
     * @param delay
     *            the delay to set
     */
    public void setDelay(final int delay) {
        for (Synapse s : exSynapseSet) {
            s.setDelay(delay);
        }
        for (Synapse s : inSynapseSet) {
            s.setDelay(delay);
        }
    }

    /**
     * Enable or disable all synapses in this group.
     *
     * @param enabled
     *            true to enable them all; false to disable them all
     */
    public void setEnabled(final boolean enabled) {
        setExcitatoryEnabled(enabled);
        setInhibitoryEnabled(enabled);
    }

    /**
     *
     * @param enabled
     */
    public void setExcitatoryEnabled(final boolean enabled) {
        excitatoryPrototype.setEnabled(enabled);
        for (Synapse s : exSynapseSet) {
            s.setEnabled(enabled);
        }
    }

    /**
     *
     * @param enabled
     */
    public void setInhibitoryEnabled(final boolean enabled) {
        inhibitoryPrototype.setEnabled(enabled);
        for (Synapse s : inSynapseSet) {
            s.setEnabled(enabled);
        }
    }

    /**
     * Set the spike responders for all synapses.
     *
     * @param responder
     *            the spike responder to set.
     */
    public void setSpikeResponders(final SpikeResponder responder) {
        setExcitatorySpikeResponders(responder);
        setInhibitorySpikeResponders(responder);
    }

    /**
     * Set the spike responders for excitatory synapses.
     *
     * @param responder
     *            the spike responder to set.
     */
    public void setExcitatorySpikeResponders(final SpikeResponder responder) {
        excitatoryPrototype.setSpikeResponder(responder.deepCopy());
        for (Synapse s : exSynapseSet) {
            s.setSpikeResponder(responder.deepCopy());
        }
    }

    /**
     * Set the spike responders for inhibitory synapses.
     *
     * @param responder
     *            the spike responder to set.
     */
    public void setInhibitorySpikeResponders(final SpikeResponder responder) {
        inhibitoryPrototype.setSpikeResponder(responder.deepCopy());
        for (Synapse s : inSynapseSet) {
            s.setSpikeResponder(responder.deepCopy());
        }
    }

    /**
     * Freeze or unfreeze all synapses in this group.
     *
     * @param freeze
     *            true to freeze the group; false to unfreeze it
     */
    public void setFrozen(final boolean freeze) {
        setExcitatoryFrozen(freeze);
        setInhibitoryFrozen(freeze);
    }

    /**
     * Freeze or unfreeze all the excitatory synapses in this group
     *
     * @param frozen
     */
    public void setExcitatoryFrozen(final boolean frozen) {
        excitatoryPrototype.setFrozen(frozen);
        for (Synapse s : exSynapseSet) {
            s.setFrozen(frozen);
        }
    }

    /**
     * Freeze or unfreeze all the inhibitorySynapses in this group.
     *
     * @param frozen
     */
    public void setInhibitoryFrozen(final boolean frozen) {
        inhibitoryPrototype.setFrozen(frozen);
        for (Synapse s : inSynapseSet) {
            s.setFrozen(frozen);
        }
    }

    /**
     * Returns true if all the synapses in this group are frozen.
     *
     * @return true if all synapses are frozen, false otherwise
     */
    public boolean isFrozen() {
        return isExcitatoryFrozen() && isInhibitoryFrozen();
    }

    /**
     * If {@link #useGroupLevelSettings} then this returns whether or not 
     * {@link #excitatoryPrototype} is frozen, which ideally is also whether or
     * not all the synapses in this group are frozen. If not, then all the
     * synapses are checked and this method returns false if <b>any</b> of the
     * excitatory synapses are not frozen.
     * @return if the excitatory synapses are frozen
     */
    public boolean isExcitatoryFrozen() {
        if (useGroupLevelSettings) {
            return excitatoryPrototype.isFrozen();
        }
        Iterator<Synapse> exIter = exSynapseSet.iterator();
        if (exIter.hasNext()) {
            Synapse s = exIter.next();
            if (!s.isFrozen()) {
                return false; // Not _all_ the excitatory synapses are frozen
            } else {
                while (exIter.hasNext()) {
                    if (!exIter.next().isFrozen()) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * If {@link #useGroupLevelSettings} then this returns whether or not 
     * {@link #excitatoryPrototype} is frozen, which ideally is also whether or
     * not all the synapses in this group are frozen. If not, then all the
     * synapses are checked and this method returns false if <b>any</b> of the
     * excitatory synapses are not frozen.
     * @return if the inhibitory synapses are frozen
     */
    public boolean isInhibitoryFrozen() {
        if (useGroupLevelSettings) {
            return inhibitoryPrototype.isFrozen();
        }
        Iterator<Synapse> inIter = inSynapseSet.iterator();
        if (inIter.hasNext()) {
            Synapse s = inIter.next();
            if (!s.isFrozen()) {
                return false; // Not _all_ the inhibitory synapses are frozen
            } else {
                while (inIter.hasNext()) {
                    if (!inIter.next().isFrozen()) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Returns true if all the synapses in this group are enabled.
     *
     * @return true if all synapses are enabled, false otherwise
     */
    public boolean isEnabled() {
        return isExcitatoryEnabled() && isInhibitoryEnabled();
    }

    /**
     * @return true if all excitatory synapses are enabled, false otherwise
     */
    public boolean isExcitatoryEnabled() {
        if (useGroupLevelSettings) {
            return excitatoryPrototype.isEnabled();
        }
        Iterator<Synapse> exIter = exSynapseSet.iterator();
        if (exIter.hasNext()) {
            Synapse s = exIter.next();
            if (!s.isEnabled()) {
                return false; // Not _all_ the excitatory synapses are enabled
            } else {
                while (exIter.hasNext()) {
                    if (!exIter.next().isEnabled()) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * @return true if all inhibitory synapses are enabled, false otherwise
     */
    public boolean isInhibitoryEnabled() {
        if (useGroupLevelSettings) {
            return inhibitoryPrototype.isEnabled();
        }
        Iterator<Synapse> inIter = inSynapseSet.iterator();
        if (inIter.hasNext()) {
            Synapse s = inIter.next();
            if (!s.isEnabled()) {
                return false; // Not _all_ the inhibitory synapses are enabled
            } else {
                while (inIter.hasNext()) {
                    if (!inIter.next().isEnabled()) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Reveals the excitatory prototype synapse, allowing more detailed synapse
     * values to be queried at the group level. To use a prototype to change
     * many values call {@link #setAndConformToTemplateExcitatory(Synapse)}
     * @return
     */
    public Synapse getExcitatoryPrototype() {
        return Synapse.copyTemplateSynapse(excitatoryPrototype);
    }

    /**
     * Reveals the inhibitory prototype synapse, allowing more detailed synapse
     * values to be queried at the group level. To use a prototype to change
     * many values call {@link #setAndConformToTemplateInhibitory(Synapse)}
     *
     * @return
     */
    public Synapse getInhibitoryPrototype() {
        return Synapse.copyTemplateSynapse(inhibitoryPrototype);
    }

    /**
     *
     * @param excitatoryPrototype
     */
    public void setAndConformToTemplateExcitatory(Synapse excitatoryPrototype) {
        // Ignore strength... must be set separately

        // Upper bound
        double upperBound = excitatoryPrototype.getUpperBound();
        if (this.excitatoryPrototype.getUpperBound() != upperBound
            || !useGroupLevelSettings) {
            for (Synapse s : exSynapseSet) {
                s.setUpperBound(upperBound);
            }
            this.excitatoryPrototype.setUpperBound(upperBound);
        }

        // Lower Bound
        double lowerBound = excitatoryPrototype.getLowerBound();
        if (this.excitatoryPrototype.getLowerBound() != lowerBound
            || !useGroupLevelSettings) {
            for (Synapse s : exSynapseSet) {
                s.setLowerBound(upperBound);
            }
            this.excitatoryPrototype.setLowerBound(upperBound);
        }

        // Increment
        double increment = excitatoryPrototype.getIncrement();
        if (this.excitatoryPrototype.getIncrement() != increment
            || !useGroupLevelSettings) {
            for (Synapse s : exSynapseSet) {
                s.setIncrement(increment);
            }
            this.excitatoryPrototype.setIncrement(increment);
        }

        // Enabled
        boolean enabled = excitatoryPrototype.isEnabled();
        if (this.excitatoryPrototype.isEnabled() != enabled
            || !useGroupLevelSettings) {
            for (Synapse s : exSynapseSet) {
                s.setEnabled(enabled);
            }
            this.excitatoryPrototype.setEnabled(enabled);
        }

        // Delay
        int delay = excitatoryPrototype.getDelay();
        if (this.excitatoryPrototype.getDelay() != delay
            || !useGroupLevelSettings) {
            for (Synapse s : exSynapseSet) {
                s.setDelay(delay);
            }
            this.excitatoryPrototype.setDelay(delay);
        }

        // Frozen
        boolean frozen = excitatoryPrototype.isFrozen();
        if (this.excitatoryPrototype.isFrozen() != frozen
            || !useGroupLevelSettings) {
            for (Synapse s : exSynapseSet) {
                s.setFrozen(frozen);
            }
            this.excitatoryPrototype.setFrozen(frozen);
        }
        this.setExcitatorySpikeResponders(excitatoryPrototype
            .getSpikeResponder());
        this.setExcitatoryRule(excitatoryPrototype.getLearningRule());
    }

    /**
     *
     * @param inhibitoryPrototype
     */
    public void setAndConformToTemplateInhibitory(Synapse inhibitoryPrototype) {
        // Ignore strength... must be set separately

        // Upper bound
        double upperBound = inhibitoryPrototype.getUpperBound();
        if (this.inhibitoryPrototype.getUpperBound() != upperBound
            || !useGroupLevelSettings) {
            for (Synapse s : inSynapseSet) {
                s.setUpperBound(upperBound);
            }
            this.inhibitoryPrototype.setUpperBound(upperBound);
        }

        // Lower Bound
        double lowerBound = inhibitoryPrototype.getLowerBound();
        if (this.inhibitoryPrototype.getLowerBound() != lowerBound
            || !useGroupLevelSettings) {
            for (Synapse s : inSynapseSet) {
                s.setLowerBound(upperBound);
            }
            this.inhibitoryPrototype.setLowerBound(upperBound);
        }

        // Increment
        double increment = inhibitoryPrototype.getIncrement();
        if (this.inhibitoryPrototype.getIncrement() != increment
            || !useGroupLevelSettings) {
            for (Synapse s : inSynapseSet) {
                s.setIncrement(increment);
            }
            this.inhibitoryPrototype.setIncrement(increment);
        }

        // Enabled
        boolean enabled = inhibitoryPrototype.isEnabled();
        if (this.inhibitoryPrototype.isEnabled() != enabled
            || !useGroupLevelSettings) {
            for (Synapse s : inSynapseSet) {
                s.setEnabled(enabled);
            }
            this.inhibitoryPrototype.setEnabled(enabled);
        }

        // Delay
        int delay = inhibitoryPrototype.getDelay();
        if (this.inhibitoryPrototype.getDelay() != delay
            || !useGroupLevelSettings) {
            for (Synapse s : inSynapseSet) {
                s.setDelay(delay);
            }
            this.inhibitoryPrototype.setDelay(delay);
        }

        // Frozen
        boolean frozen = inhibitoryPrototype.isFrozen();
        if (this.inhibitoryPrototype.isFrozen() != frozen
            || !useGroupLevelSettings) {
            for (Synapse s : inSynapseSet) {
                s.setFrozen(frozen);
            }
            this.inhibitoryPrototype.setFrozen(frozen);
        }
        this.setInhibitorySpikeResponders(inhibitoryPrototype
            .getSpikeResponder());
        this.setInhibitoryRule(inhibitoryPrototype.getLearningRule());
    }

    /**
     * @return the excitatory update rule
     */
    public SynapseUpdateRule getExcitatoryRule() {
        if (useGroupLevelSettings || exSynapseSet.isEmpty()) {
            return excitatoryPrototype.getLearningRule();
        }
        Iterator<Synapse> synIter = exSynapseSet.iterator();
        SynapseUpdateRule sur = synIter.next().getLearningRule();
        while (synIter.hasNext()) {
            if (sur.getClass() != synIter.next().getLearningRule().getClass()) {
                return null;
            }
        }
        return sur;
    }

    /**
     * Sets the update rule of all excitatory synapses to the specified rule.
     *
     * @param excitatoryRule
     */
    public void setExcitatoryRule(SynapseUpdateRule excitatoryRule) {
        excitatoryPrototype.setLearningRule(excitatoryRule.deepCopy());
        for (Synapse s : exSynapseSet) {
            s.setLearningRule(excitatoryRule.deepCopy());
        }
        exStatic = excitatoryRule instanceof StaticSynapseRule;
    }

    /**
     * @return the inhibitory update rule
     */
    public SynapseUpdateRule getInhibitoryRule() {
        if (useGroupLevelSettings || inSynapseSet.isEmpty()) {
            return inhibitoryPrototype.getLearningRule();
        }
        Iterator<Synapse> synIter = inSynapseSet.iterator();
        SynapseUpdateRule sur = synIter.next().getLearningRule();
        while (synIter.hasNext()) {
            if (sur.getClass() != synIter.next().getLearningRule().getClass()) {
                return null;
            }
        }
        return sur;
    }

    /**
     * Sets the update rule of all the inhibitory synapses to the specified
     * rule.
     *
     * @param inhibitoryRule
     */
    public void setInhibitoryRule(SynapseUpdateRule inhibitoryRule) {
        inhibitoryPrototype.setLearningRule(inhibitoryRule.deepCopy());
        for (Synapse s : inSynapseSet) {
            s.setLearningRule(inhibitoryRule.deepCopy());
        }
        inStatic = inhibitoryRule instanceof StaticSynapseRule;
    }

    public boolean isUseGroupLevelSettings() {
        return useGroupLevelSettings;
    }

    public void setUseGroupLevelSettings(boolean useGroupLevelSettings) {
        this.useGroupLevelSettings = useGroupLevelSettings;
    }

}