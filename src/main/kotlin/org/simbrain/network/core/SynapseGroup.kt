package org.simbrain.network.core

import kotlinx.coroutines.runBlocking
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.events.SynapseGroupEvents
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.SynapseNode
import org.simbrain.util.SimbrainConstants
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.AttributeContainer
import smile.math.matrix.Matrix

/**
 * Lightweight collection of synapses. Contains references to a source and target layer, a connection strategy, and a
 * list of synapses.
 */
class SynapseGroup @JvmOverloads constructor(
    val source: AbstractNeuronCollection,
    val target: AbstractNeuronCollection,
    var connectionStrategy: ConnectionStrategy = AllToAll(),
    synapses: MutableList<Synapse> = connectionStrategy.connectNeurons(source.neuronList, target.neuronList).toMutableList()
) : NetworkModel(), AttributeContainer {

    /**
     * Randomizer for all weights, regardless of polarity. Applying it can change the polarity of a weight.
     * The connection strategy contains randomizers for excitatory and inhibitory weights specifically.
     */
    @Transient
    var weightRandomizer: ProbabilityDistribution = UniformRealDistribution(-1.0, 1.0)

    @Transient
    override var events = SynapseGroupEvents()

    var synapses: MutableList<Synapse> = synapses.onEach { synapse -> addSynapseListener(synapse) }

    /**
     * Flag for whether synapses should be displayed in a GUI representation of this object.
     *
     * Individual synapse visibility is handled via the isVisible field. Changes to visibility
     * fire an event which is received by [SynapseNode].
     */
    var displaySynapses = false
        set(value) {
            field = value
            this.synapses.forEach { it.isVisible = value }
            events.visibilityChanged.fire()
        }

    init {
        // Validate that all synapses have sources in source collection and targets in target collection
        synapses.forEach { synapse ->
            require(synapse.source in source.neuronList) { 
                "Synapse source ${synapse.source.displayName} is not in source collection ${source.displayName}"
            }
            require(synapse.target in target.neuronList) { 
                "Synapse target ${synapse.target.displayName} is not in target collection ${target.displayName}"
            }
        }
        
        initializeSynapseVisibility()
        source.outgoingSg.add(this)
        target.incomingSgs.add(this)
    }

    /**
     * Determine whether this synpase group should initially have its synapses displayed. For isolated synapse groups
     * check its number of synapses. If the maximum number of possible connections exceeds a the network's synapse
     * visibility threshold, then individual synapses will not be displayed.
     */
    fun initializeSynapseVisibility() {
        val threshold = NetworkPreferences.synapseVisibilityThreshold
        displaySynapses = source.size * target.size <= threshold
    }

    private suspend fun removeAllSynapses(): List<NetworkModel> {
        return buildList {
            synapses.toList().forEach {  synapse ->
                val deletedBySynapse = synapse.delete()
                addAll(deletedBySynapse)
            }
        }.also { synapses.clear() }
    }

    override suspend fun delete(): List<NetworkModel> {
        val removedSynapses = removeAllSynapses()
        target.removeIncomingSg(this)
        source.removeOutgoingSg(this)
        events.deleted.fire(this).await()
        return listOf(this) + removedSynapses
    }

    override suspend fun afterRestore(context: Any?) {
        target.incomingSgs.add(this)
        source.outgoingSg.add(this)
        synapses.forEach { it.afterRestore() }
    }

    private fun addSynapse(syn: Synapse) {
        // Validate that synapse source and target are in the respective collections
        require(syn.source in source.neuronList) { 
            "Synapse source ${syn.source.displayName} is not in source collection ${source.displayName}"
        }
        require(syn.target in target.neuronList) { 
            "Synapse target ${syn.target.displayName} is not in target collection ${target.displayName}"
        }
        
        syn.isVisible = displaySynapses
        addSynapseListener(syn)
        this.synapses.add(syn)
        events.synapseAdded.fire(syn)
    }

    fun isRecurrent(): Boolean {
        return source == target
    }

    context(Network)
    override fun update() {
        this.synapses.forEach { it.update() }
        events.updated.fire()
    }

    fun size(): Int = this.synapses.size

    fun randomizeSymmetric(randomizer: ProbabilityDistribution?) {
        randomize(randomizer)
        this.synapses.forEach { it.symmetricSynapse?.let { s -> it.forceSetStrength(s.strength) } }
        events.updated.fire()
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        this.synapses.forEach {
            when (it.target.polarity) {
                SimbrainConstants.Polarity.EXCITATORY -> it.forceSetStrength(connectionStrategy.exRandomizer.sampleDouble())
                SimbrainConstants.Polarity.INHIBITORY -> it.forceSetStrength(connectionStrategy.inRandomizer.sampleDouble())
                SimbrainConstants.Polarity.BOTH -> it.forceSetStrength((randomizer ?: weightRandomizer).sampleDouble())
            }
        }
    }

    fun randomizeExcitatory() {
        this.synapses
            .filter { s -> s.target.polarity == SimbrainConstants.Polarity.EXCITATORY }
            .forEach { it.forceSetStrength(connectionStrategy.exRandomizer.sampleDouble()) }
    }

    fun randomizeInhibitory() {
        this.synapses
            .filter { s -> s.target.polarity == SimbrainConstants.Polarity.INHIBITORY }
            .forEach { it.forceSetStrength(connectionStrategy.exRandomizer.sampleDouble()) }
    }

    override fun toggleClamping() {
        this.synapses.forEach { it.toggleClamping() }
    }

    override fun toString(): String {
        return ("$displayName  with ${size()} synapse(s) from ${source.displayName} to ${target.displayName}")
    }

    fun applyConnectionStrategy() {
        runBlocking {
            removeAllSynapses()
        }
        connectionStrategy.connectNeurons(
            source.neuronList,
            target.neuronList
        ).forEach {
            addSynapse(it)
        }
        events.synapseListChanged.fire()
    }

    fun getWeightMatrix(): Matrix {
        return getWeightMatrix(source.neuronList, target.neuronList).transpose()
    }

    fun setWeightMatrix(matrix: Matrix) {
        source.neuronList.forEachIndexed { j, s ->
            target.neuronList.forEachIndexed { i, t ->
                s.fanOut[t]?.let {
                    it.strength = matrix[i, j]
                }
            }
        }
    }

    fun addSynapseListener(synapse: Synapse) {
        synapse.events.deleted.on(wait = true) {
            this.synapses.remove(it)
            if (this.synapses.isEmpty()) {
                this.delete()
            }
        }
    }

    override fun clear() {
        synapses.forEach { it.hardClear() }
        events.updated.fire()
    }

}