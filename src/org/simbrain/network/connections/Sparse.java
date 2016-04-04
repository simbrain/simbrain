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
package org.simbrain.network.connections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.util.math.SimbrainMath;

import umontreal.iro.lecuyer.randvar.BinomialGen;

/**
 * A superclass for all connectors whose primary parameter is related to base
 * connection density, taking no other major factors into account insofar as
 * selecting which neurons should be connected goes.
 *
 * @author Zach Tosi
 */
public class Sparse implements ConnectNeurons {

    /**
     * The default preference as to whether or not self connections are allowed.
     */
    public static boolean DEFAULT_SELF_CONNECT_PREF;

    /**
     * Sets the default behavior concerning whether or not the number of
     * efferents of each source neurons should be equalized.
     */
    public static boolean DEFAULT_FF_PREF;

    /** The default sparsity (between 0 and 1). */
    public static double DEFAULT_CONNECTION_DENSITY = 0.1;

    /**
     * Whether or not each source neuron is given an equal number of efferent
     * synapses. If true, every source neuron will have exactly the same number
     * of synapses emanating from them, that is, each source will connect to the
     * same number of targets. If you have 10 source neurons and 10 target
     * neurons, and 50% sparsity, then each source neuron will connect to
     * exactly 5 targets. If equalizeEfferents is false, then the number of
     * target neurons each source neuron connects to will be drawn from a
     * binomial distribution with a %success chance of 50%, so on average each
     * source neuron will connect to 5 targets, and the sparsity will be roughly
     * 50% (more exact the more neurons/synapses there are). However the number
     * of targets any given source neuron connects to is by no means guaranteed.
     */
    private boolean equalizeEfferents = DEFAULT_FF_PREF;

    /**
     * A tag for whether or not this sparse connector supports density editing
     * (changing the number of connecitions) after construction.
     */
    private boolean permitDensityEditing = true;
    
    /**
     * A map of permutations governing in what order connections to target
     * neurons will be added if the connection density is raised for each source
     * neuron. Maps which index of target neuron will be the next to be given a
     * connection, or in what order connections are removed for each source
     * neuron if density is lowered.
     */
    private int[][] sparseOrdering;

    /**
     * If efferent synapses are not equalized among source neurons, this array
     * contains the number of possible target neurons a given source neuron is
     * connected to.
     */
    private int[] currentOrderingIndices;

    /** The source neurons. */
    private Neuron[] sourceNeurons;

    /** The target neurons. */
    private Neuron[] targetNeurons;

    /** The synapse group associated with this connection object. */
    private SynapseGroup synapseGroup;

    /**
     * Generally speaking the connectionDensity parameter represents a
     * probability reflecting how many possible connections between a given
     * source neuron and all available target neurons will actually be made.
     */
    protected double connectionDensity;

    /**
     * Whether or not connections where the source and target are the same
     * neuron are allowed. Only applicable if the source and target neuron sets
     * are the same.
     */
    protected boolean selfConnectionAllowed = DEFAULT_SELF_CONNECT_PREF;

    /**
     * Default constructor.
     */
    public Sparse() {
        this.connectionDensity = DEFAULT_CONNECTION_DENSITY;
    }

    /**
     * Construct a sparse object from main arguments. Used in scripts.
     *
     * @param sparsity sparsity level
     * @param equalizeEfferents whether to equalize efferents
     * @param selfConnectionAllowed whether self-connections should be allowed.
     */
    public Sparse(double sparsity, boolean equalizeEfferents,
        boolean selfConnectionAllowed) {
        this.connectionDensity = sparsity;
        this.equalizeEfferents = equalizeEfferents;
        this.selfConnectionAllowed = selfConnectionAllowed;
    }

    /**
     * Connect source to target neurons using this instance of the sparse
     * object's properties to set all parameters of the connections.
     *
     * @param sourceNeurons the source neurons
     * @param targetNeurons the target neurons
     * @return the newly creates synapses connecting source to target
     */
    public List<Synapse> connectSparse(List<Neuron> sourceNeurons,
        List<Neuron> targetNeurons) {
        return connectSparse(sourceNeurons, targetNeurons, connectionDensity,
            selfConnectionAllowed, equalizeEfferents, true);
    }

    /**
     * Connects two lists of neurons with synapses assigning connections between
     * source and target neurons randomly in such a way that results in
     * "sparsity" percentage of possible connections being created.
     *
     * @param sourceNeurons source neurons
     * @param targetNeurons target neurons
     * @param sparsity sparsity of connection
     * @param selfConnectionAllowed whether to allow self-connections
     * @param equalizeEfferents whether or not the number of efferents of each
     *            source neurons should be equalized.
     * @param looseSynapses are these loose synapses
     * @return the new synapses
     */
    public static List<Synapse> connectSparse(List<Neuron> sourceNeurons,
        List<Neuron> targetNeurons, double sparsity,
        boolean selfConnectionAllowed, boolean equalizeEfferents,
        boolean looseSynapses) {
        boolean recurrent = ConnectionUtilities.testRecurrence(sourceNeurons,
            targetNeurons);
        Neuron source;
        Neuron target;
        Synapse synapse;
        ArrayList<Synapse> syns = new ArrayList<Synapse>();
        Random rand = new Random(System.nanoTime());
        if (equalizeEfferents) {
            ArrayList<Integer> targetList = new ArrayList<Integer>();
            ArrayList<Integer> tListCopy;
            for (int i = 0; i < targetNeurons.size(); i++) {
                targetList.add(i);
            }
            int numSyns;
            if (!selfConnectionAllowed && sourceNeurons == targetNeurons) {
                numSyns =
                    (int) (sparsity * sourceNeurons.size() * (targetNeurons
                        .size() - 1));
            } else {
                numSyns =
                    (int) (sparsity * sourceNeurons.size() * targetNeurons
                        .size());
            }
            int synsPerSource = numSyns / sourceNeurons.size();
            int targStart = 0;
            int targEnd = synsPerSource;
            if (synsPerSource > numSyns / 2) {
                synsPerSource = numSyns - synsPerSource;
                targStart = synsPerSource;
                targEnd = targetList.size();
            }

            for (int i = 0; i < sourceNeurons.size(); i++) {
                source = sourceNeurons.get(i);
                if (!selfConnectionAllowed && recurrent) {
                    tListCopy = new ArrayList<Integer>();
                    for (int k = 0; k < targetList.size(); k++) {
                        if (k == i) { // Exclude oneself as a possible target
                            continue;
                        }
                        tListCopy.add(targetList.get(k));
                    }
                    randShuffleK(tListCopy, synsPerSource, rand);
                } else {
                    randShuffleK(targetList, synsPerSource, rand);
                    tListCopy = targetList;
                }

                for (int j = targStart; j < targEnd; j++) {
                    target = targetNeurons.get(tListCopy.get(j));
                    synapse = new Synapse(source, target);
                    if (looseSynapses) {
                        source.getNetwork().addSynapse(synapse);
                    }
                    syns.add(synapse);
                }
            }
        } else {
            for (int i = 0; i < sourceNeurons.size(); i++) {
                for (int j = 0; j < targetNeurons.size(); j++) {
                    if (!selfConnectionAllowed && recurrent && i == j) {
                        continue;
                    } else {
                        if (Math.random() < sparsity) {
                            source = sourceNeurons.get(i);
                            target = targetNeurons.get(j);
                            synapse = new Synapse(source, target);
                            if (looseSynapses) {
                                source.getNetwork().addSynapse(synapse);
                            }
                            syns.add(synapse);
                        }
                    }
                }

            }
        }
        return syns;

    }

    /**
     * @param synapseGroup The synapse group that the connections this class
     * will generate will be added to.
     */
    public void connectNeurons(SynapseGroup synapseGroup) {
        this.synapseGroup = synapseGroup;
        boolean recurrent = synapseGroup.isRecurrent();
        int numSrc = synapseGroup.getSourceNeurons().size();
        int numTar = synapseGroup.getTargetNeurons().size();
        setPermitDensityEditing(numSrc * numTar < 10E8);
        sourceNeurons = synapseGroup.getSourceNeurons().toArray(
            new Neuron[numSrc]);
        targetNeurons = recurrent ? sourceNeurons : synapseGroup
            .getTargetNeurons().toArray(new Neuron[numTar]);
        if (isPermitDensityEditing()) {
            generateSparseOrdering(recurrent);
            if (equalizeEfferents) {
                connectEqualized(synapseGroup);
            } else {
                connectRandom(synapseGroup);
            }
        } else {
            List<Synapse> syns = this.connectSparse(synapseGroup
                    .getSourceNeurons(), synapseGroup.getTargetNeurons());
            for (Synapse s : syns) {
                synapseGroup.addNewSynapse(s);
            }
        }

    }

    /**
     * Populates the synapse group with synapses by making individual synaptic
     * connections between the neurons in the synapse group's source and target
     * groups. These synapses are initialized with default attributes and zero
     * strength. Each source neuron will have exactly the same number of
     * efferent synapses. This number being whichever satisfies the constraints
     * given by the sparsity and whether or not the synapse group is recurrent
     * and self connections are allowed.
     *
     * @param synapseGroup
     */
    private void connectEqualized(SynapseGroup synapseGroup) {
        currentOrderingIndices = new int[sourceNeurons.length];
        int numConnectsPerSrc;
        int expectedNumSyns;
        if (synapseGroup.isRecurrent() && !selfConnectionAllowed) {
            numConnectsPerSrc =
                (int) (connectionDensity * (sourceNeurons.length - 1));
        } else {
            numConnectsPerSrc =
                (int) (connectionDensity * targetNeurons.length);
        }
        expectedNumSyns = numConnectsPerSrc * sourceNeurons.length;
        synapseGroup.preAllocateSynapses(expectedNumSyns);
        for (int i = 0, n = sourceNeurons.length; i < n; i++) {
            currentOrderingIndices[i] = numConnectsPerSrc;
            Neuron src = sourceNeurons[i];
            Neuron tar;
            for (int j = 0; j < numConnectsPerSrc; j++) {
                tar = targetNeurons[sparseOrdering[i][j]];
                Synapse s = new Synapse(src, tar);
                synapseGroup.addNewSynapse(s);
            }
        }
    }

    /**
     * Populates the synapse group with synapses by making individual synaptic
     * connections between the neurons in the synapse group's source and target
     * groups. These synapses are initialized with default attributes and zero
     * strength. The number of efferent synapses assigned to each source neuron
     * is drawn from a binomial distribution with a mean of
     * NumberOfTargetNeurons * sparsity
     *
     * @param synapseGroup
     */
    private void connectRandom(SynapseGroup synapseGroup) {
        currentOrderingIndices = new int[sourceNeurons.length];
        int numTars =
            synapseGroup.isRecurrent() && !selfConnectionAllowed
                ? (sourceNeurons.length - 1)
                : targetNeurons.length;
        synapseGroup
            .preAllocateSynapses((int) (sourceNeurons.length * numTars * connectionDensity));
        for (int i = 0, n = sourceNeurons.length; i < n; i++) {
            currentOrderingIndices[i] = BinomialGen.nextInt(
                SimbrainMath.DEFAULT_RANDOM_STREAM, numTars,
                connectionDensity);
            Neuron src = sourceNeurons[i];
            Neuron tar;
            int tarLen = targetNeurons.length - 1;
            int [] o = null;
            if (sourceNeurons == targetNeurons && !selfConnectionAllowed) {
	            o = SimbrainMath.randPermuteWithExclusion(0,
	                    tarLen + 1, i);
            } else {
	            o = SimbrainMath.randPermute(0, tarLen + 1);
            }
            for (int j = 0; j < currentOrderingIndices[i]; j++) {
                tar = targetNeurons[o[j]];
                Synapse s = new Synapse(src, tar);
                synapseGroup.addNewSynapse(s);
            }
        }

    }

    /**
     *
     * @param recurrent
     */
    private void generateSparseOrdering(boolean recurrent) {
        int srcLen = sourceNeurons.length;
        if (recurrent && !selfConnectionAllowed) {
            int tarLen = targetNeurons.length - 1;
            sparseOrdering = new int[sourceNeurons.length][tarLen];
            for (int i = 0; i < srcLen; i++) {
                sparseOrdering[i] = SimbrainMath.randPermuteWithExclusion(0,
                    tarLen + 1, i);
            }
        } else {
            int tarLen = targetNeurons.length;
            sparseOrdering = new int[sourceNeurons.length][tarLen];
            for (int i = 0; i < srcLen; i++) {
                sparseOrdering[i] = SimbrainMath.randPermute(0, tarLen);
            }
        }
    }

    /**
     * Randomly shuffles k integers in a list. The first k elements are randomly
     * swapped with other elements in the list. This method will alter the list
     * passed to it, so situations where this would be undesirable should pass
     * this method a copy.
     *
     * @param inds a list of integers. This methods WILL shuffle inds, so pass a
     *            copy unless inds being shuffled is not a problem.
     * @param k how many elements will be shuffled
     * @param rand a random number generator
     */
    public static void
        randShuffleK(ArrayList<Integer> inds, int k, Random rand) {
        for (int i = 0; i < k; i++) {
            Collections.swap(inds, i, rand.nextInt(inds.size()));
        }
    }

    /**
     * @param newSparsity new sparsity connection
     */
    public void removeToSparsity(double newSparsity) {
        if (newSparsity >= connectionDensity) {
            throw new IllegalArgumentException("Cannot 'removeToSparsity' to"
                + " a higher connectivity density.");
        }
        Network net = sourceNeurons[0].getNetwork();
        int removeTotal =
            (synapseGroup.size() - (int) (newSparsity
            * getMaxPossibleConnections()));
        if (equalizeEfferents) {
            int curNumConPerSource = synapseGroup.size() / sourceNeurons.length;
            int removePerSource = removeTotal / sourceNeurons.length;
            int finalNumConPerSource = curNumConPerSource - removePerSource;
            for (int i = 0, n = sourceNeurons.length; i < n; i++) {
                for (int j = curNumConPerSource - 1; j >= finalNumConPerSource; j--) {
                    Synapse toRemove = Network.getSynapse(sourceNeurons[i],
                        targetNeurons[sparseOrdering[i][j]]);
                    net.removeSynapse(toRemove);
                }
                currentOrderingIndices[i] = finalNumConPerSource;
            }
        } else {
            for (int i = 0, n = sourceNeurons.length; i < n; i++) {
                int numToRemove = BinomialGen.nextInt(
                    SimbrainMath.DEFAULT_RANDOM_STREAM, synapseGroup
                        .getTargetNeuronGroup().size(), newSparsity);
                if (numToRemove < currentOrderingIndices[i]) {
                    List<Synapse> remove = decreaseDensity(i, numToRemove);
                    for (Synapse s : remove) {
                        synapseGroup.removeSynapse(s);
                    }
                } else {
                    List<Synapse> add = increaseDensity(i, numToRemove);
                    for (Synapse s : add) {
                        synapseGroup.addNewSynapse(s);
                    }
                }
                currentOrderingIndices[i] = numToRemove;
            }
        }
        this.connectionDensity = newSparsity;
    }

    /**
     * @param newSparsity new sparsity connection
     */
    public void addToSparsity(double newSparsity) {
        if (newSparsity <= connectionDensity) {
            throw new IllegalArgumentException("Cannot 'addToSparsity' to"
                + " a lower connectivity density.");
        }
        int addTotal =
            ((int) (newSparsity * getMaxPossibleConnections()) - synapseGroup
                .size());
        List<Synapse> addList = new ArrayList<Synapse>(addTotal);
        if (equalizeEfferents) {
            int curNumConPerSource = synapseGroup.size() / sourceNeurons.length;
            int addPerSource = addTotal / sourceNeurons.length;
            int finalNumConPerSource = curNumConPerSource + addPerSource;
            if (finalNumConPerSource > sparseOrdering[0].length) {
                finalNumConPerSource = sparseOrdering[0].length;
            }
            for (int i = 0, n = sourceNeurons.length; i < n; i++) {
                for (int j = curNumConPerSource; j < finalNumConPerSource; j++)
                {
                    Synapse toAdd = new Synapse(sourceNeurons[i],
                        targetNeurons[sparseOrdering[i][j]]);
                    addList.add(toAdd);
                }
                currentOrderingIndices[i] = finalNumConPerSource;
            }
        } else {
            for (int i = 0, n = sourceNeurons.length; i < n; i++) {
                int numToAdd = BinomialGen.nextInt(
                    SimbrainMath.DEFAULT_RANDOM_STREAM, synapseGroup
                        .getTargetNeuronGroup().size(), newSparsity);
                int finalNumConPerSource =
                    numToAdd >= currentOrderingIndices[i]
                        ? numToAdd : currentOrderingIndices[i];
                if (finalNumConPerSource > sparseOrdering[i].length) {
                    finalNumConPerSource = sparseOrdering[i].length;
                }
                if (finalNumConPerSource >= currentOrderingIndices[i]) {
                    addList.addAll(increaseDensity(i, finalNumConPerSource));
                } else {
                    List<Synapse> remove = decreaseDensity(i,
                        finalNumConPerSource);
                    for (Synapse s : remove) {
                        synapseGroup.removeSynapse(s);
                    }

                }
                currentOrderingIndices[i] = finalNumConPerSource;
            }
        }
        for (Synapse s : addList) {
            synapseGroup.addNewSynapse(s);
        }
        this.connectionDensity = newSparsity;
    }

    /**
     *
     * @param i
     * @param finalNumConnections
     * @return
     */
    private List<Synapse> increaseDensity(int i, int finalNumConnections) {
        List<Synapse> added = new ArrayList<Synapse>(finalNumConnections
            - currentOrderingIndices[i]);
        for (int j = currentOrderingIndices[i]; j < finalNumConnections; j++) {
            Synapse toAdd = new Synapse(sourceNeurons[i],
                targetNeurons[sparseOrdering[i][j]]);
            added.add(toAdd);
        }
        return added;
    }

    /**
     *
     * @param i
     * @param finalNumConnections
     * @return
     */
    private List<Synapse> decreaseDensity(int i, int finalNumConnections) {
        List<Synapse> removed =
            new ArrayList<Synapse>(currentOrderingIndices[i]
                - finalNumConnections);
        for (int j = currentOrderingIndices[i] - 1; j >= finalNumConnections; j--) {
            Synapse toRemove = Network.getSynapse(sourceNeurons[i],
                targetNeurons[sparseOrdering[i][j]]);
            removed.add(toRemove);
        }
        return removed;
    }

    public int getMaxPossibleConnections() {
        if (selfConnectionAllowed || !synapseGroup.isRecurrent()) {
            return sourceNeurons.length * targetNeurons.length;
        } else {
            return sourceNeurons.length * (sourceNeurons.length - 1);
        }
    }

    public boolean isEqualizeEfferents() {
        return equalizeEfferents;
    }

    public void setEqualizeEfferents(boolean equalizeEfferents) {
        this.equalizeEfferents = equalizeEfferents;
    }

    public boolean isPermitDensityEditing() {
		return permitDensityEditing;
	}

	public void setPermitDensityEditing(boolean permitDensityEditing) {
		this.permitDensityEditing = permitDensityEditing;
	}

	public double getConnectionDensity() {
        return connectionDensity;
    }

    /**
     * Set how dense the connections are between source and target neurons,
     * generally speaking the connectionDensity parameter represents a
     * probability reflecting how many possible connections between a given
     * source neuron and all available target neurons will actually be made.
     *
     * @param connectionDensity
     */
    public void setConnectionDensity(
        final double connectionDensity) {
    	// Don't change connection density if it's not permitted...
    	if (!permitDensityEditing) {
    		return;
    	}
        if (sparseOrdering == null) {
            this.connectionDensity = connectionDensity;
        } else {
            if (connectionDensity > this.connectionDensity) {
                addToSparsity(connectionDensity);
            } else if (connectionDensity < this.connectionDensity) {
                removeToSparsity(connectionDensity);
            }
        }
    }

    /**
     * @return whether or not self connections (connections where the source and
     *         target neuron are the same neuron) are allowed.
     */
    public boolean isSelfConnectionAllowed() {
        return selfConnectionAllowed;
    }

    /**
     * Set whether or not self connections (connections where the source and
     * target neuron are the same neuron) are allowed.
     *
     * @param selfConnectionAllowed Connections are allowed to connect to themselves.
     */
    public void setSelfConnectionAllowed(boolean selfConnectionAllowed) {
        if (this.selfConnectionAllowed != selfConnectionAllowed) {
            this.selfConnectionAllowed = selfConnectionAllowed;
            if (!selfConnectionAllowed && synapseGroup != null) {
                // Self connections were allowed but no longer and we're editing
                // an extant synapse group...
                if (synapseGroup.isRecurrent()) {
                    // Only matters if the synapse group is recurrent
                    for (Neuron n : synapseGroup.getSourceNeurons()) {
                        // Connects the neuron to itself
                        Synapse toRemove = n.getFanOut().get(n);
                        if (toRemove != null) {
                            // Remove from the synapse group
                            synapseGroup.removeSynapse(toRemove);
                            // Remove from the neuron
                            n.removeEfferent(toRemove);
                        }
                    }
                }

            }
        }
    }

    /**
     * @return the synapse group tied to this sparse object.
     */
    public SynapseGroup getSynapseGroup() {
        return synapseGroup;
    }

    /**
     * Returns a short name for this connection type, used in combo boxes.
     *
     * @return the name for this connection type
     */
    public static String getName() {
        return "Sparse";
    }

    @Override
    public String toString() {
        return getName();
    }

}
