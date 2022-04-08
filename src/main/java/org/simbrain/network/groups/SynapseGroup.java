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
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.*;
import org.simbrain.network.events.SynapseGroupEvents;
import org.simbrain.network.matrix.WeightMatrix;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.NonResponder;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.network.util.SynapseSet;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.UniformRealDistribution;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Producible;

import java.util.*;
import java.util.stream.Collectors;

import static org.simbrain.network.connections.ConnectionUtilitiesKt.*;

/**
 * A group of synapses. Must connect a source and target neuron group.
 * <p>
 * Previously contained many optimizations. However, for larger uses cases we now moving to matrix backed entities, like
 * {@link WeightMatrix}.
 *
 * @author Zoë Tosi
 */
public class SynapseGroup extends NetworkModel implements EditableObject, AttributeContainer {

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
    private SynapseSet inSynapseSet = new SynapseSet(this, 0);

    /**
     * Event support
     */
    protected transient SynapseGroupEvents events = new SynapseGroupEvents(this);

    /**
     * The <b>default>/b> polarized randomizer associated with excitatory.
     * <p> synapse strengths for all synapse groups.
     */
    private static final ProbabilityDistribution DEFAULT_EX_RANDOMIZER = new UniformRealDistribution();

    /**
     * The <b>default>/b> polarized randomizer associated with inhibitory synapse strengths for all synapse groups.
     */
    private static final ProbabilityDistribution DEFAULT_IN_RANDOMIZER = new UniformRealDistribution();

    /**
     * The default ratio (all excitatory) for all synapse groups.
     */
    public static final double DEFAULT_EXCITATORY_RATIO = .5;

    public static final ConnectionStrategy DEFAULT_CONNECTION_MANAGER = new Sparse();

    /**
     * Connection strategy associated with this group.
     */
    private ConnectionStrategy connectionManager;

    /**
     * The percent of synapses that are excitatory. This parameter represents the ideal value of {@link
     * #exSynapseSet}.size() / {@link #size()} (the
     * <b>actual</b> excitatory ratio). This value is a mutable parameter the
     * changing of which will cause the synapse group to attempt to make: {@link #exSynapseSet}.size() / {@link #size()}
     * as close to the excitatoryRatio value as possible (see {@link #setExcitatoryRatio(double)}). This means that this
     * value is an ideal that (usually) the actual excitatory ratio is near, but seldom exactly equal to. Because of the
     * way polarity is chosen for new synapses added to the group this value represents a central limit that the synapse
     * group would absolutely reach exactly given an infinite number of synapses. If the source neurons of this group
     * are themselves polarized the actual excitatory ratio of the synapses in this group will reflect the excitatory
     * ratio of the source neurons <b>not<b> this variable. The group will however attempt to get as close to this ratio
     * as possible without ever assigning a synapse to a source neuron of opposing polarity.
     */
    @UserParameter(label = "Excitatory ratio", editable = false, order = 50)
    private double excitatoryRatio = DEFAULT_EXCITATORY_RATIO;

    @UserParameter(label = "Excitatory Learning Rule", useSetter = true,
            isObjectType = true, order = 100)
    private SynapseUpdateRule exLearningRule = new StaticSynapseRule();

    @UserParameter(label = "Excitatory Spike Responder", isObjectType = true,
            showDetails = false, order = 200)
    private SpikeResponder exSpikeResponder = new NonResponder();

    @UserParameter(label = "Inhibitory Learning Rule", useSetter = true,
            isObjectType = true, order = 100)
    private SynapseUpdateRule inLearningRule = new StaticSynapseRule();

    @UserParameter(label = "Inhibitory Spike Responder", isObjectType = true,
            showDetails = false, order = 200)
    private SpikeResponder inSpikeResponder = new NonResponder();

    /**
     * The randomizer governing excitatory synapses. If null new synapses are not randomized.
     */
    private ProbabilityDistribution exciteRand = DEFAULT_EX_RANDOMIZER;

    /**
     * The randomizer governing inhibitory synapses. If null new synapses are not randomized.
     */
    private ProbabilityDistribution inhibRand = DEFAULT_IN_RANDOMIZER;

    /**
     * Flag for whether synapses should be displayed in a GUI representation of this object.
     */
    private boolean displaySynapses;

    /**
     * Whether or not this synapse group is recurrent.
     */
    private boolean recurrent;

    /**
     * Creates a synapse group with the desired parameters. Last argument is variable argument.
     *
     * @param source the source neuron group.
     * @param target the target neuron group.
     * @param args   args[0] the connection manager args[1] the ratio of excitatory to inhibitory synapses args[2] the
     *               randomizer to be used to determine the weights of excitatory synapses args[3] the randomizer to be
     *               used to determine the weights of inhibitory synapses.
     * @return a synapse group with the above parameters.
     */
    public static SynapseGroup createSynapseGroup(
            final NeuronGroup source,
            final NeuronGroup target,
            Object... args
    ) {
        SynapseGroup synGroup;
        if (args.length == 0) {
            synGroup = new SynapseGroup(source, target, DEFAULT_CONNECTION_MANAGER);
        } else {
            synGroup = new SynapseGroup(source, target, (ConnectionStrategy) args[0]);
        }

        if (args.length >= 2) {
            synGroup.setExcitatoryRatio((Double) args[1]);
        }
        if (args.length >= 3) {
            synGroup.setExcitatoryRandomizer((ProbabilityDistribution) args[2]);
        }
        if (args.length >= 4) {
            synGroup.setRandomizers((ProbabilityDistribution) args[3], (ProbabilityDistribution) args[3]);
        }
        synGroup.makeConnections();
        // Ensure that displayed ratio is consistent with actual ratio.
        // Process of determining synapse polarity is stochastic.
        synGroup.excitatoryRatio = synGroup.getExcitatoryRatioPrecise();
        return synGroup;
    }

    /**
     * Private constructor for static builder methods. Only creates the synapse group. Until {@link #makeConnections()}
     * is called this group will be empty and will not be added to the source or target neuron groups' respective
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
        setLabel(parentNetwork.getIdManager().getProposedId(this.getClass()));
    }

    /**
     * The "build" method which actually constructs the group in terms of populating it with synapses. If called on a
     * synapse group which has already been made (i.e. already has synapses populating it) those synapses will be
     * destroyed and the current connection manager and parameters used to create new connections. This method adds the
     * current synapse group the the source neuron group's outgoing synapse set and the target neuron group's incoming
     * synapse set.
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
     * Copy this synapse group onto another neurongroup source/target pair.
     */
    public SynapseGroup copy(NeuronGroup src, NeuronGroup tar) {
        if ((sourceNeuronGroup.size() != src.size()) || (targetNeuronGroup.size() != tar.size())) {
            throw new IllegalArgumentException("Size of source and target neuron groups of this synapse group do not match size " + "of source and target neuron groups of this synapse group.");
        }
        SynapseGroup copy = new SynapseGroup(src, tar, connectionManager);
        copy.exSynapseSet = new SynapseSet(this, 0);
        copy.inSynapseSet = new SynapseSet(this, 0);
        var mapping = new HashMap<Neuron, Neuron>();
        for (int i = 0; i < src.size(); i++) {
            mapping.put(sourceNeuronGroup.getNeuron(i), src.getNeuron(i));
        }
        for (int i = 0; i < tar.size(); i++) {
            mapping.put(targetNeuronGroup.getNeuron(i), tar.getNeuron(i));
        }
        var newSynsExc = exSynapseSet.stream()
                .map(synapse ->
                        new Synapse(
                                parentNetwork,
                                mapping.get(synapse.getSource()),
                                mapping.get(synapse.getTarget()),
                                synapse
                        )
                ).collect(Collectors.toList());
        copy.exSynapseSet.addAll(newSynsExc);
        var newInhSyns = inSynapseSet.stream()
                .map(synapse ->
                        new Synapse(
                                parentNetwork,
                                mapping.get(synapse.getSource()),
                                mapping.get(synapse.getTarget()),
                                synapse
                        )
                ).collect(Collectors.toList());
        copy.inSynapseSet.addAll(newInhSyns);
        return copy;
    }

    /**
     * Pre-allocates, that is sets the initial capacity of the arraylist containing this synapse group's synapses. This
     * allows expectedNumber of synapses to be added to this synapse group without the synapse list having to perform
     * any operations related to expanding the list size.
     * <p>
     * Sets initial capacity of hashsets, so that when you add new synapses you reduce the chances of it rehashing,
     * resizing etc when creating a large synapse group.
     *
     * @param expectedNumSynapses the number of synapses the connection manager predicts will be created.
     * @throws IllegalStateException if the synapse group has already been initialized.
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
     * If an algorithm (like training) extensively changes the polarity of the synapses in this group, it's impractical
     * to check every time a change is made. Thus after the bulk of the algorithm is completed this method can be called
     * to sort synapses into their appropriate sets.
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
     * Update group. Override for special updating. Recommended that overrides call super.update() some time during the
     * custom update.
     */
    public void update() {

        if (!(exLearningRule instanceof StaticSynapseRule)) {
            exSynapseSet.forEach(s -> {
                exLearningRule.apply(s, s.getDataHolder());
            });
        }
        if (!(inLearningRule instanceof StaticSynapseRule)) {
            inSynapseSet.forEach(s -> {
                inLearningRule.apply(s, s.getDataHolder());
            });
        }

    }

    @Override
    public void updateInputs() {
        if (!(exSpikeResponder instanceof NonResponder)) {
            exSynapseSet.forEach(exSpikeResponder::apply);
        }
        if (!(inSpikeResponder instanceof NonResponder)) {
            inSynapseSet.forEach(inSpikeResponder::apply);
        }
    }

    public int size() {
        return exSynapseSet.size() + inSynapseSet.size();
    }

    public boolean isEmpty() {
        return exSynapseSet.isEmpty() && inSynapseSet.isEmpty();
    }

    public void delete() {
        clear();
        exSynapseSet.forEach(Synapse::delete);
        inSynapseSet.forEach(Synapse::delete);
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
     * Determine whether this synpase group should initially have its synapses displayed. For isolated synapse groups
     * check its number of synapses. If the maximum number of possible connections exceeds a the network's synapse
     * visibility threshold, then individual synapses will not be displayed.
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

    @Override
    public void increment() {
        exSynapseSet.forEach(Synapse::increment);
        inSynapseSet.forEach(Synapse::increment);
    }

    @Override
    public void decrement() {
        exSynapseSet.forEach(Synapse::decrement);
        inSynapseSet.forEach(Synapse::decrement);
    }

    // TODO: Checks.
    public void addExcitatorySynapse(final Synapse s) {
        exSynapseSet.add(s);
    }

    public void addInhibitorySynapse(final Synapse s) {
        inSynapseSet.add(s);
    }

    /**
     * Adds a new synapse (one which is "blank") to the synapse group. This is the <b>preferred</b> method to use for
     * adding synapses to the synapse group over addSynapseUnsafe(Synapse) because it makes the added synapse conform to
     * the global parameters of this synapse group.
     *
     * @param synapse the blank synapse to be added and assigned new values based on the parameters of this group.
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
     * @param synapse the blank excitatory synapse which will be added to the group and have its parameters set based on
     *                the parameters of this group.
     */
    private void addNewExcitatorySynapse(final Synapse synapse) {
        synapse.setParentGroup(this);
        if (exciteRand != null) {
            synapse.setStrength(exciteRand.sampleDouble());
        } else {
            synapse.setStrength(DEFAULT_EXCITATORY_STRENGTH);
        }
        // TODO
        // synapse.setLearningRule(excitatoryPrototype.getLearningRule().deepCopy());
        // synapse.setFrozen(excitatoryPrototype.isFrozen());
        // synapse.setEnabled(excitatoryPrototype.isEnabled());
        // synapse.setDelay(excitatoryPrototype.getDelay());
        // synapse.setIncrement(excitatoryPrototype.getIncrement());
        // synapse.setUpperBound(excitatoryPrototype.getUpperBound());
        // synapse.setLowerBound(excitatoryPrototype.getLowerBound());
        // synapse.setSpikeResponder(excitatoryPrototype.getSpikeResponder());
        exSynapseSet.add(synapse);
        fireSynapseAdded(synapse);
    }

    /**
     * @param synapse the blank inhibitory synapse which will be added to the group and have its parameters set based on
     *                the parameters of this group.
     */
    private void addNewInhibitorySynapse(final Synapse synapse) {
        synapse.setParentGroup(this);
        if (inhibRand != null) {
            synapse.setStrength(inhibRand.sampleDouble());
        } else {
            synapse.setStrength(DEFAULT_INHIBITORY_STRENGTH);
        }
        // TODO
        // synapse.setLearningRule(inhibitoryPrototype.getLearningRule().deepCopy());
        // synapse.setFrozen(inhibitoryPrototype.isFrozen());
        // synapse.setEnabled(inhibitoryPrototype.isEnabled());
        // synapse.setDelay(inhibitoryPrototype.getDelay());
        // synapse.setIncrement(inhibitoryPrototype.getIncrement());
        // synapse.setUpperBound(inhibitoryPrototype.getUpperBound());
        // synapse.setLowerBound(inhibitoryPrototype.getLowerBound());
        // synapse.setSpikeResponder(inhibitoryPrototype.getSpikeResponder());
        inSynapseSet.add(synapse);
        fireSynapseAdded(synapse);
    }

    /**
     * Changes the ratio of synapses in this group that are excitatory subject to two constraints: <b>1)</b> If neurons
     * in the source neuron group have their own polarity ratio, the desired excitatoryRatio may not be possible. In
     * this case, this class will <b>NOT</b> add or remove synapses to achieve this number. If not all source neurons
     * are polarized, however this class will attempt to get as close as possible to the desired excitatoryRatio.
     * <b>2)</b> Changes in weights performed to achieve the desired excitatoryRatio will result from sign changes to
     * synapses. The absolute value of synapse strengths will not be changed.
     *
     * @param excitatoryRatio the ratio of synapses which will be made excitatory, value must be in the range [0, 1]
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
                        s.setStrength(inhibRand.sampleDouble());
                    } else {
                        s.setStrength(DEFAULT_INHIBITORY_STRENGTH);
                    }
                    // TODO
                    // s.setLearningRule(inhibitoryPrototype.getLearningRule().deepCopy());
                    // s.setFrozen(inhibitoryPrototype.isFrozen());
                    // s.setEnabled(inhibitoryPrototype.isEnabled());
                    // s.setDelay(inhibitoryPrototype.getDelay());
                    // s.setIncrement(inhibitoryPrototype.getIncrement());
                    // s.setUpperBound(inhibitoryPrototype.getUpperBound());
                    // s.setLowerBound(inhibitoryPrototype.getLowerBound());
                    // s.setSpikeResponder(inhibitoryPrototype.getSpikeResponder());
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
                        s.setStrength(exciteRand.sampleDouble());
                    } else {
                        s.setStrength(DEFAULT_EXCITATORY_STRENGTH);
                    }
                    // TODO
                    // s.setLearningRule(excitatoryPrototype.getLearningRule().deepCopy());
                    // s.setFrozen(excitatoryPrototype.isFrozen());
                    // s.setEnabled(excitatoryPrototype.isEnabled());
                    // s.setDelay(excitatoryPrototype.getDelay());
                    // s.setIncrement(excitatoryPrototype.getIncrement());
                    // s.setUpperBound(excitatoryPrototype.getUpperBound());
                    // s.setLowerBound(excitatoryPrototype.getLowerBound());
                    // s.setSpikeResponder(excitatoryPrototype.getSpikeResponder());
                    exSynapseSet.add(s);
                    numSwitch--;
                }
            }
        }
        this.excitatoryRatio = excitatoryRatio;
    }

    /**
     * Returns the excitatory ratio <b>parameter</b>. For the <i>actual</i> value use {@link
     * #getExcitatoryRatioPrecise()}.
     *
     * @return the ration of excitatory synapses in this group
     */
    public double getExcitatoryRatioParameter() {
        return excitatoryRatio;
    }

    /**
     * @return the <b>actual</b> excitatory ratio measured as the number of excitatory synapses divided by the total.
     */
    public double getExcitatoryRatioPrecise() {
        return exSynapseSet.size() / (double) size();
    }

    /**
     * @return a flat list representation of all the synapses in this synapse group. This list is a defensive copy.
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
     * Randomizes all the synapses according to their corresponding randomizers. {@link
     * #randomizeExcitatoryConnections()}, {@link #randomizeInhibitoryConnections()}
     */
    @Override
    public void randomize() {
        randomizeExcitatoryConnections();
        randomizeInhibitoryConnections();
    }

    /**
     * Randomizes the weights of the excitatory connections in this group based on the parameters of {@link
     * #exciteRand}. Assumes that all synapses in {@link #exSynapseSet} are--in fact--excitatory. If some action on the
     * synapses may have corrupted that assumption call {@link #revalidateSynapseSets()} first.
     */
    public void randomizeExcitatoryConnections() {
        randomizeExcitatorySynapsesUnsafe(exSynapseSet, exciteRand);
    }

    /**
     * Randomizes the weights of the inhibitory connections in this group based on the parameters of {@link #inhibRand}.
     * Assumes that all synapses in {@link #inSynapseSet} are--in fact--inhibitory. If some action on the synapses may
     * have corrupted that assumption call {@link #revalidateSynapseSets()} first.
     */
    public void randomizeInhibitoryConnections() {
        randomizeInhibitorySynapsesUnsafe(inSynapseSet, inhibRand);
    }

    /**
     * Sets the connection manager for this synapse group.
     *
     * @param connection the connection manager to be used by this synapse group for making synaptic connections.
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
     * @param excitatoryRandomizer the randomizer to be used to determine the weights of excitatory synapses.
     */
    public void setExcitatoryRandomizer(ProbabilityDistribution excitatoryRandomizer) {
        this.exciteRand = excitatoryRandomizer == null ? null : excitatoryRandomizer.deepCopy();
    }

    /**
     * @param inhibitoryRandomizer the randomizer to be used to determine the weights of inbihitory synapses.
     */
    public void setInhibitoryRandomizer(ProbabilityDistribution inhibitoryRandomizer) {
        inhibRand = inhibitoryRandomizer == null ? null : inhibitoryRandomizer.deepCopy();
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
     * If a randomize operation changes the ratio of excitatory to inhibitory synapses, this method can be called to
     * change the excitatoryRatio to reflect this value. This happens rarely, and generally speaking is not recommended
     * outside prototyping.
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
     * Tests if this synapse group is in fact recurrent (it's target and source neuron groups are the same).
     *
     * @return if this synapse group's source neuron group and target neuron group are the same group.
     */
    private boolean testRecurrent() {
        return sourceNeuronGroup == targetNeuronGroup;
    }

    /**
     * Return a list of source neurons associated with the synapses in this group.
     *
     * @return the source neuron list.
     */
    public List<Neuron> getSourceNeurons() {
        return getSourceNeuronGroup().getNeuronList();
    }

    /**
     * Return a list of target neurons associated with the synapses in this group.
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

    @Override
    public void postOpenInit() {
        if (events == null) {
            events = new SynapseGroupEvents(this);
        }
        exSynapseSet.forEach(Synapse::postOpenInit);
        inSynapseSet.forEach(Synapse::postOpenInit);
        revalidateSynapseSets();
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

    public SynapseUpdateRule getExLearningRule() {
        return exLearningRule;
    }

    public void setExLearningRule(SynapseUpdateRule exLearningRule) {
        this.exLearningRule = exLearningRule;
    }

    public SpikeResponder getExSpikeResponder() {
        return exSpikeResponder;
    }

    public void setExSpikeResponder(SpikeResponder exSpikeResponder) {
        this.exSpikeResponder = exSpikeResponder;
    }

    public SynapseUpdateRule getInLearningRule() {
        return inLearningRule;
    }

    public void setInLearningRule(SynapseUpdateRule inLearningRule) {
        this.inLearningRule = inLearningRule;
    }

    public SpikeResponder getInSpikeResponder() {
        return inSpikeResponder;
    }

    public void setInSpikeResponder(SpikeResponder inSpikeResponder) {
        this.inSpikeResponder = inSpikeResponder;
    }
}