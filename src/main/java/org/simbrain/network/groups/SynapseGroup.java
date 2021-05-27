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

import org.simbrain.network.NetworkModel;
import org.simbrain.network.connections.ConnectionStrategy;
import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.*;
import org.simbrain.network.events.SynapseGroupEvents;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.NonResponder;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.network.util.SynapseSet;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Producible;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A group of synapses. Must connect a source and target neuron group.
 *
 * @author Zoë Tosi
 */
public class SynapseGroup extends NetworkModel implements CopyableObject, AttributeContainer {

    /**
     * Reference to the network this group is a part of.
     */
    private final Network parentNetwork;

    /**
     * Reference to source neuron group.
     */
    private final NeuronGroup sourceNeuronGroup;

    /**
     * Reference to target neuron group.
     */
    private final NeuronGroup targetNeuronGroup;

    /**
     * A set containing all the excitatory (wt > 0) synapses in the group.
     */
    private SynapseSet exSynapseSet = new SynapseSet(this, 0);

    /**
     * A set containing all the inhibitory (wt < 0) synapses in the group.
     */
    private SynapseSet inSynapseSet = new SynapseSet(this, 0);;

    /**
     * Event support
     */
    protected transient SynapseGroupEvents events = new SynapseGroupEvents(this);

    /**
     * The <b>default>/b> polarized randomizer associated with excitatory.
     * <p> synapse strengths for all synapse groups.
     */
    private static final ProbabilityDistribution DEFAULT_EX_RANDOMIZER =
        UniformDistribution.builder()
            .polarity(Polarity.EXCITATORY)
            .build();

    /**
     * The <b>default>/b> polarized randomizer associated with inhibitory
     * synapse strengths for all synapse groups.
     */
    private static final ProbabilityDistribution DEFAULT_IN_RANDOMIZER =
        UniformDistribution.builder()
            .polarity(Polarity.INHIBITORY)
            .build();

    /**
     * The default ratio (all excitatory) for all synapse groups.
     */
    public static final double DEFAULT_EXCITATORY_RATIO = .5;

    public static final ConnectionStrategy DEFAULT_CONNECTION_MANAGER = new Sparse(.1, false, false);

    /**
     * Connection strategy associated with this group.
     */
    private ConnectionStrategy connectionManager;

    /**
     * The percent of synapses that are excitatory. This parameter represents
     * the ideal value of {@link #exSynapseSet}.size() / {@link #size()} (the
     * <b>actual</b> excitatory ratio). This value is a mutable parameter the
     * changing of which will cause the synapse group to attempt to make: {@link
     * #exSynapseSet}.size() / {@link #size()} as close to the excitatoryRatio
     * value as possible (see {@link #setExcitatoryRatio(double)}). This means
     * that this value is an ideal that (usually) the actual excitatory ratio is
     * near, but seldom exactly equal to. Because of the way polarity is chosen
     * for new synapses added to the group this value represents a central limit
     * that the synapse group would absolutely reach exactly given an infinite
     * number of synapses. If the source neurons of this group are themselves
     * polarized the actual excitatory ratio of the synapses in this group will
     * reflect the excitatory ratio of the source neurons <b>not<b> this
     * variable. The group will however attempt to get as close to this ratio as
     * possible without ever assigning a synapse to a source neuron of opposing
     * polarity.
     */
    @UserParameter(label = "Excitatory ratio", editable = false, order = 50)
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
    private ProbabilityDistribution exciteRand = DEFAULT_EX_RANDOMIZER;

    /**
     * The randomizer governing inhibitory synapses. If null new synapses are
     * not randomized.
     */
    private ProbabilityDistribution inhibRand = DEFAULT_IN_RANDOMIZER;

    /**
     * Flag for whether synapses should be displayed in a GUI representation of
     * this object.
     */
    private boolean displaySynapses;

    /**
     * Whether or not this synapse group is recurrent.
     */
    private boolean recurrent;

    /**
     * A boolean flag set based on if all the inhibitory synapses in this group
     * are static. Technically this flag can be set to true even if all the
     * inhibitory synapses are not static. It is used as an optimization along
     * with {@link #useGroupLevelSettings} to determine whether or not all the
     * inhibitory synapses should be iterated over during update. This flag will
     * have no effect on panels or update procedure if {@link
     * #useGroupLevelSettings} is <b>false</b>, however if it is
     * <b>true</b> this flag will be dominant over the actual state of synapses
     * for the purpose of updating.
     */
    private boolean inStatic = true;

    /**
     * A boolean flag set based on if all the excitatory synapses in this group
     * are static. Technically this flag can be set to true even if all the
     * excitatory synapses are not static. It is used as an optimization along
     * with {@link #useGroupLevelSettings} to determine whether or not all the
     * excitatory synapses should be iterated over during update. This flag will
     * have no effect on panels or update procedure if {@link
     * #useGroupLevelSettings} is <b>false</b>, however if it is
     * <b>true</b> this flag will be dominant over the actual state of synapses
     * for the purpose of updating.
     */
    private boolean exStatic = false;

    // TODO: redundant with statics? When would group level be on but statics off?

    /**
     *
     * If true, use prototype synapses for all synapse properties, except strengths.
     *
     * Instead of iterating over all the synapses in the group to supply an answer, the method will return the
     * result of {@link #excitatoryPrototype}.isFrozen(). [todo]
     *
     * This is useful for
     * cases where synapses within synapse groups are entirely governed by group
     * level attributes and it is known to the user that individual synapse
     * settings will/should not be changed apart from the group.
     * <p>
     * If set to true a compressed representation of the weight matrix is used
     * in saving, see SynapseGroupConverter.compressedMatrixRep.
     */
    @UserParameter(label = "Compressed representation", order = 60)
    private boolean useGroupLevelSettings = false;

    /**
     * Completely creates a synapse group between the two neuron groups with all
     * default parameters. This method creates the individual connections.
     *
     * @param source the source neuron group.
     * @param target the target neuron group.
     * @return a synapse group with all default values connecting the source and
     * target neuron groups.
     */
    public static SynapseGroup createSynapseGroup(final NeuronGroup source, final NeuronGroup target) {
        return createSynapseGroup(source, target, DEFAULT_CONNECTION_MANAGER);
    }

    /**
     * Completely creates a synapse group with the desired parameters. That is
     * the connections (individual synapses) are created along with the group.
     *
     * @param source          the source neuron group.
     * @param target          the target neuron group. neurons in the group are
     *                        connected
     * @param excitatoryRatio the ratio of excitatory to inhibitory synapses [0,
     *                        1].
     * @return a synapse group with the above parameters.
     */
    public static SynapseGroup createSynapseGroup(final NeuronGroup source, final NeuronGroup target, final double excitatoryRatio) {
        return createSynapseGroup(source, target, DEFAULT_CONNECTION_MANAGER, excitatoryRatio, DEFAULT_EX_RANDOMIZER, DEFAULT_IN_RANDOMIZER);
    }

    /**
     * Completely creates a synapse group with the desired parameters. That is
     * the connections (individual synapses) are created along with the group.
     *
     * @param source            the source neuron group.
     * @param target            the target neuron group.
     * @param connectionManager the connection manager used to establish which
     * @return a synapse group with the above parameters.
     */
    public static SynapseGroup createSynapseGroup(final NeuronGroup source, final NeuronGroup target, final ConnectionStrategy connectionManager) {
        SynapseGroup synGroup = new SynapseGroup(source, target, connectionManager);
        synGroup.setRandomizers(DEFAULT_EX_RANDOMIZER, DEFAULT_IN_RANDOMIZER);
        synGroup.makeConnections();
        // Ensure that displayed ratio is consistent with actual ratio.
        // Process of determining synapse polarity is stochastic.
        synGroup.excitatoryRatio = synGroup.getExcitatoryRatioPrecise();

        return synGroup;
    }

    /**
     * Completely creates a synapse group with the desired parameters. That is
     * the connections (individual synapses) are created along with the group.
     *
     * @param source            the source neuron group.
     * @param target            the target neuron group.
     * @param connectionManager the connection manager used to establish which
     * @param excitatoryRatio   the ratio of excitatory to inhibitory synapses
     *                          [0, 1].
     * @return a synapse group with the above parameters.
     */
    public static SynapseGroup createSynapseGroup(final NeuronGroup source, final NeuronGroup target, final ConnectionStrategy connectionManager, final double excitatoryRatio) {
        return createSynapseGroup(source, target, connectionManager, excitatoryRatio, DEFAULT_EX_RANDOMIZER, DEFAULT_IN_RANDOMIZER);
    }

    /**
     * Completely creates a synapse group with the desired parameters. That is
     * the connections (individual synapses) are created along with the group.
     *
     * @param source            the source neuron group.
     * @param target            the target neuron group.
     * @param connectionManager the connection manager used to establish which
     * @param excitatoryRatio   the ratio of excitatory to inhibitory synapses
     *                          [0, 1].
     * @param exciteRand        the randomizer to be used to determine the
     *                          weights of excitatory synapses.
     * @param inhibRand         the randomizer to be used to determine the
     *                          weights of inhibitory synapses.
     * @return a synapse group with the above parameters.
     */
    public static SynapseGroup createSynapseGroup(
        final NeuronGroup source,
        final NeuronGroup target,
        final ConnectionStrategy connectionManager,
        double excitatoryRatio,
        final ProbabilityDistribution exciteRand,
        final ProbabilityDistribution inhibRand
    ) {
        SynapseGroup synGroup = new SynapseGroup(source, target, connectionManager);
        synGroup.setExcitatoryRatio(excitatoryRatio);
        synGroup.setRandomizers(exciteRand, inhibRand);
        synGroup.makeConnections();
        // Ensure that displayed ratio is consistent with actual ratio.
        // Process of determining synapse polarity is stochastic.
        synGroup.excitatoryRatio = synGroup.getExcitatoryRatioPrecise();
        return synGroup;
    }

    /**
     * Creates a blank synapse group between a source and target neuron group
     * using the default connection manager. Until {@link #makeConnections()} is
     * called this group will be empty and will not be added to the source or
     * target neuron groups' respective outgoing and incoming synapse group
     * sets.
     *
     * @param source the source neuron group.
     * @param target the target neuron group.
     */
    public SynapseGroup(final NeuronGroup source, final NeuronGroup target) {
        parentNetwork = source.getParentNetwork();
        this.sourceNeuronGroup = source;
        this.targetNeuronGroup = target;
        recurrent = testRecurrent();
        initializeSynapseVisibility();
        initSpikeResponders();
        source.addOutgoingSg(this);
        target.addIncomingSg(this);
        setLabel(parentNetwork.getIdManager().getProposedId(this.getClass()));
    }

    /**
     * Creates a blank synapse group between a source and target neuron group.
     * Until {@link #makeConnections()} is called this group will be empty and
     * will not be added to the source or target neuron groups' respective
     * outgoing and incoming synapse group sets.
     *
     * @param source            source neuron group
     * @param target            target neuron group
     * @param connectionManager a connection object which builds this group
     */
    private SynapseGroup(final NeuronGroup source, final NeuronGroup target, final ConnectionStrategy connectionManager) {
        parentNetwork = source.getParentNetwork();
        this.sourceNeuronGroup = source;
        this.targetNeuronGroup = target;
        this.connectionManager = connectionManager;
        recurrent = testRecurrent();
        initializeSynapseVisibility();
        initSpikeResponders();
        setLabel(parentNetwork.getIdManager().getProposedId(this.getClass()));
    }

    /**
     * Group level analog of {@link Synapse#initSpikeResponder()}
     */
    private void initSpikeResponders() {
        //When the source neuron group is spiking, prototype synapses should have spike responders.
        if(sourceNeuronGroup.isSpikingNeuronGroup()) {
            // Do not change existing spike responders if they are there already there
            if (excitatoryPrototype.getSpikeResponder() instanceof NonResponder) {
                excitatoryPrototype.setSpikeResponder(Synapse.DEFAULT_SPIKE_RESPONDER.deepCopy());
            } if (inhibitoryPrototype.getSpikeResponder() instanceof NonResponder) {
                inhibitoryPrototype.setSpikeResponder(Synapse.DEFAULT_SPIKE_RESPONDER.deepCopy());
            }
        } else {
            excitatoryPrototype.setSpikeResponder(new NonResponder());
            inhibitoryPrototype.setSpikeResponder(new NonResponder());
        }

    }

    /**
     * The "build" method which actually constructs the group in terms of
     * populating it with synapses. If called on a synapse group which has
     * already been made (i.e. already has synapses populating it) those
     * synapses will be destroyed and the current connection manager and
     * parameters used to create new connections. This method adds the current
     * synapse group the the source neuron group's outgoing synapse set and the
     * target neuron group's incoming synapse set.
     */
    public void makeConnections() {
        clear();
        sourceNeuronGroup.addOutgoingSg(this);
        targetNeuronGroup.addIncomingSg(this);
        connectionManager.connectNeurons(this);
        if (size() == 0) {
            String errMessage = "Synapse group creation failed because there are no synapses;";
            errMessage += "source neuron group = " + this.getSourceNeuronGroup().getLabel();
            errMessage += "; target neuron group = " + this.getTargetNeuronGroup().getLabel();
            delete();
            throw new IllegalStateException(errMessage);
        }
        events.fireVisibilityChange();
    }

    /**
     * Pre-allocates, that is sets the initial capacity of the arraylist
     * containing this synapse group's synapses. This allows expectedNumber of
     * synapses to be added to this synapse group without the synapse list
     * having to perform any operations related to expanding the list size.
     *
     * Sets initial capacity of hashsets, so that when you add new synapses
     * you reduce the chances of it rehashing, resizing etc when creating a large
     * synapse group.
     *
     *
     * @param expectedNumSynapses the number of synapses the connection manager
     *                            predicts will be created.
     * @throws IllegalStateException if the synapse group has already been
     *                               initialized.
     */
    public void preAllocateSynapses(int expectedNumSynapses) throws IllegalStateException {
        if (!exSynapseSet.isEmpty() || !inSynapseSet.isEmpty()) {
            throw new IllegalArgumentException("Cannot pre-allocate space for"
                + " some expected number of synapses"
                + " when one or both synapse sets are already populated."
                + " Pre-allocations can only occur before connections"
                + " have been initialized.");
        }
        // Using /0.8 instead of /0.75 because expected number is _expected_
        // but not precisely known.
        exSynapseSet = new SynapseSet(this, (int) (expectedNumSynapses * excitatoryRatio / 0.8));
        inSynapseSet = new SynapseSet(this, (int) (expectedNumSynapses * (1 - excitatoryRatio) / 0.8));
    }

    /**
     * If an algorithm (like training) extensively changes the polarity of the
     * synapses in this group, it's impractical to check every time a change is
     * made. Thus after the bulk of the algorithm is completed this method can
     * be called to sort synapses into their appropriate sets.
     */
    public void revalidateSynapseSets() {
        Iterator<Synapse> exIterator = exSynapseSet.iterator();
        ArrayList<Synapse> exSwitches = new ArrayList<>(exSynapseSet.size());
        while (exIterator.hasNext()) {
            Synapse s = exIterator.next();
            if (s.getStrength() < 0) {
                exSwitches.add(s);
                exIterator.remove();
            }
        }
        Iterator<Synapse> inIterator = inSynapseSet.iterator();
        ArrayList<Synapse> inSwitches = new ArrayList<>(inSynapseSet.size());
        while (inIterator.hasNext()) {
            Synapse s = inIterator.next();
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
     * Update group. Override for special updating. Recommended that overrides
     * call super.update() some time during the custom update.
     */
    public void update() {
        if (useGroupLevelSettings) {
            // If static, nothing to do!
            if (!exStatic) {
                if (!isFrozen(Polarity.EXCITATORY)) {
                    updateExcitatorySynapses();
                }
            }
            if (!inStatic) {
                if (!isFrozen(Polarity.INHIBITORY)) {
                    updateInhibitorySynapses();
                }
            }
        } else {
            updateExcitatorySynapses();
            updateInhibitorySynapses();
        }
    }

    private void updateExcitatorySynapses() {
        exSynapseSet.forEach(Synapse::update);
    }

    private void updateInhibitorySynapses() {
        inSynapseSet.forEach(Synapse::update);
    }

    public int size() {
        return exSynapseSet.size() + inSynapseSet.size();
    }

    public boolean isEmpty() {
        return exSynapseSet.isEmpty() && inSynapseSet.isEmpty();
    }

    public void delete() {
        clear();
        targetNeuronGroup.removeIncomingSg(this);
        sourceNeuronGroup.removeOutgoingSg(this);
        events.fireDeleted();
    }

    @Override
    public String toString() {
        return getId() + " with " + this.size()
                + " synapse(s) from "
                + getSourceNeuronGroup().getId()
                + " to " + getTargetNeuronGroup().getId();
    }

    /**
     * Determine whether this synpase group should initially have its synapses
     * displayed. For isolated synapse groups check its number of synapses. If
     * the maximum number of possible connections exceeds a the network's
     * synapse visibility threshold, then individual synapses will not be
     * displayed.
     */
    public void initializeSynapseVisibility() {
        int threshold = NetworkKt.getSynapseVisibilityThreshold();
        if (sourceNeuronGroup.size() * targetNeuronGroup.size() > threshold) {
            displaySynapses = false;
        } else {
            displaySynapses = true;
        }
    }

    public void setDisplaySynapses(boolean displaySynapses) {
        this.displaySynapses = displaySynapses;
        events.fireVisibilityChange();
    }

    public boolean isDisplaySynapses() {
        return displaySynapses;
    }

    /**
     * Remove the provided synapse from the group, but not the network.
     *
     * @param toDelete the synapse to delete
     * @return the deleted synapse
     */
    public Synapse removeSynapse(Synapse toDelete) {
        exSynapseSet.remove(toDelete);
        inSynapseSet.remove(toDelete);
        if (toDelete != null) {
            // TODO: Discuss np check with Zoë
            toDelete.getSource().removeEfferent(toDelete);
            toDelete.getTarget().removeAfferent(toDelete);
        }
        this.excitatoryRatio = getExcitatoryRatioPrecise();
        if (isDisplaySynapses()) {
            fireSynapseRemoved(toDelete);
        }
        // TODO: Fire event to just update this synapse
        if (isEmpty()) {
            delete();
        }
        return toDelete;
    }

    /**
     * Removes all synapses with weight 0 from the group.
     */
    public void prune() {
        for (Synapse s : this.getAllSynapses()) {
            if (s.getStrength() == 0) {
                removeSynapse(s);
            }
        }
    }

    public void hardClear() {
        exSynapseSet.forEach(Synapse::hardClear);
        inSynapseSet.forEach(Synapse::hardClear);
    }

    /**
     * Adds a new synapse (one which is "blank") to the synapse group. This is
     * the <b>preferred</b> method to use for adding synapses to the synapse
     * group over {@link #addSynapseUnsafe(Synapse)} because it makes the added
     * synapse conform to the global parameters of this synapse group.
     *
     * @param synapse the blank synapse to be added and assigned new values
     *                based on the parameters of this group.
     */
    public void addNewSynapse(final Synapse synapse) {
        if (synapse.getSource().isPolarized()) {
            if (Polarity.EXCITATORY.equals(synapse.getSource().getPolarity())) {
                addNewExcitatorySynapse(synapse);
            } else if (Polarity.INHIBITORY.equals(synapse.getSource().getPolarity())) {
                addNewInhibitorySynapse(synapse);
            }
        } else {
            double rand = Math.random();
            double correctionTerm = size() == 0 ? 0 : excitatoryRatio - (exSynapseSet.size() / (double) size());
            if (rand < (excitatoryRatio + correctionTerm)) {
                addNewExcitatorySynapse(synapse);
            } else {
                addNewInhibitorySynapse(synapse);
            }
        }
    }

    /**
     * @param synapse the blank excitatory synapse which will be added to the
     *                group and have its parameters set based on the parameters
     *                of this group.
     */
    public void addNewExcitatorySynapse(final Synapse synapse)  {
        synapse.setParentGroup(this);
        if (exciteRand != null) {
            synapse.setStrength(exciteRand.getRandom());
        } else {
            synapse.setStrength(ConnectionUtilities.DEFAULT_EXCITATORY_STRENGTH);
        }
        synapse.setLearningRule(excitatoryPrototype.getLearningRule().deepCopy());
        synapse.setFrozen(excitatoryPrototype.isFrozen());
        synapse.setEnabled(excitatoryPrototype.isEnabled());
        synapse.setDelay(excitatoryPrototype.getDelay());
        synapse.setIncrement(excitatoryPrototype.getIncrement());
        synapse.setUpperBound(excitatoryPrototype.getUpperBound());
        synapse.setLowerBound(excitatoryPrototype.getLowerBound());
        synapse.setSpikeResponder(excitatoryPrototype.getSpikeResponder());
        exSynapseSet.add(synapse);
        fireSynapseAdded(synapse);
    }

    /**
     * @param synapse the blank inhibitory synapse which will be added to the
     *                group and have its parameters set based on the parameters
     *                of this group.
     */
    public void addNewInhibitorySynapse(final Synapse synapse) {
        synapse.setParentGroup(this);
        if (inhibRand != null) {
            synapse.setStrength(inhibRand.getRandom());
        } else {
            synapse.setStrength(ConnectionUtilities.DEFAULT_INHIBITORY_STRENGTH);
        }
        synapse.setLearningRule(inhibitoryPrototype.getLearningRule().deepCopy());
        synapse.setFrozen(inhibitoryPrototype.isFrozen());
        synapse.setEnabled(inhibitoryPrototype.isEnabled());
        synapse.setDelay(inhibitoryPrototype.getDelay());
        synapse.setIncrement(inhibitoryPrototype.getIncrement());
        synapse.setUpperBound(inhibitoryPrototype.getUpperBound());
        synapse.setLowerBound(inhibitoryPrototype.getLowerBound());
        synapse.setSpikeResponder(inhibitoryPrototype.getSpikeResponder());
        inSynapseSet.add(synapse);
        fireSynapseAdded(synapse);
    }

    /**
     * Add a synapse to this synapse group adding it to the appropriate synapse
     * set (excitatory or inhibitory if it's weight is above or below zero
     * respecively). Using this method is <b>NOT RECOMMENDED</b> under most
     * circumstances. This is because no checks are performed on the synapse to
     * ensure that it can sensibly be added to this group. The synapse can also
     * have parameters which will make the global parameters of the synapse
     * group no longer accurately apply to the whole group. This is not as
     * problematic as say not connecting neurons which are in this synapse
     * group's source and/or target neuron groups, but it does undermine the
     * purpose of having synapse groups.
     * <p>
     * Possible Use Case: When it is known beforehand that the synapse(s) being
     * added all conform to the parameters of this synapse group.
     *
     * @param synapse synapse to add
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
     * See: {@link #addSynapseUnsafe(Synapse)}. Same but specific to excitatory
     * synapses. This is even less safe however because an inhibitory synapse
     * could potentially be added to the excitatory set.
     *
     * @param synapse the synapse to add.
     */
    public void addExcitatorySynapseUnsafe(final Synapse synapse) {
        exSynapseSet.add(synapse);
        excitatoryRatio = exSynapseSet.size() / (double) size();
        if (getParentNetwork() != null) {
            synapse.setParentGroup(this);
        }
        fireSynapseAdded(synapse);
    }

    /**
     * See: {@link #addSynapseUnsafe(Synapse)}. Same but specific to inhibitory
     * synapses. This is even less safe however because an excitatory synapse
     * could potentially be added to the inhibitory set.
     *
     * @param synapse the synapse to add.
     */
    public void addInhibitorySynapseUnsafe(final Synapse synapse) {
        inSynapseSet.add(synapse);
        excitatoryRatio = exSynapseSet.size() / (double) size();
        if (getParentNetwork() != null) {
            synapse.setParentGroup(this);
        }
        fireSynapseAdded(synapse);
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
     * @param excitatoryRatio the ratio of synapses which will be made
     *                        excitatory, value must be in the range [0, 1]
     * @throws IllegalArgumentException if the ratio is not on [0, 1].
     */
    public void setExcitatoryRatio(double excitatoryRatio) throws IllegalArgumentException {
        if (excitatoryRatio > 1 || excitatoryRatio < 0) {
            throw new IllegalArgumentException("The parameter" + " 'excitatoryRatio' passed to setExcitatoryRatio" + " must be on [0, 1]");
        }

        // Return if there is no change or the group is empty.
        if (excitatoryRatio == getExcitatoryRatioPrecise()) {
            return;
        }
        if (isEmpty()) {
            this.excitatoryRatio = excitatoryRatio;
            return;
        }

        if (excitatoryRatio < getExcitatoryRatioPrecise()) {
            int numSwitch = (int) ((this.excitatoryRatio * size()) - (excitatoryRatio * size()));
            Iterator<Synapse> setIterator = exSynapseSet.iterator();
            while (setIterator.hasNext()) {
                Synapse s = setIterator.next();
                if (!s.getSource().isPolarized() && numSwitch > 0) {
                    setIterator.remove();
                    if (inhibRand != null) {
                        s.setStrength(inhibRand.getRandom());
                    } else {
                        s.setStrength(ConnectionUtilities.DEFAULT_INHIBITORY_STRENGTH);
                    }
                    s.setLearningRule(inhibitoryPrototype.getLearningRule().deepCopy());
                    s.setFrozen(inhibitoryPrototype.isFrozen());
                    s.setEnabled(inhibitoryPrototype.isEnabled());
                    s.setDelay(inhibitoryPrototype.getDelay());
                    s.setIncrement(inhibitoryPrototype.getIncrement());
                    s.setUpperBound(inhibitoryPrototype.getUpperBound());
                    s.setLowerBound(inhibitoryPrototype.getLowerBound());
                    s.setSpikeResponder(inhibitoryPrototype.getSpikeResponder());
                    inSynapseSet.add(s);
                    numSwitch--;
                }
            }
        } else {
            int numSwitch = (int) ((excitatoryRatio * size()) - (getExcitatoryRatioPrecise() * size()));
            Iterator<Synapse> setIterator = inSynapseSet.iterator();
            while (setIterator.hasNext()) {
                Synapse s = setIterator.next();
                if (!s.getSource().isPolarized() && numSwitch > 0) {
                    setIterator.remove();
                    if (exciteRand != null) {
                        s.setStrength(exciteRand.getRandom());
                    } else {
                        s.setStrength(ConnectionUtilities.DEFAULT_EXCITATORY_STRENGTH);
                    }
                    s.setLearningRule(excitatoryPrototype.getLearningRule().deepCopy());
                    s.setFrozen(excitatoryPrototype.isFrozen());
                    s.setEnabled(excitatoryPrototype.isEnabled());
                    s.setDelay(excitatoryPrototype.getDelay());
                    s.setIncrement(excitatoryPrototype.getIncrement());
                    s.setUpperBound(excitatoryPrototype.getUpperBound());
                    s.setLowerBound(excitatoryPrototype.getLowerBound());
                    s.setSpikeResponder(excitatoryPrototype.getSpikeResponder());
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
     * @return the ration of excitatory synapses in this group
     */
    public double getExcitatoryRatioParameter() {
        return excitatoryRatio;
    }

    /**
     * @return the <b>actual</b> excitatory ratio measured as the number of
     * excitatory synapses divided by the total.
     */
    public double getExcitatoryRatioPrecise() {
        return exSynapseSet.size() / (double) size();
    }

    /**
     * @return a flat list representation of all the synapses in this synapse
     * group. This list is a defensive copy.
     */
    public List<Synapse> getAllSynapses() {
        ArrayList<Synapse> flatList = new ArrayList<Synapse>(size());
        flatList.addAll(getExcitatorySynapses());
        flatList.addAll(getInhibitorySynapses());
        return flatList;
    }

    public Set<Synapse> getExcitatorySynapses() {
        return new HashSet<>(exSynapseSet);
    }

    public Set<Synapse> getInhibitorySynapses() {
        return new HashSet<>(inSynapseSet);
    }

    @Producible
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
    @Producible
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
    @Producible
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
     * parameter. If the synapse does not exist in this group returns false. If
     * the this makes the synapse change polarity it will be removed from its
     * current set and added to the appropriate set.
     *
     * @param synapse   sets the strength of an individual synapse in the group
     * @param newWeight the new weight to set it to
     * @return true if this group contained the specified synapse, and false if
     * it did not and thus failed to set the strength value.
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
     * {@link #randomizeExcitatoryConnections()}, {@link
     * #randomizeInhibitoryConnections()}
     */
    @Override
    public void randomize() {
        randomizeExcitatoryConnections();
        randomizeInhibitoryConnections();
    }

    /**
     * Randomizes the weights of the excitatory connections in this group based
     * on the parameters of {@link #exciteRand}. Assumes that all synapses in
     * {@link #exSynapseSet} are--in fact--excitatory. If some action on the
     * synapses may have corrupted that assumption call {@link
     * #revalidateSynapseSets()} first.
     */
    public void randomizeExcitatoryConnections() {
        ConnectionUtilities.randomizeExcitatorySynapsesUnsafe(exSynapseSet, exciteRand);
    }

    /**
     * Randomizes the weights of the inhibitory connections in this group based
     * on the parameters of {@link #inhibRand}. Assumes that all synapses in
     * {@link #inSynapseSet} are--in fact--inhibitory. If some action on the
     * synapses may have corrupted that assumption call {@link
     * #revalidateSynapseSets()} first.
     */
    public void randomizeInhibitoryConnections() {
        ConnectionUtilities.randomizeInhibitorySynapsesUnsafe(inSynapseSet, inhibRand);
    }

    /**
     * Sets the connection manager for this synapse group.
     *
     * @param connection the connection manager to be used by this synapse group
     *                   for making synaptic connections.
     */
    public void setConnectionManager(ConnectionStrategy connection) {
            this.connectionManager = connection;
    }

    /**
     * @return the connection manager for this synapse group.
     */
    public ConnectionStrategy getConnectionManager() {
        return connectionManager;
    }

    /**
     * @param excitatoryRandomizer the randomizer to be used to determine the
     *                             weights of excitatory synapses.
     */
    public void setExcitatoryRandomizer(ProbabilityDistribution excitatoryRandomizer) {
        this.exciteRand = excitatoryRandomizer == null ? null : excitatoryRandomizer.deepCopy();
        if(exciteRand != null)
            exciteRand.setPolarity(Polarity.EXCITATORY);
    }

    /**
     * @param inhibitoryRandomizer the randomizer to be used to determine the
     *                             weights of inbihitory synapses.
     */
    public void setInhibitoryRandomizer(ProbabilityDistribution inhibitoryRandomizer) {
        inhibRand = inhibitoryRandomizer == null ? null : inhibitoryRandomizer.deepCopy();
        if(inhibRand != null)
            inhibRand.setPolarity(Polarity.INHIBITORY);
    }

    public void setRandomizers(ProbabilityDistribution excitatoryRandomizer, ProbabilityDistribution inhibitoryRandomizer) {
        setExcitatoryRandomizer(excitatoryRandomizer);
        setInhibitoryRandomizer(inhibitoryRandomizer);
    }

    public ProbabilityDistribution getExcitatoryRandomizer() {
        return exciteRand;
    }

    public ProbabilityDistribution getInhibitoryRandomizer() {
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
     * @return if this synapse group's source neuron group and target neuron
     * group are the same group.
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

    public NeuronGroup getSourceNeuronGroup() {
        return sourceNeuronGroup;
    }

    public NeuronGroup getTargetNeuronGroup() {
        return targetNeuronGroup;
    }

    /**
     * Reveals the excitatory prototype synapse, allowing more detailed synapse
     * values to be queried at the group level. To use a prototype to change
     * many values call {@link #setAndConformToTemplate(Synapse, Polarity)}
     *
     * @return the prototype synapse
     */
    public Synapse getExcitatoryPrototype() {
        return Synapse.copyTemplateSynapse(excitatoryPrototype);
    }

    /**
     * Reveals the inhibitory prototype synapse, allowing more detailed synapse
     * values to be queried at the group level. To use a prototype to change
     * many values call {@link #setAndConformToTemplate(Synapse, Polarity)}
     *
     * @return the prototype synapse
     */
    public Synapse getInhibitoryPrototype() {
        return Synapse.copyTemplateSynapse(inhibitoryPrototype);
    }

    /**
     * @return whether or not the synapse group is using group-level properties
     * Homogeneous within synapse type (excitatory/inhibitory) for faster
     * indexing and optimized updating.
     */
    public boolean isUseGroupLevelSettings() {
        return useGroupLevelSettings;
    }

    public void setUseGroupLevelSettings(boolean useGroupLevelSettings) {
        this.useGroupLevelSettings = useGroupLevelSettings;
    }

    public void setAndConformToTemplate(Synapse template, Polarity polarity) {
        setDelay(template.getDelay(), polarity);
        setEnabled(template.isEnabled(), polarity);
        setFrozen(template.isFrozen(), polarity);
        setIncrement(template.getIncrement(), polarity);
        setLearningRule(template.getLearningRule(), polarity);
        setLowerBound(template.getLowerBound(), polarity);
        setSpikeResponder(template.getSpikeResponder(), polarity);
        setUpperBound(template.getUpperBound(), polarity);
        if (Polarity.EXCITATORY == polarity) {
            excitatoryPrototype = template;
        } else if (Polarity.INHIBITORY == polarity) {
            inhibitoryPrototype = template;
        } else {
            excitatoryPrototype = Synapse.copyTemplateSynapse(template);
            inhibitoryPrototype = Synapse.copyTemplateSynapse(template);
        }
    }

    public void setDelay(int delay, Polarity polarity) {
        setProperty(s -> s.setDelay(delay), Polarity.BOTH);
    }

    public void setEnabled(boolean enabled) {
        setProperty(s -> s.setEnabled(enabled), Polarity.BOTH);
    }

    public void setEnabled(boolean enabled, Polarity polarity) {
        setProperty(s -> s.setEnabled(enabled), polarity);
    }

    public void setFrozen(boolean frozen, Polarity polarity) {
        setProperty(s -> s.setFrozen(frozen), polarity);
    }

    public void setIncrement(double increment, Polarity polarity) {
        setProperty(s -> s.setIncrement(increment), polarity);
    }

    public void setLearningRule(SynapseUpdateRule sur, Polarity polarity) {
        setProperty(s -> s.setLearningRule(sur), polarity);
        if (Polarity.EXCITATORY == polarity) {
            exStatic = sur instanceof StaticSynapseRule;
        } else if (Polarity.INHIBITORY == polarity) {
            inStatic = sur instanceof StaticSynapseRule;
        } else {
            exStatic = sur instanceof StaticSynapseRule;
            inStatic = sur instanceof StaticSynapseRule;
        }
    }

    public void setLowerBound(double lowerBound, Polarity polarity) {
        setProperty(s -> s.setLowerBound(lowerBound), polarity);
    }

    public void setSpikeResponder(SpikeResponder spr, Polarity polarity) {
        if (spr == null) {
            return;
        }
        setProperty(s -> s.setSpikeResponder(spr), polarity);
    }

    public void setStrength(double strength, Polarity polarity) {
        final double str = polarity.value(strength);
        setProperty(s -> s.setStrength(str), polarity);
        if (Polarity.BOTH == polarity) {
            if (strength > 0) {
                exSynapseSet.addAll(inSynapseSet);
                inSynapseSet.clear();
                excitatoryRatio = 1;
            } else {
                inSynapseSet.addAll(exSynapseSet);
                exSynapseSet.clear();
                excitatoryRatio = 0;
            }
        }
    }

    public void setUpperBound(double upperBound, Polarity polarity) {
        setProperty(s -> s.setUpperBound(upperBound), polarity);
    }

    public Integer getDelay(Polarity polarity) {
        return getProperty(Synapse::getDelay, polarity);
    }

    public Boolean isEnabled(Polarity polarity) {
        return getProperty(Synapse::isEnabled, polarity);
    }

    public Boolean isFrozen(Polarity polarity) {
        return getProperty(Synapse::isFrozen, polarity);
    }

    public double getIncrement(Polarity polarity) {
        return getProperty(Synapse::getIncrement, polarity);
    }

    public String getLearningRuleDescription(Polarity polarity) {
        String rule = getProperty(s -> s.getLearningRule().getName(), polarity);
        return rule == null ? SimbrainConstants.NULL_STRING : rule;
    }

    public double getLowerBound(Polarity polarity) {
        Double lowB = getProperty(Synapse::getLowerBound, polarity);
        return lowB == null ? Double.NaN : lowB;
    }

    public double getUpperBound(Polarity polarity) {
        Double upB = getProperty(Synapse::getUpperBound, polarity);
        return upB == null ? Double.NaN : upB;
    }

    public String getSpikeResponderDescription(Polarity polarity) {
        String rule = getProperty(s -> s.getSpikeResponder().getDescription(), polarity);
        return rule == null ? SimbrainConstants.NULL_STRING : rule;
    }

    /**
     * Returns a property associated with the synapse group.  If the property
     * is consistent throughout the group the consistent value is returned.
     * If the property is inconsistent null is returned. Polarity can be
     * specified to only look at excitatory or inhibitory (or all) synapses.
     *
     * @param action the function that returns the property, e.g. <code>Synapse::getIncrement</code>
     * @param polarity which polarity to check
     * @param <T> the generic type of the returned value
     * @return the value of this property
     */
    public <T> T getProperty(Function<Synapse, T> action, Polarity polarity) {

        Collection<Synapse> synapses;

        // Group level settings or empty group
        if (Polarity.EXCITATORY == polarity) {
            synapses = exSynapseSet;
            if (useGroupLevelSettings || exSynapseSet.isEmpty()) {
                return action.apply(excitatoryPrototype);
            }
        } else if (Polarity.INHIBITORY == polarity) {
            synapses = inSynapseSet;
            if (useGroupLevelSettings || inSynapseSet.isEmpty()) {
                return action.apply(inhibitoryPrototype);
            }
        } else {
            synapses = getAllSynapses();
            if (synapses.isEmpty()) {
                return null;
            }
        }

        // Return null if they are inconsistent, or the value if they are consistent
        Iterator<Synapse> synIter = synapses.iterator();
        T first = action.apply(synIter.next());
        while (synIter.hasNext()) {
            if (!first.equals(action.apply(synIter.next()))) {
                return null;
            }
        }
        return first;
    }

    /**
     * Applies a lambda (e.g. setting strength) to synapses in this group,
     * depending on their polarity.
     *
     * @param action the lambda to apply, e.g. <code>s -> s.setStrength(s)</code>
     * @param polarity which synapses to apply the lambda to
     */
    public void setProperty(Consumer<Synapse> action, Polarity polarity) {
        if(polarity == Polarity.EXCITATORY) {
            exSynapseSet.forEach(action);
            action.accept(excitatoryPrototype);
        } else if (polarity == Polarity.INHIBITORY) {
            inSynapseSet.forEach(action);
            action.accept(inhibitoryPrototype);
        } else {
            exSynapseSet.forEach(action);
            inSynapseSet.forEach(action);
            action.accept(excitatoryPrototype);
            action.accept(inhibitoryPrototype);
        }
    }

    @Override
    public void postUnmarshallingInit() {
        if (events == null) {
            events = new SynapseGroupEvents(this);
        }
        exSynapseSet.forEach(Synapse::postUnmarshallingInit);
        inSynapseSet.forEach(Synapse::postUnmarshallingInit);
        revalidateSynapseSets();
    }

    @Override
    public SynapseGroup copy() {
        // TODO
        return null;
    }

    /**
     * Notify listeners that a synapse has been added.
     *
     * @param synapse synapse to add
     */
    private void fireSynapseAdded(Synapse synapse) {
        events.fireSynapseAdded(synapse);
    }

    /**
     * Notify listeners that a synapse has been removed.
     *
     * @param synapse synapse to remove
     */
    private void fireSynapseRemoved(Synapse synapse) {
        events.fireSynapseRemoved(synapse);
    }

    public Network getParentNetwork() {
        return parentNetwork;
    }

    public SynapseGroupEvents getEvents() {
        return events;
    }
}