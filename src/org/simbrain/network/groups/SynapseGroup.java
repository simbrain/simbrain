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

import org.simbrain.network.connections.ConnectionStrategy;
import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.network.connections.ConnectionUtilities.SynapseParameterGetter;
import org.simbrain.network.connections.ConnectionUtilities.SynapseParameterSetter;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.gui.nodes.SynapseGroupNode;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.network.util.io_utilities.GroupDeserializer;
import org.simbrain.network.util.io_utilities.GroupSerializer.Precision;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.Utils;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.propertyeditor2.EditableObject;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * A group of synapses. Must connect a source and target neuron group.
 *
 * @author Zoë Tosi
 */
public class SynapseGroup extends Group {

    /**
     * The <b>default>/b> polarized randomizer associated with excitatory.
     * <p>
     * synapse strengths for all synapse groups.
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

    /**
     * All to All.
     */
    public static final ConnectionStrategy DEFAULT_CONNECTION_MANAGER = new Sparse(.1, false, false);

    /**
     * A set containing all the excitatory (wt > 0) synapses in the group.
     */
    private Set<Synapse> exSynapseSet = new HashSet<Synapse>();

    /**
     * A set containing all the inhibitory (wt < 0) synapses in the group.
     */
    private Set<Synapse> inSynapseSet = new HashSet<Synapse>();

    /**
     * The precision with which weights should be saved.
     */
    private Precision serialzationPrecision = Precision.FLOAT_64;

    /**
     * A temporary set containing all the excitatory synapses in the group. Used
     * when saving synapse groups since the regular set is destroyed. If the
     * group is going to continue being used after saving the values in this
     * temporary holder are used to repopulate the excitatory synapse set.
     */
    private Set<Synapse> exTemp;

    /**
     * A temporary set containing all the inhibitory synapses in the group. Used
     * when saving synapse groups since the regular set is destroyed. If the
     * group is going to continue being used after saving the values in this
     * temporary holder are used to repopulate the inhibitory synapse set.
     */
    private Set<Synapse> inTemp;

    /**
     * Reference to source neuron group.
     */
    private final NeuronGroup sourceNeuronGroup;

    /**
     * Reference to target neuron group.
     */
    private final NeuronGroup targetNeuronGroup;

    /**
     * The connect neurons object associated with this group.
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
    private ProbabilityDistribution exciteRand;

    /**
     * The randomizer governing inhibitory synapses. If null new synapses are
     * not randomized.
     */
    private ProbabilityDistribution inhibRand;

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

    /**
     * Whether or not group level settings i.e. : those stored in {@link
     * #excitatoryPrototype} and {@link #inhibitoryPrototype} and the flags
     * {@link #inStatic} and {@link #exStatic} should be dominant over the
     * actual values stored in the individual synapses for the purposes of
     * updating and queries made to this group. For instance: If this setting
     * is
     * <b>true</b>, then instead of iterating over all the
     * synapses in the group to supply an answer, the method will return the
     * result of {@link #excitatoryPrototype}.isFrozen(). This is useful for
     * cases where synapses within synapse groups are entirely governed by group
     * level attributes and it is known to the user that individual synapse
     * settings will/should not be changed apart from the group.
     * <p>
     * If set to true a compressed representation of the weight matrix is used
     * in saving, see {@link SynapseGroup#compressedMatrixRep}.
     */
    private boolean useGroupLevelSettings = true;

    /**
     * This field is used to store the compressed representation of a synapse
     * group's weight matrix. If compression is on (i.e. group level settings
     * are on) it is populated just before saving. Else it is null.
     */
    private byte[] compressedMatrixRep = null;

    /**
     * A byte-encoded representation of all relevant synapse parameters of all
     * synapses in the group. The compressed rep only stores synapse weights,
     * relying on the prototype synapses ({@link #excitatoryPrototype} and
     * {@link #inhibitoryPrototype}) to fill in the remaining values. But if the
     * synapses also all have different delays, PSRs, etc. This representation
     * saves all of those.
     */
    private byte[] fullSynapseRep = null;

    /**
     * Whether or not to use the compressed rep or the full rep.
     */
    private boolean useFullRepOnSave = false;

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
     * @param source            source neuron group
     * @param target            target neuron group
     * @param connectionManager a connection object which builds this group
     */
    private SynapseGroup(final NeuronGroup source, final NeuronGroup target, final ConnectionStrategy connectionManager) {
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
    }

    /**
     * Pre-allocates, that is sets the initial capacity of the arraylist
     * containing this synapse group's synapses. This allows expectedNumber of
     * synapses to be added to this synapse group without the synapse list
     * having to perform any operations related to expanding the list size.
     *
     * @param expectedNumSynapses the number of synapses the connection manager
     *                            predicts will be created.
     * @throws IllegalStateException if the synapse group has already been
     *                               initialized.
     */
    public void preAllocateSynapses(int expectedNumSynapses) throws IllegalStateException {
        if (!exSynapseSet.isEmpty() || !inSynapseSet.isEmpty()) {
            throw new IllegalArgumentException("Cannot pre-allocate space for"
                + " some expected number of synapses when the synapse"
                + " when one or both synapse sets are already populated."
                + " Pre-allocations can only occur before connections"
                + " have been initialized.");
        }
        // Using /0.8 instead of /0.75 because expected number is _expected_
        // but not precisely known.
        exSynapseSet = new HashSet<Synapse>((int) (expectedNumSynapses * excitatoryRatio / 0.8));
        inSynapseSet = new HashSet<Synapse>((int) (expectedNumSynapses * (1 - excitatoryRatio) / 0.8));
    }

    /**
     * If an algorithm (like training) extensively changes the polarity of the
     * synapses in this group, it's impractical to check every time a change is
     * made. Thus after the bulk of the algorithm is completed this method can
     * be called to sort synapses into their appropriate sets.
     */
    public void revalidateSynapseSets() {
        Iterator<Synapse> exIterator = exSynapseSet.iterator();
        ArrayList<Synapse> exSwitches = new ArrayList<Synapse>(exSynapseSet.size());
        while (exIterator.hasNext()) {
            Synapse s = exIterator.next();
            if (s.getStrength() < 0) {
                exSwitches.add(s);
                exIterator.remove();
            }
        }
        Iterator<Synapse> inIterator = inSynapseSet.iterator();
        ArrayList<Synapse> inSwitches = new ArrayList<Synapse>(inSynapseSet.size());
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
            if (!exStatic) { // Only iterate if excitatory synapses aren't static
                // Assuming they're not static, only iterate if they aren't frozen.
                if (!isFrozen(Polarity.EXCITATORY)) {
                    updateExcitatorySynapses();
                }
            }

            if (!inStatic) { // Only iterate if inhibitory synapses aren't static
                // Assuming they're not static, only iterate if they aren't frozen.
                if (!isFrozen(Polarity.INHIBITORY)) {
                    updateInhibitorySynapses();
                }
            }
        } else {
            updateExcitatorySynapses();
            updateInhibitorySynapses();
        }
    }

    /**
     *
     */
    private void updateExcitatorySynapses() {
        for (Synapse synapse : exSynapseSet) {
            synapse.update();
        }
    }

    /**
     *
     */
    private void updateInhibitorySynapses() {
        for (Synapse synapse : inSynapseSet) {
            synapse.update();
        }
    }

    /**
     * {@inheritDoc}
     */
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
        ret += ("Synapse Group [" + getLabel() + "]. Contains " + this.size() + " synapse(s)." + " Connects " + getSourceNeuronGroup().getId() + " [" + getSourceNeuronGroup().getLabel() + "]" + " to " + getTargetNeuronGroup().getId() + " [" + getTargetNeuronGroup().getLabel() + "]\n");

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
     * @param displaySynapses the displaySynapses to set
     */
    public void setDisplaySynapses(boolean displaySynapses) {
        this.displaySynapses = displaySynapses;
        // getParentNetwork().fireGroupChanged(new NetworkEvent<Group>(getParentNetwork(), this, this),
        //     SynapseGroupNode.SYNAPSE_VISIBILITY_CHANGED);
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
     * @param toDelete the synapse to delete
     * @return the deleted synapse
     */
    public Synapse removeSynapse(Synapse toDelete) {
        exSynapseSet.remove(toDelete);
        inSynapseSet.remove(toDelete);
        toDelete.getSource().removeEfferent(toDelete);
        toDelete.getTarget().removeAfferent(toDelete);
        this.excitatoryRatio = getExcitatoryRatioPrecise();
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
     * Removes all synapses with weight 0 from the group.
     */
    public void prune() {
        for (Synapse s : this.getAllSynapses()) {
            if (s.getStrength() == 0) {
                removeSynapse(s);
            }
        }
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
    public void addNewExcitatorySynapse(final Synapse synapse) {
        getParentNetwork().fireGroupChanged(this, this, "synapseAdded");
        synapse.setId(getParentNetwork().getSynapseIdGenerator().getId());
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
    }

    /**
     * @param synapse the blank inhibitory synapse which will be added to the
     *                group and have its parameters set based on the parameters
     *                of this group.
     */
    public void addNewInhibitorySynapse(final Synapse synapse) {
        getParentNetwork().fireGroupChanged(this, this, "synapseAdded");
        synapse.setId(getParentNetwork().getSynapseIdGenerator().getId());
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
            synapse.setId(getParentNetwork().getSynapseIdGenerator().getId());
            synapse.setParentGroup(this);
        }
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
            synapse.setId(getParentNetwork().getSynapseIdGenerator().getId());
            synapse.setParentGroup(this);
        }
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

    /**
     * @return the set of excitatory synapses
     */
    public Set<Synapse> getExcitatorySynapses() {
        return new HashSet<Synapse>(exSynapseSet);
    }

    /**
     * @return the set of inhibitory synapses
     */
    public Set<Synapse> getInhibitorySynapses() {
        return new HashSet<Synapse>(inSynapseSet);
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

    // TODO: Consider putting some of the below methods or part of their bodies
    // into SimbrainMath or Utils

    /**
     * For large, sparse synapse groups this will cause a heap overflow. Use
     * {@link #getRowCompressedMatrixRepresentation()} instead.
     *
     * @return a representation of the synapse strengths in this synapse group
     * as a weight matrix between two activation vectors (neuron groups).
     */
    public double[][] getWeightMatrix() {
        double[][] weightMatrix = new double[getSourceNeurons().size()][getTargetNeurons().size()];
        int i = 0;
        int j = 0;
        // Create numbers for neurons... less expensive than constant
        // indexOf calls to array lists.
        Map<Neuron, Integer> sourceMap = new HashMap<Neuron, Integer>();
        Map<Neuron, Integer> targetMap = new HashMap<Neuron, Integer>();
        for (Neuron n : getSourceNeurons()) {
            sourceMap.put(n, i++);
        }
        for (Neuron n : getTargetNeurons()) {
            targetMap.put(n, j++);
        }
        // Construct uncompressed matrix from weights
        for (Synapse s : getExcitatorySynapses()) {
            i = sourceMap.get(s.getSource());
            j = targetMap.get(s.getTarget());
            weightMatrix[i][j] = s.getStrength();
        }
        for (Synapse s : getInhibitorySynapses()) {
            i = sourceMap.get(s.getSource());
            j = targetMap.get(s.getTarget());
            weightMatrix[i][j] = s.getStrength();
        }
        return weightMatrix;
    }

    /**
     * A more compressed version of a weight matrix for cases where a weight
     * matrix is needed, but may cause memory issues if fully instantiated. Eg,
     * for very sparse synapse groups between very large neuron groups.
     *
     * @return a 2D array with a number of rows equal to the total number of
     * synapses and a number of columns equal to 3. Each row contains the the
     * source index number, the target index number, and the strength in that
     * order. This array is then sorted by source index then target index. Ex: 1
     * 2 .9 0 3 5.3 0 1 -.1 Becomes: 0 1 -.1 0 3 5.3 1 2 .9
     */
    public double[][] getNumericIndices() {

        double[][] pairs = new double[size()][3];
        int i = 0;
        int j = 0;
        // Create numbers for neurons... less expensive than constant
        // indexOf calls to array lists.
        Map<Neuron, Integer> sourceMap = new HashMap<Neuron, Integer>((int) (sourceNeuronGroup.size() / 0.75));
        Map<Neuron, Integer> targetMap = new HashMap<Neuron, Integer>((int) (targetNeuronGroup.size() / 0.75));
        // Assign indices to each source and target neuron in a lookup table
        // so that synapses can be identified by a source and target index
        for (Neuron n : getSourceNeurons()) {
            sourceMap.put(n, i++);
        }
        for (Neuron n : getTargetNeurons()) {
            targetMap.put(n, j++);
        }
        // Put each synapse strength into a table [i, j, w], where i is the
        // source neuron index in a weight matrix, j is the target index and
        // w is the synapse strength.
        int k = 0;
        for (Synapse s : getAllSynapses()) {
            pairs[k++] = new double[] {sourceMap.get(s.getSource()), targetMap.get(s.getTarget()), s.getStrength()};
        }
        // Create a comparator to sort synapse table entries by source, then
        // by column.
        Comparator<double[]> rowColOrderer = new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                if (o1[0] < o2[0]) {
                    return -1;
                } else if (o1[0] > o2[0]) {
                    return 1;
                } else {
                    if (o1[1] < o2[1]) {
                        return -1;
                    } else if (o1[1] > o2[1]) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            }
        };
        // And sort the table
        Arrays.sort(pairs, rowColOrderer);
        return pairs;
    }

    /**
     * A more compressed version of a weight matrix for cases where a weight
     * matrix is needed, but may cause memory issues if fully instantiated. Eg,
     * for very sparse synapse groups between very large neuron groups.
     *
     * @return a 2D array with a number of rows equal to the total number of
     * synapses and a number of columns equal to 3. Each row contains the the
     * source index number, the target index number, and the strength in that
     * order. This array is then sorted by source index then target index. Ex: 1
     * 2 .9 0 3 5.3 0 1 -.1 Becomes: 0 1 -.1 0 3 5.3 1 2 .9
     */
    public Number[][] getNumericIndices(SynapseParameterGetter<Number> getter) {
        Number[][] pairs = new Number[size()][3];
        int i = 0;
        int j = 0;
        // Create numbers for neurons... less expensive than constant
        // indexOf calls to array lists.
        Map<Neuron, Integer> sourceMap = new HashMap<Neuron, Integer>((int) (sourceNeuronGroup.size() / 0.75));
        Map<Neuron, Integer> targetMap = new HashMap<Neuron, Integer>((int) (targetNeuronGroup.size() / 0.75));
        // Assign indices to each source and target neuron in a lookup table
        // so that synapses can be identified by a source and target index
        for (Neuron n : getSourceNeurons()) {
            sourceMap.put(n, i++);
        }
        for (Neuron n : getTargetNeurons()) {
            targetMap.put(n, j++);
        }
        // Put each synapse strength into a table [i, j, w], where i is the
        // source neuron index in a weight matrix, j is the target index and
        // w is the synapse strength.
        int k = 0;
        for (Synapse s : getAllSynapses()) {
            pairs[k++] = new Number[] {sourceMap.get(s.getSource()), targetMap.get(s.getTarget()), getter.getParameterFromSynapse(s)};
        }
        // Create a comparator to sort synapse table entries by source, then
        // by column.
        Comparator<Number[]> rowColOrderer = new Comparator<Number[]>() {
            @Override
            public int compare(Number[] o1, Number[] o2) {
                if (o1[0].doubleValue() < o2[0].doubleValue()) {
                    return -1;
                } else if (o1[0].doubleValue() > o2[0].doubleValue()) {
                    return 1;
                } else {
                    if (o1[1].doubleValue() < o2[1].doubleValue()) {
                        return -1;
                    } else if (o1[1].doubleValue() > o2[1].doubleValue()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            }
        };
        // And sort the table
        Arrays.sort(pairs, rowColOrderer);
        return pairs;
    }

    /**
     * @return a row compressed representation of the weight matrix derived from
     * this synapse group. All values are stored as longs, and row changes are
     * denoted by -1.
     */
    public long[] getRowCompressedMatrixRepresentation() {
        double[][] pairs = getNumericIndices();
        int numSyns = size();
        int numSrc = sourceNeuronGroup.size();
        long[] compRowRep = new long[numSrc + (2 * numSyns)];
        int currRow = 0;
        int m = 0;
        compRowRep[0] = numSyns;
        int l = 1;
        for (int n = numSyns + numSrc; l < n; l++) {
            if (m == numSyns) {
                break;
            }
            if (pairs[m][0] != currRow) {
                compRowRep[l] = -1L;
                currRow++;
            } else {
                compRowRep[l] = (long) pairs[m][1];
                compRowRep[numSyns + numSrc + m] = Double.doubleToLongBits(pairs[m][2]);
                m++;
            }
        }
        // Adds trailing "new row" markers when the last synapse comes from a
        // source neuron other than the last one. This represents empty rows on the end.
        while(currRow < numSrc-1) {
            compRowRep[l++] = -1L;
            currRow++;
        }

        return compRowRep;
    }


    /**
     * Produces a sparse representation of the synapse group and returns that data as a byte buffer. Ordering is such
     * that meta-data comes first indicating a marker which is currently used for backwards compatibility indicating that
     * the bytes here represent the new serialization scheme. This is followed by the number of synapses. From there
     * each source neuron index is followed by the number of outgoing connections it has and then the indices of the
     * outgoing connections in the target neuron group. All indices and metadata are encoded as integers. The remaining
     * bytes are bit represenations of the FP weight values either in single or double precision.
     *
     *
     * @param precision what precision to story the weights.
     * @return
     */
    public ByteBuffer getSparseCode(Precision precision) {
        double[][] pairs = getNumericIndices();
        // Can't use src group size because some neurons in the group may not have any synaptic connections to the target
        int numSrc = 0;
        int curSrc = -1;
        // keeps track of indices of the beginnings and ends of target neuron to each source
        List<Integer> localOutInds = new ArrayList<>();
        for(int ii=0; ii<pairs.length; ++ii) {
            if (curSrc != pairs[ii][0]) {
                localOutInds.add(ii);
                curSrc = (int) pairs[ii][0];
                numSrc++;
            }
        }
        localOutInds.add(pairs.length);
        ByteBuffer buffer;
        if(precision == Precision.FLOAT_64) {
             buffer = ByteBuffer.allocate(4 * (2*numSrc + 3*pairs.length) + 4 + 1 + 4 + 4);
        } else {
             buffer = ByteBuffer.allocate(4 * (2*numSrc + 2*pairs.length) + 4 + 1 + 4 + 4);
        }

        buffer.putInt(-1); // Marker so what we know that the new serialization method is being used.
        // Meta encoding whether or not double precision is being used
        buffer.put((byte) (precision == Precision.FLOAT_64 ? 0x1 : 0x0));
        // Meta number of synapses
        buffer.putInt(pairs.length);
        // Meta number of EFFECTIVE source neurons--ones with at least one outgoing connection in this group
        buffer.putInt(numSrc);
        for(int ii=0; ii < numSrc; ++ii) {
            int start = localOutInds.get(ii);
            int end = localOutInds.get(ii+1);
            buffer.putInt((int) pairs[start][0]); // SourceIndex
            buffer.putInt(end-start); // number of targets
            for(int jj = start; jj< end; ++jj) {
                buffer.putInt((int) pairs[jj][1]);
            }
        }

        if (precision == Precision.FLOAT_64) {
            for(int ii=0, n=pairs.length; ii<n; ++ii) {
                buffer.putLong(Double.doubleToLongBits(pairs[ii][2]));
            }
        } else {
            for(int ii=0, n=pairs.length; ii<n; ++ii) {
                buffer.putInt(Float.floatToIntBits((float) pairs[ii][2]));
            }
        }

        return  buffer;

    }

    /**
     * Saves the synapse group to a file in linked list format which can be read
     * by most graph analysis software, in particular as of 6/27/2015 the author
     * is aware of both OSLOM and Infomap which can interpret the linked list
     * format.
     * <p>
     * Format is as follows: source index target index weight
     * <p>
     * By default ".dat" is appended to the given filename since .dat is the
     * standard extension for linked lists.
     *
     * @param filename the name to be given to the file
     */
    public void saveToFileAsLinkedList(String filename) {
        try (FileWriter fw = new FileWriter(filename + ".dat"); PrintWriter pw = new PrintWriter(fw);) {
            double[][] pairs = getNumericIndices();
            int offset = this.isRecurrent() ? 0 : sourceNeuronGroup.size();
            for (int i = 0, n = pairs.length; i < n; i++) {
                pw.print((int) pairs[i][0]);
                pw.print(" ");
                pw.print((int) pairs[i][1] + offset);
                pw.print(" ");
                pw.println(pairs[i][2]);
            }
        } catch (IOException ie) {
            ie.printStackTrace();
            System.out.println("Synapse group linked-list write failed.");
        }
    }

    /**
     * See: {@link #saveToFileAsLinkedList(String)} Uses the default name which
     * is: label + date & time
     */
    public void saveToFileAsLinkedList() {
        saveToFileAsLinkedList(getLabel() + "_" + Utils.getTimeString());
    }

    /**
     * Saves the weight matrix represented by this synapse group to a file.
     *
     * @param filename the name of the file to be used
     * @throws OutOfMemoryError if {@link #getWeightMatrix()} causes an out of
     *                          memory error for being to large.
     */
    public void saveToFileAsMatrix(String filename) throws OutOfMemoryError {
        try (FileWriter fw = new FileWriter(filename); PrintWriter pw = new PrintWriter(fw);) {
            double[][] wtMat = getWeightMatrix();
            for (double[] row : wtMat) {
                for (double col : row) {
                    pw.print(col + ", ");
                }
                pw.println();
            }
        } catch (IOException ie) {
            System.err.println("Failed to save Synapse Group " + getLabel() + " as matrix. File IO issue.");
            ie.printStackTrace();
        }
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
    public void randomizeConnectionWeights() {
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
        if(excitatoryRandomizer != null)
            excitatoryRandomizer.setPolarity(Polarity.EXCITATORY);
        this.exciteRand = excitatoryRandomizer;
    }

    /**
     * @param inhibitoryRandomizer the randomizer to be used to determine the
     *                             weights of inbihitory synapses.
     */
    public void setInhibitoryRandomizer(ProbabilityDistribution inhibitoryRandomizer) {
        if(inhibitoryRandomizer != null)
            inhibitoryRandomizer.setPolarity(Polarity.INHIBITORY);
        this.inhibRand = inhibitoryRandomizer;
    }

    /**
     * @param excitatoryRandomizer
     * @param inhibitoryRandomizer
     */
    public void setRandomizers(ProbabilityDistribution excitatoryRandomizer, ProbabilityDistribution inhibitoryRandomizer) {
        setExcitatoryRandomizer(excitatoryRandomizer);
        setInhibitoryRandomizer(inhibitoryRandomizer);
    }

    /**
     * @return
     */
    public ProbabilityDistribution getExcitatoryRandomizer() {
        return exciteRand;
    }

    /**
     * @return
     */
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

    /**
     * @param useGroupLevelSettings
     */
    public void setUseGroupLevelSettings(boolean useGroupLevelSettings) {
        this.useGroupLevelSettings = useGroupLevelSettings;
    }

    /**
     * @param template the set synapse template
     * @param polarity
     */
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

    /**
     * @param delay    the set delay
     * @param polarity
     */
    public void setDelay(int delay, Polarity polarity) {
        SynapseParameterSetter<Integer> setDelay = new SynapseParameterSetter<Integer>() {
            @Override
            public void setSynapseParameter(Synapse synapse, Integer val) {
                synapse.setDelay(val);
            }
        };
        setSynapses(setDelay, delay, polarity);
    }

    /**
     * Enable / disable synapses for both polarities.
     *
     * @param enabled whether they should be enabled or not
     */
    public void setEnabled(boolean enabled) {
        setEnabled(enabled, Polarity.BOTH);
    }

    /**
     * @param enabled
     * @param polarity
     */
    public void setEnabled(boolean enabled, Polarity polarity) {
        SynapseParameterSetter<Boolean> setEnabled = new SynapseParameterSetter<Boolean>() {
            @Override
            public void setSynapseParameter(Synapse synapse, Boolean val) {
                synapse.setEnabled(val);
            }
        };
        setSynapses(setEnabled, enabled, polarity);
    }

    /**
     * @param frozen
     * @param polarity
     */
    public void setFrozen(boolean frozen, Polarity polarity) {
        SynapseParameterSetter<Boolean> setFrozen = new SynapseParameterSetter<Boolean>() {
            @Override
            public void setSynapseParameter(Synapse synapse, Boolean val) {
                synapse.setFrozen(val);
            }
        };
        setSynapses(setFrozen, frozen, polarity);
    }

    /**
     * @param increment
     * @param polarity
     */
    public void setIncrement(double increment, Polarity polarity) {
        SynapseParameterSetter<Double> setIncrement = new SynapseParameterSetter<Double>() {
            @Override
            public void setSynapseParameter(Synapse synapse, Double val) {
                synapse.setIncrement(val);
            }
        };
        setSynapses(setIncrement, increment, polarity);
    }

    /**
     * @param sur
     * @param polarity
     */
    public void setLearningRule(SynapseUpdateRule sur, Polarity polarity) {
        SynapseParameterSetter<SynapseUpdateRule> setSUR = new SynapseParameterSetter<SynapseUpdateRule>() {
            @Override
            public void setSynapseParameter(Synapse synapse, SynapseUpdateRule val) {
                synapse.setLearningRule(val.deepCopy());
            }
        };
        setSynapses(setSUR, sur, polarity);
        if (Polarity.EXCITATORY == polarity) {
            exStatic = sur instanceof StaticSynapseRule;
        } else if (Polarity.INHIBITORY == polarity) {
            inStatic = sur instanceof StaticSynapseRule;
        } else {
            exStatic = sur instanceof StaticSynapseRule;
            inStatic = sur instanceof StaticSynapseRule;
        }
    }

    /**
     * @param lowerBound
     * @param polarity
     */
    public void setLowerBound(double lowerBound, Polarity polarity) {
        SynapseParameterSetter<Double> setLowBound = new SynapseParameterSetter<Double>() {
            @Override
            public void setSynapseParameter(Synapse synapse, Double val) {
                synapse.setLowerBound(val);
            }
        };
        setSynapses(setLowBound, lowerBound, polarity);
    }

    /**
     * @param spr
     * @param polarity
     */
    public void setSpikeResponder(SpikeResponder spr, Polarity polarity) {
        SynapseParameterSetter<SpikeResponder> setSPR = new SynapseParameterSetter<SpikeResponder>() {
            @Override
            public void setSynapseParameter(Synapse synapse, SpikeResponder sr) {
                if (sr == null) {
                    return;
                }
                synapse.setSpikeResponder(sr.deepCopy());
            }
        };
        setSynapses(setSPR, spr, polarity);
    }

    /**
     * @param strength
     * @param polarity
     */
    public void setStrength(double strength, Polarity polarity) {
        strength = polarity.value(strength);
        SynapseParameterSetter<Double> setStrength = new SynapseParameterSetter<Double>() {
            @Override
            public void setSynapseParameter(Synapse synapse, Double val) {
                synapse.setStrength(val);
            }
        };
        setSynapses(setStrength, strength, polarity);
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

    /**
     * @param upperBound
     * @param polarity
     */
    public void setUpperBound(double upperBound, Polarity polarity) {
        SynapseParameterSetter<Double> setUpBound = new SynapseParameterSetter<Double>() {
            @Override
            public void setSynapseParameter(Synapse synapse, Double val) {
                synapse.setUpperBound(val);
            }
        };
        setSynapses(setUpBound, upperBound, polarity);
    }

    /**
     * @param polarity
     * @return
     */
    public Integer getDelay(Polarity polarity) {
        SynapseParameterGetter<Integer> delayCheck = new SynapseParameterGetter<Integer>() {
            @Override
            public Integer getParameterFromSynapse(Synapse synapse) {
                return synapse.getDelay();
            }
        };
        return checkSynapses(delayCheck, polarity);
    }

    /**
     * @param polarity
     * @return
     */
    public Boolean isEnabled(Polarity polarity) {
        SynapseParameterGetter<Boolean> enabledCheck = new SynapseParameterGetter<Boolean>() {
            @Override
            public Boolean getParameterFromSynapse(Synapse synapse) {
                return synapse.isEnabled();
            }
        };
        return checkSynapses(enabledCheck, polarity);
    }

    /**
     * @param polarity
     * @return
     */
    public Boolean isFrozen(Polarity polarity) {
        SynapseParameterGetter<Boolean> frozenCheck = new SynapseParameterGetter<Boolean>() {
            @Override
            public Boolean getParameterFromSynapse(Synapse synapse) {
                return synapse.isFrozen();
            }
        };
        return checkSynapses(frozenCheck, polarity);
    }

    /**
     * @param polarity
     * @return
     */
    public double getIncrement(Polarity polarity) {
        SynapseParameterGetter<Double> incrementCheck = new SynapseParameterGetter<Double>() {
            @Override
            public Double getParameterFromSynapse(Synapse synapse) {
                return synapse.getIncrement();
            }
        };
        Double increment = checkSynapses(incrementCheck, polarity);
        return increment == null ? Double.NaN : increment;
    }

    /**
     * @param polarity
     * @return
     */
    public String getLearningRuleDescription(Polarity polarity) {
        SynapseParameterGetter<String> updateRuleCheck = new SynapseParameterGetter<String>() {
            @Override
            public String getParameterFromSynapse(Synapse synapse) {
                return synapse.getLearningRule().getName();
            }
        };
        String rule = checkSynapses(updateRuleCheck, polarity);
        return rule == null ? SimbrainConstants.NULL_STRING : rule;
    }

    /**
     * @param polarity
     * @return
     */
    public double getLowerBound(Polarity polarity) {
        SynapseParameterGetter<Double> lowBoundCheck = new SynapseParameterGetter<Double>() {
            @Override
            public Double getParameterFromSynapse(Synapse synapse) {
                return synapse.getLowerBound();
            }
        };
        Double lowB = checkSynapses(lowBoundCheck, polarity);
        return lowB == null ? Double.NaN : lowB;
    }

    /**
     * @param polarity
     * @return
     */
    public String getSpikeResponderDescription(Polarity polarity) {
        SynapseParameterGetter<String> spikeResponderCheck = new SynapseParameterGetter<String>() {
            @Override
            public String getParameterFromSynapse(Synapse synapse) {
                return synapse.getSpikeResponder().getDescription();
            }
        };
        String rule = checkSynapses(spikeResponderCheck, polarity);
        return rule == null ? SimbrainConstants.NULL_STRING : rule;
    }

    /**
     * @param polarity
     * @return
     */
    public double getUpperBound(Polarity polarity) {
        SynapseParameterGetter<Double> upBoundCheck = new SynapseParameterGetter<Double>() {
            @Override
            public Double getParameterFromSynapse(Synapse synapse) {
                return synapse.getUpperBound();
            }
        };
        Double upB = checkSynapses(upBoundCheck, polarity);
        return upB == null ? Double.NaN : upB;
    }

    /**
     * A generic method which takes in a functional interface and a polarity. It
     * then performs that function on every synapse in the set(s) corresponding
     * to the polarity parameter and returns a result matching the type
     * specified by the passed function.
     *
     * @param <T>
     * @param check
     * @param polarity
     * @return
     */
    public <T> T checkSynapses(SynapseParameterGetter<T> check, Polarity polarity) {
        Collection<Synapse> synapses;
        Synapse prototype;
        if (Polarity.EXCITATORY == polarity) {
            synapses = exSynapseSet;
            prototype = excitatoryPrototype;
            if (useGroupLevelSettings || synapses.isEmpty()) {
                return check.getParameterFromSynapse(prototype);
            }
        } else if (Polarity.INHIBITORY == polarity) {
            synapses = inSynapseSet;
            prototype = inhibitoryPrototype;
            if (useGroupLevelSettings || synapses.isEmpty()) {
                return check.getParameterFromSynapse(prototype);
            }
        } else {
            synapses = getAllSynapses();
            if (synapses.isEmpty()) {
                return null;
            }
        }
        Iterator<Synapse> synIter = synapses.iterator();
        T first = check.getParameterFromSynapse(synIter.next());
        while (synIter.hasNext()) {
            if (!first.equals(check.getParameterFromSynapse(synIter.next()))) {
                return null;
            }
        }
        return first;
    }

    /**
     * A generic method that takes in a functional interface, a value and a
     * polarity. The functional interface takes in a synapse and a value and the
     * calling method specifies which parameter of synapse the function sets the
     * value to. This method then performs that function (setting some synapse
     * parameter) on all the synapses in the set(s) corresponding to the
     * specified polarity.
     *
     * @param <T>
     * @param set
     * @param val
     * @param polarity
     */
    public <T> void setSynapses(SynapseParameterSetter<T> set, T val, Polarity polarity) {
        Collection<Synapse> synapses;
        if (Polarity.EXCITATORY == polarity) {
            synapses = exSynapseSet;
            set.setSynapseParameter(excitatoryPrototype, val);
        } else if (Polarity.INHIBITORY == polarity) {
            synapses = inSynapseSet;
            set.setSynapseParameter(inhibitoryPrototype, val);
        } else {
            synapses = getAllSynapses();
            set.setSynapseParameter(excitatoryPrototype, val);
            set.setSynapseParameter(inhibitoryPrototype, val);
        }
        for (Synapse s : synapses) {
            set.setSynapseParameter(s, val);
        }
    }

    public boolean isUseFullRepOnSave() {
        return useFullRepOnSave;
    }

    public void setUseFullRepOnSave(boolean useFullRepOnSave) {
        this.useFullRepOnSave = useFullRepOnSave;
    }

    /**
     * Perform operations required before saving a synapse group.
     */
    public void preSaveInit() {
        if (isUseFullRepOnSave()) {
            preSaveInitFull();
            return;
        }
        if (isUseGroupLevelSettings()) {
            long[] rowCompression = getRowCompressedMatrixRepresentation();
            // long start = System.nanoTime();
            // System.out.println("Begin Serialization... ");
            compressedMatrixRep = getSparseCode(serialzationPrecision).array();//GroupSerializer.rowCompMat2CompByteArray(rowCompression, Precision.FLOAT_32);
            // long end = System.nanoTime();
            // System.out.println("Serialization Time: "
            // + SimbrainMath.roundDouble((end - start) / Math.pow(10, 9),
            // 4) + " secs.");

            // Don't explicitly save the synapses.
            inTemp = inSynapseSet;
            exTemp = exSynapseSet;
            inSynapseSet = null;
            exSynapseSet = null;

        } else {
            compressedMatrixRep = null;
        }
    }

    /**
     * The pre-save init to be used to save all relevant synapse parameters in
     * the byte array.
     */
    public void preSaveInitFull() {
        Map<Neuron, Integer> srcMap = new HashMap<Neuron, Integer>((int) (sourceNeuronGroup.size() / 0.75));
        Map<Neuron, Integer> tarMap = new HashMap<Neuron, Integer>((int) (targetNeuronGroup.size() / 0.75));
        int i = 0;
        for (Neuron n : sourceNeuronGroup.getNeuronList()) {
            srcMap.put(n, i++);
        }
        i = 0;
        for (Neuron n : targetNeuronGroup.getNeuronList()) {
            tarMap.put(n, i++);
        }
        byte[][] synBytes = new byte[size()][];
        i = 0;
        int totalBytes = 0;
        for (Synapse s : this.getAllSynapses()) {
            byte[] synCode = s.getNumericValuesAsByteArray();
            ByteBuffer indices = ByteBuffer.allocate(8);
            indices.putInt(srcMap.get(s.getSource()).intValue());
            indices.putInt(tarMap.get(s.getTarget()).intValue());
            int index = synCode.length - 8;
            for (int j = index, n = synCode.length; j < n; j++) {
                synCode[j] = indices.array()[j - index];
            }
            synBytes[i++] = synCode;
            totalBytes += synCode.length;
        }
        ByteBuffer buff = ByteBuffer.allocate(totalBytes);
        for (byte[] synCodes : synBytes) {
            buff.put(synCodes);
        }
        fullSynapseRep = buff.array();
        inTemp = inSynapseSet;
        exTemp = exSynapseSet;
        inSynapseSet = null;
        exSynapseSet = null;
    }

    /**
     * A post initialization which must be done if the user wants to save the
     * network, but continue using the network after saving (since the saving
     * process sets the synapse sets to null.
     */
    public void postSaveReInit() {
        if (isUseGroupLevelSettings()) {
            inSynapseSet = inTemp;
            exSynapseSet = exTemp;
            inTemp = null;
            exTemp = null;
        }
    }

    /**
     * Perform operations required after opening a synapse group.
     */
    public void postUnmarshallingInit() {

        // Rebuild weight matrix if needed.
        if (this.isUseGroupLevelSettings() && compressedMatrixRep != null) {
            exSynapseSet = new HashSet<Synapse>();
            inSynapseSet = new HashSet<Synapse>();
            GroupDeserializer.reconstructCompressedSynapseStrengths(this.compressedMatrixRep, this);
            this.compressedMatrixRep = null;
            setAndConformToTemplate(excitatoryPrototype, Polarity.EXCITATORY);
            setAndConformToTemplate(inhibitoryPrototype, Polarity.INHIBITORY);
        } else if (fullSynapseRep != null) {
            exSynapseSet = new HashSet<Synapse>();
            inSynapseSet = new HashSet<Synapse>();
            Map<Integer, Neuron> srcMap = new HashMap<Integer, Neuron>((int) (sourceNeuronGroup.size() / 0.75));
            Map<Integer, Neuron> tarMap = new HashMap<Integer, Neuron>((int) (targetNeuronGroup.size() / 0.75));
            int i = 0;
            for (Neuron n : sourceNeuronGroup.getNeuronList()) {
                srcMap.put(i++, n);
            }
            i = 0;
            for (Neuron n : targetNeuronGroup.getNeuronList()) {
                tarMap.put(i++, n);
            }
            ByteBuffer bigBuff = ByteBuffer.wrap(fullSynapseRep);
            while (bigBuff.hasRemaining()) {
                int delay = bigBuff.getInt();
                int codeBuffSize = 20 + (delay * 8) + 4 + 1;
                ByteBuffer codeBuff = ByteBuffer.allocate(codeBuffSize);
                codeBuff.putInt(delay);
                byte[] data = new byte[codeBuffSize - 4];
                bigBuff.get(data);
                codeBuff.put(data);
                Neuron src = srcMap.get(bigBuff.getInt());
                Neuron tar = tarMap.get(bigBuff.getInt());
                Synapse s = new Synapse(src, tar);
                s.decodeNumericByteArray(ByteBuffer.wrap(codeBuff.array()));
                addSynapseUnsafe(s);
            }
            setIncrement(excitatoryPrototype.getIncrement(), Polarity.EXCITATORY);
            setLearningRule(excitatoryPrototype.getLearningRule(), Polarity.EXCITATORY);
            setSpikeResponder(excitatoryPrototype.getSpikeResponder(), Polarity.EXCITATORY);
            setLowerBound(excitatoryPrototype.getLowerBound(), Polarity.EXCITATORY);
            setUpperBound(excitatoryPrototype.getUpperBound(), Polarity.EXCITATORY);
            setIncrement(inhibitoryPrototype.getIncrement(), Polarity.INHIBITORY);
            setLearningRule(inhibitoryPrototype.getLearningRule(), Polarity.INHIBITORY);
            setSpikeResponder(inhibitoryPrototype.getSpikeResponder(), Polarity.INHIBITORY);
            setLowerBound(inhibitoryPrototype.getLowerBound(), Polarity.INHIBITORY);
            setUpperBound(inhibitoryPrototype.getUpperBound(), Polarity.INHIBITORY);
            fullSynapseRep = null;
        } else {
            for (Synapse synapse : this.getAllSynapses()) {
                synapse.postUnmarshallingInit();
            }
        }
        if (connectionManager instanceof Sparse) {
            ((Sparse) connectionManager).setPermitDensityEditing(false);
        }
    }

    /**
     * Take another synapse group and copy it's synapses in to this one.
     *
     * @param sgToCopy
     */
    public void copySynapses(SynapseGroup sgToCopy) {
        if ((this.getTargetNeuronGroup().size() != sgToCopy.getTargetNeuronGroup().size()) || (this.getSourceNeuronGroup().size() != sgToCopy.getSourceNeuronGroup().size())) {

            throw new IllegalArgumentException("Size of source and target neuron groups of this synapse group do not match size " + "of source and target neuron groups of this synapse group.");
        }

        clear();

        List<Neuron> srcNeurons = sgToCopy.sourceNeuronGroup.getNeuronListUnsafe();
        List<Neuron> tarNeurons = sgToCopy.targetNeuronGroup.getNeuronListUnsafe();
        for (int ii = 0; ii < sourceNeuronGroup.size(); ii++) {
            for (int jj = 0; jj < targetNeuronGroup.size(); jj++) {
                // Does a synapse exist here?
                if (srcNeurons.get(ii).getFanOut().containsKey(tarNeurons.get(jj))) {
                    //TODO: Check that such a synapse actually exists in this group
                    Synapse newSyn = new Synapse(sourceNeuronGroup.getNeuronListUnsafe().get(ii), targetNeuronGroup.getNeuronListUnsafe().get(jj));
                    newSyn.setStrength(srcNeurons.get(ii).getFanOut().get(tarNeurons.get(jj)).getStrength());
                    this.addSynapseUnsafe(newSyn);
                }
            }
        }
    }

    public Precision getSerialzationPrecision() {
        return serialzationPrecision;
    }

    public void setSerialzationPrecision(Precision serialzationPrecision) {
        this.serialzationPrecision = serialzationPrecision;
    }

    //TODO
    @Override
    public EditableObject copy() {
        return this.copy();
    }
}