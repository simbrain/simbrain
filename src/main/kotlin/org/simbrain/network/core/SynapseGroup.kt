package org.simbrain.network.core

import org.simbrain.network.NetworkModel
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.events.SynapseGroupEvents
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.SynapseNode
import org.simbrain.network.util.SimnetUtils
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
    var synapses: MutableList<Synapse> = connectionStrategy.connectNeurons(source.neuronList, target.neuronList).toMutableList()
) : NetworkModel(), AttributeContainer {

    // TODO: When passing in synapses check all source are in source and all target are in target
    // reuse this in addsynapse


    /**
     * Randomizer for all weights, regardless of polarity. Applying it can change the polarity of a weight.
     * The connection strategy contains randomizers for excitatory and inhibitory weights specifically.
     */
    @Transient
    var weightRandomizer: ProbabilityDistribution = UniformRealDistribution(-1.0, 1.0)


    @Transient
    override var events = SynapseGroupEvents()

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
            events.visibilityChanged.fireAndForget()
        }

    init {
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
        displaySynapses = source.size() * target.size() <= threshold
    }

    override fun delete() {
        this.synapses.forEach { it.delete() }
        target.removeIncomingSg(this)
        source.removeOutgoingSg(this)
        events.deleted.fireAndForget(this)
    }

    fun addSynapse(syn: Synapse) {
        syn.isVisible = displaySynapses
        this.synapses.add(syn)
        events.synapseAdded.fireAndForget(syn)
    }

    fun removeSynapse(syn: Synapse) {
        this.synapses.remove(syn)
        events.synapseRemoved.fireAndForget(syn)
    }

    fun isRecurrent(): Boolean {
        return source == target
    }

    context(Network)
    override fun update() {
        this.synapses.forEach { it.update() }
    }

    fun size(): Int = this.synapses.size

    fun randomizeSymmetric() {
        randomize()
        this.synapses.forEach { it.symmetricSynapse?.let { s -> it.forceSetStrength(s.strength) } }
        events.updated.fireAndBlock()
    }

    override fun randomize() {
        this.synapses.forEach {
            when (it.target.polarity) {
                SimbrainConstants.Polarity.EXCITATORY -> it.forceSetStrength(connectionStrategy.exRandomizer.sampleDouble())
                SimbrainConstants.Polarity.INHIBITORY -> it.forceSetStrength(connectionStrategy.inRandomizer.sampleDouble())
                SimbrainConstants.Polarity.BOTH -> it.forceSetStrength(weightRandomizer.sampleDouble())
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

    override fun postOpenInit() {
        if (events == null) {
            events = SynapseGroupEvents()
        }
        this.synapses.forEach { it.postOpenInit() }
    }

    override fun toString(): String {
        return ("$id  with ${size()} synapse(s) from $source.id to $target.id")
    }

    /**
     * Copy this synapse group onto another neurongroup source/target pair.
     */
    fun copy(src: AbstractNeuronCollection, tar: AbstractNeuronCollection): SynapseGroup {
        
        require(!(source.size() != src.size() || target.size() != tar.size())) { "Size of source and " +
                "target of this synapse group do not match." }

        val mapping = (source.neuronList + target.neuronList)
            .zip(src.neuronList + tar.neuronList)
            .toMap()

        val syns = this.synapses.map{
                Synapse(mapping[it.source]!!, mapping[it.target]!!, it)
            }.toMutableList()

        return SynapseGroup(src, tar, connectionStrategy).also {
            it.synapses = syns
        }
    }

    fun applyConnectionStrategy() {
        synapses.toList().forEach { removeSynapse(it) }
        connectionStrategy.connectNeurons(
            source.neuronList,
            target.neuronList
        ).forEach {
            addSynapse(it)
        }
        events.synapseListChanged.fireAndForget()
    }

    fun getWeightMatrixArray(): Array<DoubleArray> {
        return SimnetUtils.getWeights(source.neuronList, target.neuronList);
    }

    fun getWeightMatrix(): Matrix {
        return Matrix.of(SimnetUtils.getWeights(source.neuronList, target.neuronList));
    }

    override fun clear() {
        synapses.forEach { it.hardClear() }
        events.updated.fireAndBlock()
    }

}