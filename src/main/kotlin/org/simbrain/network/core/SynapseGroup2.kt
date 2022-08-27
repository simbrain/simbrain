package org.simbrain.network.core

import org.simbrain.network.NetworkModel
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.ConnectionSelector
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.events.SynapseGroup2Events
import org.simbrain.network.groups.AbstractNeuronCollection
import org.simbrain.network.gui.nodes.SynapseNode
import org.simbrain.network.util.SimnetUtils
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.AttributeContainer
import smile.math.matrix.Matrix

/**
 * Lightweight collection of synapses
 */
class SynapseGroup2 @JvmOverloads constructor(
    val source: AbstractNeuronCollection,
    val target: AbstractNeuronCollection,
    connection: ConnectionStrategy = AllToAll(),
    val synapses: MutableList<Synapse> = connection.connectNeurons(source.network, source.neuronList, target
        .neuronList, false).toMutableList()
) : NetworkModel(), AttributeContainer {

    var connectionSelector: ConnectionSelector = ConnectionSelector(connection)

    // TODO: When passing in synapses check all source are in source and all target are in target
    // reuse this in addsynapse

    /**
     * Randomizer for all weights, regardless of polarity. Applying it can change the polarity of a weight.
     */
    val weightRandomizer = ProbabilityDistribution.Randomizer(UniformRealDistribution(-1.0, 1.0))

    /**
     * Randomizer for excitatory weights.
     */
    val excitatoryRandomizer = ProbabilityDistribution.Randomizer(UniformRealDistribution(0.0, 1.0))

    /**
     * Randomizer for inhibitory weights.
     */
    val inhibitoryRandomizer = ProbabilityDistribution.Randomizer(UniformRealDistribution(-1.0, 0.0))

    @Transient
    override var events: SynapseGroup2Events = SynapseGroup2Events(this)

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
            events.fireVisibilityChange()
        }

    init {
        initializeSynapseVisibility()
        label = source.network.idManager.getProposedId(this.javaClass)
        source.outgoingSg.add(this)
        target.incomingSgs.add(this)
    }

    /**
     * Determine whether this synpase group should initially have its synapses displayed. For isolated synapse groups
     * check its number of synapses. If the maximum number of possible connections exceeds a the network's synapse
     * visibility threshold, then individual synapses will not be displayed.
     */
    fun initializeSynapseVisibility() {
        val threshold = synapseVisibilityThreshold
        displaySynapses = source.size() * target.size() <= threshold
    }

    override fun delete() {
        this.synapses.forEach { it.delete() }
        target.removeIncomingSg(this)
        source.removeOutgoingSg(this)
        events.fireDeleted()
    }

    fun addSynapse(syn: Synapse) {
        syn.isVisible = displaySynapses
        this.synapses.add(syn)
        events.fireSynapseAdded(syn)
    }

    fun removeSynapse(syn: Synapse) {
        this.synapses.remove(syn)
        events.fireSynapseRemoved(syn)
    }

    fun isRecurrent(): Boolean {
        return source == target
    }

    override fun update() {
        this.synapses.forEach { it.update() }
    }

    fun size(): Int = this.synapses.size

    override fun randomize() {
        this.synapses.forEach { it.randomize() }
    }

    override fun toggleClamping() {
        this.synapses.forEach { it.toggleClamping() }
    }

    override fun postOpenInit() {
        if (events == null) {
            events = SynapseGroup2Events(this)
        }
        this.synapses.forEach { it.postOpenInit() }
    }

    override var id: String? = super<NetworkModel>.id

    override fun toString(): String {
        return ("$id  with ${size()} synapse(s) from $source.id to $target.id")
    }

    /**
     * Copy this synapse group onto another neurongroup source/target pair.
     */
    fun copy(src: AbstractNeuronCollection, tar: AbstractNeuronCollection): SynapseGroup2 {
        
        require(!(source.size() != src.size() || target.size() != tar.size())) { "Size of source and " +
                "target of this synapse group do not match." }

        val mapping = (source.neuronList + target.neuronList)
            .zip(src.neuronList + tar.neuronList)
            .toMap()

        val syns = this.synapses.map{
                Synapse(it.parentNetwork, mapping[it.source], mapping[it.target], it )
            }.toMutableList()

        return SynapseGroup2(src, tar, connectionSelector.cs.copy(), syns)
    }

    fun applyConnectionStrategy() {
        synapses.toList().forEach { removeSynapse(it) }
        val syns = connectionSelector.cs.connectNeurons(
            source.network,
            source.neuronList,
            target.neuronList,
            false
        )
        syns.forEach { addSynapse(it) }
        events.fireSynapseListChanged()
    }

    fun getWeightMatrixArray(): Array<DoubleArray> {
        return SimnetUtils.getWeights(source.neuronList, target.neuronList);
    }

    fun getWeightMatrix(): Matrix {
        return Matrix(SimnetUtils.getWeights(source.neuronList, target.neuronList));
    }
}