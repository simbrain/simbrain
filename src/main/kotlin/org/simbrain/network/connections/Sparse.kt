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
package org.simbrain.network.connections

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.getLooseSynapse
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.randShuffleK
import umontreal.ssj.randvar.BinomialGen
import java.util.*

/**
 * A superclass for all connectors whose primary parameter is related to base
 * connection density, taking no other major factors into account insofar as
 * selecting which neurons should be connected goes.
 *
 * @author Zoë Tosi
 */
class Sparse() : ConnectionStrategy(), EditableObject {

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
    var isEqualizeEfferents = DEFAULT_EE_PREF

    /**
     * A tag for whether or not this sparse connector supports density editing
     * (changing the number of connections after construction).
     */
    var isPermitDensityEditing = true

    /**
     * A map of permutations governing in what order connections to target
     * neurons will be added if the connection density is raised for each source
     * neuron. Maps which index of target neuron will be the next to be given a
     * connection, or in what order connections are removed for each source
     * neuron if density is lowered.
     * <br></br>
     * This is the thing that allows permitDensityEditing.  This is what makes it
     * expensive.
     */
    @Transient
    private var sparseOrdering: Array<IntArray>? = null

    /**
     * If efferent synapses are not equalized among source neurons, this array
     * contains the number of possible target neurons a given source neuron is
     * connected to.
     */
    @Transient
    private lateinit var currentOrderingIndices: IntArray

    /**
     * The source neurons.
     */
    @Transient
    private lateinit var sourceNeurons: Array<Neuron>

    /**
     * The target neurons.
     */
    @Transient
    private lateinit var targetNeurons: Array<Neuron>

    /**
     * @return the synapse group tied to this sparse object.
     */
    /**
     * The synapse group associated with this connection object.
     */
    var synapseGroup: SynapseGroup? = null
        private set

    /**
     * Generally speaking the connectionDensity parameter represents a
     * probability reflecting how many possible connections between a given
     * source neuron and all available target neurons will actually be made.
     */
    var connectionDensity = .8
        /**
         * Set how dense the connections are between source and target neurons,
         * generally speaking the connectionDensity parameter represents a
         * probability reflecting how many possible connections between a given
         * source neuron and all available target neurons will actually be made.
         *
         * @param connectionDensity
         */
        set(connectionDensity) {
            // Don't change connection density if it's not permitted...
            if (!isPermitDensityEditing) {
                return
            }
            if (sparseOrdering == null) {
                field = connectionDensity
            } else {
                if (connectionDensity > field) {
                    addToSparsity(connectionDensity)
                } else if (connectionDensity < field) {
                    removeToSparsity(connectionDensity)
                }
            }
        }

    /**
     * Whether or not connections where the source and target are the same
     * neuron are allowed. Only applicable if the source and target neuron sets
     * are the same.
     */
    protected var selfConnectionAllowed = false


    /**
     * Construct a sparse object from main arguments. Used in scripts.
     *
     * @param sparsity              sparsity level
     * @param equalizeEfferents     whether to equalize efferents
     * @param selfConnectionAllowed whether self-connections should be allowed.
     */
    constructor(sparsity: Double, equalizeEfferents: Boolean, selfConnectionAllowed: Boolean) : this() {
        connectionDensity = sparsity
        isEqualizeEfferents = equalizeEfferents
        this.selfConnectionAllowed = selfConnectionAllowed
    }

    /**
     * Connect source to target neurons using this instance of the sparse
     * object's properties to set all parameters of the connections.
     *
     * @param sourceNeurons the source neurons
     * @param targetNeurons the target neurons
     * @return the newly creates synapses connecting source to target
     */
    fun connect(sourceNeurons: List<Neuron>, targetNeurons: List<Neuron>): List<Synapse> {
        return connectSparse(
            sourceNeurons,
            targetNeurons,
            connectionDensity,
            selfConnectionAllowed,
            isEqualizeEfferents
        )

    }

    /**
     * Should only be called for initialization.
     *
     * @param synapseGroup The synapse group that the connections this class
     * will generate will be added to.
     */
    override fun connectNeurons(synapseGroup: SynapseGroup) {
        this.synapseGroup = synapseGroup
        val recurrent = synapseGroup.isRecurrent
        val numSrc = synapseGroup.sourceNeurons.size
        val numTar = synapseGroup.targetNeurons.size
        isPermitDensityEditing = numSrc * numTar < 10E8
        sourceNeurons = synapseGroup.sourceNeurons.toTypedArray()
        targetNeurons = if (recurrent) sourceNeurons else synapseGroup.targetNeurons.toTypedArray()
        // Are you initializing with the intention of editing later on?
        if (isPermitDensityEditing) {
            generateSparseOrdering(recurrent)
            if (isEqualizeEfferents) {
                connectEqualized(synapseGroup)
            } else {
                connectRandom(synapseGroup)
            }
        } else {
            val syns = connectSparse(synapseGroup.sourceNeurons, synapseGroup.targetNeurons)
            syns.forEach{s -> synapseGroup.addNewSynapse(s)}
        }
    }

    override fun connectNeurons(network: Network, source: List<Neuron>, target: List<Neuron>): List<Synapse> {
        val syns = connectSparse(source, target)
        network.addNetworkModels(syns)
        return syns
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
    private fun connectEqualized(synapseGroup: SynapseGroup) {
        currentOrderingIndices = IntArray(sourceNeurons.size)
        val numConnectsPerSrc: Int
        val expectedNumSyns: Int
        numConnectsPerSrc = if (synapseGroup.isRecurrent && !selfConnectionAllowed) {
            (connectionDensity * (sourceNeurons.size - 1)).toInt()
        } else {
            (connectionDensity * targetNeurons.size).toInt()
        }
        expectedNumSyns = numConnectsPerSrc * sourceNeurons.size
        synapseGroup.preAllocateSynapses(expectedNumSyns)
        var i = 0
        val n = sourceNeurons.size
        while (i < n) {
            currentOrderingIndices[i] = numConnectsPerSrc
            val src = sourceNeurons[i]
            var tar: Neuron
            for (j in 0 until numConnectsPerSrc) {
                tar = targetNeurons[sparseOrdering!![i][j]]
                val s = Synapse(src, tar)
                synapseGroup.addNewSynapse(s)
            }
            i++
        }
    }

    /**
     * Populates the synapse group with synapses by making individual synaptic
     * connections between the neurons in the synapse group's source and target
     * groups. These synapses are initialized with default attributes and zero
     * strength. The number of efferent synapses assigned to each source neuron
     * is drawn from a binomial distribution with a mean of
     * NumberOfTargetNeurons * sparsity
     * <br></br>
     * Assumes [.permitDensityEditing] is true.  Uses machinery for that.
     * This is for _initialization_ (of a connection that will allow permitdensity
     * editing), not re-editing.
     *
     *
     * @param synapseGroup
     */
    private fun connectRandom(synapseGroup: SynapseGroup) {
        currentOrderingIndices = IntArray(sourceNeurons.size)
        val numTars =
            if (synapseGroup.isRecurrent && !selfConnectionAllowed) sourceNeurons.size - 1 else targetNeurons.size
        synapseGroup.clear() // TODO: Zoe? Make
        synapseGroup.preAllocateSynapses((sourceNeurons.size * numTars * connectionDensity).toInt())
        var i = 0
        val n = sourceNeurons.size
        while (i < n) {
            currentOrderingIndices[i] =
                BinomialGen.nextInt(SimbrainMath.DEFAULT_RANDOM_STREAM, numTars, connectionDensity)
            val src = sourceNeurons[i]
            var tar: Neuron
            val tarLen = targetNeurons.size - 1
            var o: IntArray? = null
            o = if (sourceNeurons == targetNeurons && !selfConnectionAllowed) {
                SimbrainMath.randPermuteWithExclusion(0, tarLen + 1, i)
            } else {
                SimbrainMath.randPermute(0, tarLen + 1)
            }
            for (j in 0 until currentOrderingIndices[i]) {
                tar = targetNeurons[o[j]]
                val s = Synapse(src, tar)
                synapseGroup.addNewSynapse(s)
            }
            i++
        }
    }

    /**
     * @param recurrent
     */
    private fun generateSparseOrdering(recurrent: Boolean) {
        val srcLen = sourceNeurons.size
        if (recurrent && !selfConnectionAllowed) {
            val tarLen = targetNeurons.size - 1
            sparseOrdering = Array(sourceNeurons.size) { IntArray(tarLen) }
            for (i in 0 until srcLen) {
                sparseOrdering!![i] = SimbrainMath.randPermuteWithExclusion(0, tarLen + 1, i)
            }
        } else {
            val tarLen = targetNeurons.size
            sparseOrdering = Array(sourceNeurons.size) { IntArray(tarLen) }
            for (i in 0 until srcLen) {
                sparseOrdering!![i] = SimbrainMath.randPermute(0, tarLen)
            }
        }
    }

    /**
     * @param newSparsity new sparsity connection
     */
    fun removeToSparsity(newSparsity: Double) {
        require(newSparsity < connectionDensity) { "Cannot 'removeToSparsity' to" + " a higher connectivity density." }
        val net = sourceNeurons[0].network
        val removeTotal = synapseGroup!!.size() - (newSparsity * maxPossibleConnections).toInt()
        if (isEqualizeEfferents) {
            val curNumConPerSource = synapseGroup!!.size() / sourceNeurons.size
            val removePerSource = removeTotal / sourceNeurons.size
            val finalNumConPerSource = curNumConPerSource - removePerSource
            var i = 0
            val n = sourceNeurons.size
            while (i < n) {
                for (j in curNumConPerSource - 1 downTo finalNumConPerSource) {
                    getLooseSynapse(sourceNeurons[i], targetNeurons[sparseOrdering!![i][j]])!!.delete()
                }
                currentOrderingIndices[i] = finalNumConPerSource
                i++
            }
        } else {
            var i = 0
            val n = sourceNeurons.size
            while (i < n) {
                val numToRemove = BinomialGen.nextInt(
                    SimbrainMath.DEFAULT_RANDOM_STREAM,
                    synapseGroup!!.targetNeuronGroup.size(),
                    newSparsity
                )
                if (numToRemove < currentOrderingIndices[i]) {
                    val remove = decreaseDensity(i, numToRemove)
                    for (s in remove) {
                        synapseGroup!!.removeSynapse(s)
                    }
                } else {
                    val add = increaseDensity(i, numToRemove)
                    for (s in add) {
                        synapseGroup!!.addNewSynapse(s)
                    }
                }
                currentOrderingIndices[i] = numToRemove
                i++
            }
        }
        connectionDensity = newSparsity
    }

    /**
     * @param newSparsity new sparsity connection
     */
    fun addToSparsity(newSparsity: Double) {
        require(newSparsity > connectionDensity) { "Cannot 'addToSparsity' to" + " a lower connectivity density." }
        val addTotal = (newSparsity * maxPossibleConnections).toInt() - synapseGroup!!.size()
        val addList: MutableList<Synapse> = ArrayList(addTotal)
        if (isEqualizeEfferents) {
            val curNumConPerSource = synapseGroup!!.size() / sourceNeurons.size
            val addPerSource = addTotal / sourceNeurons.size
            var finalNumConPerSource = curNumConPerSource + addPerSource
            if (finalNumConPerSource > sparseOrdering!![0].size) {
                finalNumConPerSource = sparseOrdering!![0].size
            }
            var i = 0
            val n = sourceNeurons.size
            while (i < n) {
                for (j in curNumConPerSource until finalNumConPerSource) {
                    val toAdd = Synapse(sourceNeurons[i], targetNeurons[sparseOrdering!![i][j]])
                    addList.add(toAdd)
                }
                currentOrderingIndices[i] = finalNumConPerSource
                i++
            }
        } else {
            var i = 0
            val n = sourceNeurons.size
            while (i < n) {
                val numToAdd = BinomialGen.nextInt(
                    SimbrainMath.DEFAULT_RANDOM_STREAM,
                    synapseGroup!!.targetNeuronGroup.size(),
                    newSparsity
                )
                var finalNumConPerSource =
                    if (numToAdd >= currentOrderingIndices[i]) numToAdd else currentOrderingIndices[i]
                if (finalNumConPerSource > sparseOrdering!![i].size) {
                    finalNumConPerSource = sparseOrdering!![i].size
                }
                if (finalNumConPerSource >= currentOrderingIndices[i]) {
                    addList.addAll(increaseDensity(i, finalNumConPerSource))
                } else {
                    val remove = decreaseDensity(i, finalNumConPerSource)
                    for (s in remove) {
                        synapseGroup!!.removeSynapse(s)
                    }
                }
                currentOrderingIndices[i] = finalNumConPerSource
                i++
            }
        }
        for (s in addList) {
            synapseGroup!!.addNewSynapse(s)
        }
        connectionDensity = newSparsity
    }

    private fun increaseDensity(i: Int, finalNumConnections: Int): List<Synapse> {
        val added: MutableList<Synapse> = ArrayList(finalNumConnections - currentOrderingIndices[i])
        for (j in currentOrderingIndices[i] until finalNumConnections) {
            val toAdd = Synapse(sourceNeurons[i], targetNeurons[sparseOrdering!![i][j]])
            added.add(toAdd)
        }
        return added
    }

    /**
     * @param i
     * @param finalNumConnections
     * @return
     */
    private fun decreaseDensity(i: Int, finalNumConnections: Int): List<Synapse?> {
        val removed: MutableList<Synapse?> = ArrayList(currentOrderingIndices[i] - finalNumConnections)
        for (j in currentOrderingIndices[i] - 1 downTo finalNumConnections) {
            val toRemove = getLooseSynapse(sourceNeurons[i], targetNeurons[sparseOrdering!![i][j]])
            removed.add(toRemove)
        }
        return removed
    }

    val maxPossibleConnections: Int
        get() = if (selfConnectionAllowed || !synapseGroup!!.isRecurrent) {
            sourceNeurons.size * targetNeurons.size
        } else {
            sourceNeurons.size * (sourceNeurons.size - 1)
        }


    /**
     * @return whether or not self connections (connections where the source and
     * target neuron are the same neuron) are allowed.
     */
    fun isSelfConnectionAllowed(): Boolean {
        return selfConnectionAllowed
    }

    override fun getName(): String {
        return "Sparse"
    }

    override fun toString(): String {
        return name
    }

}

/**
 * The default preference as to whether or not self connections are allowed.
 */
const val DEFAULT_SELF_CONNECT_PREF = false

/**
 * Sets the default behavior concerning whether or not the number of
 * efferents of each source neurons should be equalized.
 */
val DEFAULT_EE_PREF = false

/**
 * The default sparsity (between 0 and 1).
 */
const val DEFAULT_CONNECTION_DENSITY = 0.1

/**
 * Connects two lists of neurons with synapses assigning connections between
 * source and target neurons randomly in such a way that results in
 * "sparsity" percentage of possible connections being created.
 *
 * @param sourceNeurons         source neurons
 * @param targetNeurons         target neurons
 * @param sparsity              sparsity of connection
 * @param selfConnectionAllowed whether to allow self-connections
 * @param equalizeEfferents     whether or not the number of efferents of each
 * source neurons should be equalized.
 * @return the new synapses
 */
fun connectSparse(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    sparsity: Double = DEFAULT_CONNECTION_DENSITY,
    selfConnectionAllowed: Boolean = DEFAULT_SELF_CONNECT_PREF,
    equalizeEfferents: Boolean = DEFAULT_EE_PREF
): List<Synapse> {
    val recurrent = testRecurrence(sourceNeurons, targetNeurons)
    var source: Neuron
    var target: Neuron
    var synapse: Synapse
    val syns = ArrayList<Synapse>()
    val rand = Random(System.nanoTime())
    if (equalizeEfferents) {
        val targetList = ArrayList<Int?>()
        var tListCopy: ArrayList<Int?>
        for (i in targetNeurons.indices) {
            targetList.add(i)
        }
        val numSyns: Int
        numSyns = if (!selfConnectionAllowed && sourceNeurons === targetNeurons) {
            (sparsity * sourceNeurons.size * (targetNeurons.size - 1)).toInt()
        } else {
            (sparsity * sourceNeurons.size * targetNeurons.size).toInt()
        }
        var synsPerSource = numSyns / sourceNeurons.size
        var targStart = 0
        var targEnd = synsPerSource
        if (synsPerSource > numSyns / 2) {
            synsPerSource = numSyns - synsPerSource
            targStart = synsPerSource
            targEnd = targetList.size
        }
        for (i in sourceNeurons.indices) {
            source = sourceNeurons[i]
            if (!selfConnectionAllowed && recurrent) {
                tListCopy = ArrayList()
                for (k in targetList.indices) {
                    if (k == i) { // Exclude oneself as a possible target
                        continue
                    }
                    tListCopy.add(targetList[k])
                }
                randShuffleK(tListCopy, synsPerSource, rand)
            } else {
                randShuffleK(targetList, synsPerSource, rand)
                tListCopy = targetList
            }
            for (j in targStart until targEnd) {
                target = targetNeurons[tListCopy[j]!!]
                synapse = Synapse(source, target)
                syns.add(synapse)
            }
        }
    } else {
        for (i in sourceNeurons.indices) {
            for (j in targetNeurons.indices) {
                if (!selfConnectionAllowed && recurrent && i == j) {
                    continue
                } else {
                    if (Math.random() < sparsity) {
                        source = sourceNeurons[i]
                        target = targetNeurons[j]
                        synapse = Synapse(source, target)
                        syns.add(synapse)
                    }
                }
            }
        }
    }
    return syns
}
