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

    private static final PolarizedRandomizer DEFAULT_EX_RANDOMIZER =
        new PolarizedRandomizer(
            Polarity.EXCITATORY);

    private static final PolarizedRandomizer DEFAULT_IN_RANDOMIZER =
        new PolarizedRandomizer(
            Polarity.INHIBITORY);

    public static final double DEFAULT_EXCITATORY_RATIO = 1.0;

    public static final ConnectNeurons DEFAULT_CONNECTION_MANAGER =
        new AllToAll();

    private Set<Synapse> exSynapseSet = new HashSet<Synapse>();

    private Set<Synapse> inSynapseSet = new HashSet<Synapse>();

    /** Reference to source neuron group. */
    private final NeuronGroup sourceNeuronGroup;

    /** Reference to target neuron group. */
    private final NeuronGroup targetNeuronGroup;

    private ConnectNeurons connectionManager;

    private double excitatoryRatio = DEFAULT_EXCITATORY_RATIO;

    private Synapse excitatoryPrototype = Synapse.getTemplateSynapse();

    private Synapse inhibitoryPrototype = Synapse.getTemplateSynapse();

    private PolarizedRandomizer exciteRand;

    private PolarizedRandomizer inhibRand;

    /**
     * Flag for whether synapses should be displayed in a GUI representation of
     * this object.
     */
    private boolean displaySynapses;

    boolean recurrent;

    /**
     *
     * @param source
     * @param target
     * @return
     */
    public static SynapseGroup createSynapseGroup(final NeuronGroup source,
        final NeuronGroup target) {
        return createSynapseGroup(source, target, DEFAULT_CONNECTION_MANAGER);
    }

    /**
     *
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
     *
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
     *
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
     *
     * @param source
     * @param target
     */
    public SynapseGroup(final NeuronGroup source, final NeuronGroup target) {
        super(source.getParentNetwork());
        this.sourceNeuronGroup = source;
        this.targetNeuronGroup = target;
        source.addOutgoingSg(this);
        target.addIncomingSg(this);
        recurrent = testRecurrent();
        initializeSynapseVisibility();
    }

    /**
     * Create a new synapse group.
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
        source.addOutgoingSg(this);
        target.addIncomingSg(this);
        recurrent = testRecurrent();
        initializeSynapseVisibility();
    }

    /**
     *
     */
    public void makeConnections() {
        clear();
        connectionManager.connectNeurons(this);
    }

    /**
     *
     */
    public synchronized void randomizeConnections() {
        randomizeExcitatoryConnections();
        randomizeInhibitoryConnections();
    }

    /**
     *
     */
    public synchronized void randomizeExcitatoryConnections() {
        ConnectionUtilities.randomizeExcitatorySynapsesUnsafe(exSynapseSet,
            exciteRand);
    }

    /**
     *
     */
    public synchronized void randomizeInhibitoryConnections() {
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
    public synchronized void setExcitatoryRatio(double excitatoryRatio)
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
     * Returns the excitatory ratio.
     *
     * @return the ration of excitatory synapses in this group
     */
    public double getExcitatoryRatio() {
        return excitatoryRatio;
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
    }

    /**
     * Add a synapse to this synapse group.
     *
     * @param synapse
     *            synapse to add
     */
    public void addSynapse(final Synapse synapse) {
        if (synapse.getStrength() > 0) {
            addExcitatorySynapse(synapse);
        }
        if (synapse.getStrength() < 0) {
            addInhibitorySynapse(synapse);
        }
    }

    /**
     *
     * @param synapse
     */
    public void addExcitatorySynapse(final Synapse synapse) {
        exSynapseSet.add(synapse);
        excitatoryRatio = exSynapseSet.size() / (double) size();
        if (getParentNetwork() != null) {
            synapse.setId(getParentNetwork().getSynapseIdGenerator().getId());
            synapse.setParentGroup(this);
        }
    }

    /**
     *
     * @param synapse
     */
    public void addInhibitorySynapse(final Synapse synapse) {
        inSynapseSet.add(synapse);
        excitatoryRatio = exSynapseSet.size() / (double) size();
        if (getParentNetwork() != null) {
            synapse.setId(getParentNetwork().getSynapseIdGenerator().getId());
            synapse.setParentGroup(this);
        }
    }

    /**
     *
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
            if (rand < excitatoryRatio) {
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
     * Remove the provided synapse.
     *
     * @param toDelete
     *            the synapse to delete
     */
    public void removeSynapse(Synapse toDelete) {
        if (toDelete.getStrength() > 0) {
            exSynapseSet.remove(toDelete);
        } else {
            inSynapseSet.remove(toDelete);
        }
        getParentNetwork().fireSynapseRemoved(toDelete);
        getParentNetwork().fireGroupChanged(this, this, "synapseRemoved");
        if (isEmpty()) {
            delete();
        }
    }

    /**
     *
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

    /** {@inheritDoc} */
    public int size() {
        return exSynapseSet.size() + inSynapseSet.size();
    }

    /**
     * Update group. Override for special updating.
     */
    public void update() {
        updateAllSynapses();
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
     * @return the set of excitatory synapses
     */
    public synchronized Set<Synapse> getExcitatorySynapses() {
        return Collections.unmodifiableSet(exSynapseSet);
    }

    /**
     * @return the set of inhibitory synapses
     */
    public synchronized Set<Synapse> getInhibitorySynapses() {
        return Collections.unmodifiableSet(inSynapseSet);
    }

    /**
     * @return a flat list representation of all the synapses in this synapse
     *         group.
     */
    public synchronized List<Synapse> getAllSynapses() {
        ArrayList<Synapse> flatList = new ArrayList<Synapse>(size());
        flatList.addAll(exSynapseSet);
        flatList.addAll(inSynapseSet);
        return Collections.unmodifiableList(flatList);
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
     * If an algorithm (like training) extensively changes the polarity of the
     * synapses in this group, it's impractical to check every time a change is
     * made. Thus after the bulk of the algorithm is completed this method can
     * be called to sort synapses into their appropriate sets.
     */
    public void checkAndFixInconsistencies() {
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
        return sourceNeuronGroup == targetNeuronGroup ? true : false;
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

    /**
     * Update all synapses.
     */
    public void updateAllSynapses() {
        for (Synapse synapse : exSynapseSet) {
            synapse.update();
        }
        for (Synapse synapse : inSynapseSet) {
            synapse.update();
        }
    }

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

    @Override
    public boolean isEmpty() {
        return exSynapseSet.isEmpty() && inSynapseSet.isEmpty();
    }

    public boolean hasExcitatory() {
        return !exSynapseSet.isEmpty();
    }

    public boolean hasInhibitory() {
        return !inSynapseSet.isEmpty();
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
     * values to be set for any synapses that might be added to the group.
     *
     * @return
     */
    public Synapse getExcitatoryPrototype() {
        return excitatoryPrototype;
    }

    /**
     * Reveals the inhibitory prototype synapse, allowing more detailed synapse
     * values to be set for any synapses that might be added to the group.
     *
     * @return
     */
    public Synapse getInhibitoryPrototype() {
        return inhibitoryPrototype;
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
    public synchronized double[] getInhibitoryStrengths() {
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
    public synchronized double[] getExcitatoryStrengths() {
        double[] retArray = new double[exSynapseSet.size()];
        int i = 0;
        for (Synapse synapse : exSynapseSet) {
            retArray[i++] = synapse.getStrength();
        }
        return retArray;
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
        return isAllExcitatoryFrozen() && isAllInhibitoryFrozen();
    }

    /**
     * @return if the excitatory synapses are frozen
     */
    public boolean isAllExcitatoryFrozen() {
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
     * @return if the inhibitory synapses are frozen
     */
    public boolean isAllInhibitoryFrozen() {
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
        return isAllExcitatoryEnabled() && isAllInhibitoryEnabled();
    }

    /**
     * @return
     */
    public boolean isAllExcitatoryEnabled() {
        Iterator<Synapse> exIter = exSynapseSet.iterator();
        if (exIter.hasNext()) {
            Synapse s = exIter.next();
            if (!s.isEnabled()) {
                return false; // Not _all_ the excitatory synapses are enabled
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
     * @return
     */
    public boolean isAllInhibitoryEnabled() {
        Iterator<Synapse> inIter = inSynapseSet.iterator();
        if (inIter.hasNext()) {
            Synapse s = inIter.next();
            if (!s.isEnabled()) {
                return false; // Not _all_ the inhibitory synapses are enabled
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
     * A quick method for testing if the inhibitory synapses of this group are
     * frozen... If the synapses have not been edited by methods outside this
     * group, then this method will reliably report if the inhibitory synapses
     * of this group are all frozen or not in O(1) vs. O(n) time for
     * {@link #isAllInhibitoryFrozen()}.
     *
     * @return
     */
    public boolean isInhibitoryFrozenQuick() {
        return inhibitoryPrototype.isFrozen();
    }

    /**
     * A quick method for testing if the Excitatory synapses of this group are
     * frozen... If the synapses have not been edited by methods outside this
     * group, then this method will reliably report if the Excitatory synapses
     * of this group are all frozen or not in O(1) vs. O(n) time for
     * {@link #isAllExcitatoryFrozen()}.
     *
     * @return
     */
    public boolean isExcitatoryFrozenQuick() {
        return excitatoryPrototype.isFrozen();
    }

    /**
     * A quick method for testing if the inhibitory synapses of this group are
     * enabled... If the synapses have not been edited by methods outside this
     * group, then this method will reliably report if the inhibitory synapses
     * of this group are all enabled or not in O(1) vs. O(n) time for
     * {@link #isAllInhibitoryEnabled()}.
     *
     * @return
     */
    public boolean isInhibitoryEnabledQuick() {
        return inhibitoryPrototype.isEnabled();
    }

    /**
     * A quick method for testing if the excitatory synapses of this group are
     * enabled... If the synapses have not been edited by methods outside this
     * group, then this method will reliably report if the excitatory synapses
     * of this group are all enabled or not in O(1) vs. O(n) time for
     * {@link #isAllExcitatoryEnabled()}.
     *
     * @return
     */
    public boolean isExcitatoryEnabledQuick() {
        return excitatoryPrototype.isEnabled();
    }

    @Override
    public String getUpdateMethodDesecription() {
        return "Update synapses";
    }

    /**
     * @return the displaySynapses
     */
    public boolean isDisplaySynapses() {
        return displaySynapses;
    }

    /**
     * @param displaySynapses
     *            the displaySynapses to set
     */
    public void setDisplaySynapses(boolean displaySynapses) {
        this.displaySynapses = displaySynapses;
    }

    /**
     * @return the excitatory update rule
     */
    public SynapseUpdateRule getExcitatoryRule() {
        return excitatoryPrototype.getLearningRule();
    }

    /**
     * Sets the update rule of all excitatory synapses to the specified rule.
     *
     * @param excitatoryRule
     */
    public void setExcitatoryRule(SynapseUpdateRule excitatoryRule) {
        excitatoryPrototype.setLearningRule(excitatoryRule);
        for (Synapse s : exSynapseSet) {
            s.setLearningRule(excitatoryRule.deepCopy());
        }
    }

    /**
     * @return the inhibitory update rule
     */
    public SynapseUpdateRule getInhibitoryRule() {
        return inhibitoryPrototype.getLearningRule();
    }

    /**
     * Sets the update rule of all the inhibitory synapses to the specified
     * rule.
     *
     * @param inhibitoryRule
     */
    public void setInhibitoryRule(SynapseUpdateRule inhibitoryRule) {
        inhibitoryPrototype.setLearningRule(inhibitoryRule);
        for (Synapse s : inSynapseSet) {
            s.setLearningRule(inhibitoryRule.deepCopy());
        }
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
}
