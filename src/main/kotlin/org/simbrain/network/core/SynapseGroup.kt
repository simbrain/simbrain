package org.simbrain.network.core

import kotlinx.coroutines.Dispatchers
import org.simbrain.network.connections.*
import org.simbrain.network.events.SynapseGroupEvents
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.SynapseNode
import org.simbrain.util.CachedObject
import org.simbrain.util.MessageKind
import org.simbrain.util.SimbrainConstants
import org.simbrain.util.showMessage
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.AttributeContainer
import smile.math.matrix.Matrix

/**
 * Lightweight collection of synapses. Contains references to a source and target layer, a connection strategy, and a
 * list of synapses.
 *
 * Create new Synapse Groups by specifying a strategy or providing a list of synapses.
 */
class SynapseGroup @JvmOverloads constructor(
    val source: NeuronCollection,
    val target: NeuronCollection,
    var connectionStrategy: ConnectionStrategy = AllToAll(),
    synapses: MutableList<Synapse> = connectionStrategy.connectNeurons(source.neuronList, target.neuronList).toMutableList()
) : NetworkModel(), AttributeContainer {
    override val attributeName get() = displayName

    /**
     * Randomizer for all weights, regardless of polarity. Applying it can change the polarity of a weight.
     * The connection strategy contains randomizers for excitatory and inhibitory weights specifically.
     */
    @Transient
    var weightRandomizer: ProbabilityDistribution = UniformRealDistribution(-1.0, 1.0)

    @Transient
    override var events = SynapseGroupEvents()

    /**
     * The synapses with a non-static learning rule, the only ones [update] has work for; a large group of static
     * synapses would otherwise be walked serially on every update. Dropped when the list changes or a synapse's
     * learning rule is replaced.
     */
    // `this.`: in an initializer, a bare `synapses` is the constructor parameter, not the group's current list
    @Transient
    private val learningSynapses = CachedObject { this.synapses.filter { it.isLearning } }

    /** One instance shared by every synapse in the group, so each pays only a slot in its event's handler list. */
    @Transient
    private val learningRuleChanged: (Any?) -> Unit = { learningSynapses.invalidate() }

    /**
     * The group's synapses. Change them through [addSynapse], [restoreSynapse] and [restoreSynapses], which keep
     * [learningSynapses] current.
     */
    val synapses: List<Synapse> = ArrayList(synapses).onEach { synapse ->
        synapse.isVisible = false
        addSynapseListener(synapse)
    }

    /**
     * [synapses] for editing. The field is declared as a read-only list so other code cannot edit it in place, and
     * keeps its name because saved workspaces store it under that name.
     */
    private val synapseList get() = synapses as MutableList<Synapse>

    private inline fun editSynapses(edit: MutableList<Synapse>.() -> Unit) {
        synapseList.edit()
        learningSynapses.invalidate()
    }

    /**
     * Flag for whether synapses should be displayed in a GUI representation of this object.
     *
     * Individual synapse visibility is handled via the isVisible field. Changes to visibility
     * fire an event which is received by [SynapseNode].
     */
    var displaySynapses = false
        set(value) {
            field = value
            // Snapshot: refreshVisibility may flip this from a Dispatchers.Default synapse listener while
            // the GUI reconcile iterates `synapses` on the EDT, so don't iterate the live list here.
            this.synapses.toList().forEach { it.isVisible = value }
            events.visibilityChanged.fire()
        }

    /**
     * When true, [displaySynapses] is kept in sync with the visibility threshold automatically as the
     * group's size changes (see [refreshVisibility]). A manual visibility toggle clears this so the
     * user's explicit choice is preserved.
     */
    var autoVisibility = true

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

    /**
     * Recompute [displaySynapses] from the current synapse count against the visibility threshold, so the
     * group shows individual synapses when few and collapses to an arrow when many. Uses the actual
     * synapse count (matching the manual-toggle gate and the rendering reconcile), which is exactly what
     * the add/remove triggers change. No-op once the user has manually overridden visibility
     * ([autoVisibility] is false). Only assigns on an actual change, so the visibilityChanged event (and
     * the GUI rebuild it drives) fires only when the representation actually flips.
     */
    fun refreshVisibility() {
        if (!autoVisibility) return
        val show = synapses.size <= NetworkPreferences.synapseVisibilityThreshold
        if (show != displaySynapses) displaySynapses = show
    }

    private suspend fun removeAllSynapses(): List<NetworkModel> {
        return buildList {
            synapses.toList().forEach {  synapse ->
                val deletedBySynapse = synapse.delete()
                addAll(deletedBySynapse)
            }
        }.also { editSynapses { clear() } }
    }

    /**
     * True while [delete] tears the group down synapse by synapse. The per-synapse listeners then leave the list
     * to be cleared at the end: shrinking it one by one would cross the visibility threshold partway through and
     * have the GUI build loose nodes for synapses about to go, and emptying it would re-enter [delete].
     */
    @Transient
    @Volatile
    private var deleting = false

    override suspend fun delete(): List<NetworkModel> {
        deleting = true
        try {
            val removedSynapses = removeAllSynapses()
            target.removeIncomingSg(this)
            source.removeOutgoingSg(this)
            events.deleted.fire(this)
            return listOf(this) + removedSynapses
        } finally {
            deleting = false
        }
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
        editSynapses { add(syn) }
        refreshVisibility()
    }

    /** Puts back a synapse of this group whose deletion is being undone. */
    fun restoreSynapse(synapse: Synapse) = editSynapses { add(synapse) }

    /** Puts back the group's synapses as they were when an addition being redone was first made. */
    fun restoreSynapses(synapses: List<Synapse>) = editSynapses {
        clear()
        addAll(synapses)
    }

    fun isRecurrent(): Boolean {
        return source == target
    }

    context(Network)
    override fun update() {
        learningSynapses.value.forEach { it.update() }
        events.updated.fire()
    }

    fun size(): Int = this.synapses.size

    fun randomizeSymmetric(randomizer: ProbabilityDistribution?) {
        randomize(randomizer)
        this.synapses.forEach { it.symmetricSynapse?.let { s -> it.forceSetStrength(s.strength) } }
        events.updated.fire()
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        if (randomizer != null) {
            // If a specific randomizer is provided, use it directly for all synapses
            this.synapses.forEach { it.forceSetStrength(randomizer.sampleDouble()) }
        } else {
            // Otherwise use the connection strategy's weight initializer
            val polarized = splitSynapsesByPolarity(this.synapses, connectionStrategy.percentExcitatory)
            connectionStrategy.weightInitializer.initializeWeights(polarized)
        }
    }

    fun randomizeExcitatory() {
        val excitatorySynapses = this.synapses
            .filter { s -> s.source.polarity == SimbrainConstants.Polarity.EXCITATORY }
        connectionStrategy.weightInitializer.initializeWeights(PolarizedSynapseCollection(excitatorySynapses, emptyList()))
    }

    fun randomizeInhibitory() {
        val inhibitorySynapses = this.synapses
            .filter { s -> s.source.polarity == SimbrainConstants.Polarity.INHIBITORY }
        connectionStrategy.weightInitializer.initializeWeights(PolarizedSynapseCollection(emptyList(), inhibitorySynapses))
    }

    override fun toggleClamping() {
        this.synapses.forEach { it.toggleClamping() }
    }


    override fun toString(): String {
        return """
            Name: $displayName
            Size: ${size()} synapses
            Source: ${source.displayName}
            Target: ${target.displayName} 
            Connection Strategy: ${connectionStrategy.tooltipText()}
        """.trimIndent()
    }

    suspend fun applyConnectionStrategy() {
        connectionStrategy.let { strategy ->
            if (strategy is Sparse) {
                with(strategy) {
                    val result = createSparseSynapses(source.neuronList, target.neuronList, connectionDensity, allowSelfConnection, equalizeEfferents, random)
                    if (result is ConnectionsResult.Remove && result.removedAll) {
                        showMessage("Connection strategy not applied: The result is empty. Please check your connection strategy parameters.", MessageKind.WARNING)
                        return
                    }
                    when(result) {
                        is ConnectionsResult.Add -> {
                            polarizeSynapses(result.connectionsToAdd, percentExcitatory)
                            result.connectionsToAdd.forEach {
                                addSynapse(it)
                            }
                        }
                        is ConnectionsResult.Reset -> {
                            polarizeSynapses(result.resultConnections, percentExcitatory)
                        }
                        is ConnectionsResult.Remove -> {
                            result.connectionsToRemove.forEach { it.delete() }
                        }
                    }
                }

            } else {
                val existingSynapses = synapses.toList()
                val newSynapses = strategy.connectNeurons(
                    source.neuronList,
                    target.neuronList
                )
                if (newSynapses.isEmpty()) {
                    showMessage("Connection strategy not applied: The result is empty. Please check your connection strategy parameters.", MessageKind.WARNING)
                    return
                }
                // Can’t let number of synapses get to 0, because this triggers deletion of the synapse group.
                newSynapses.forEach {
                    addSynapse(it)
                }
                existingSynapses.toList().forEach { it.delete() }
            }
            events.synapseListChanged.fire()
        }
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
        synapse.events.learningRuleUpdated.addImmediate(learningRuleChanged)
        synapse.events.deleted.on(Dispatchers.Default) {
            if (deleting) return@on
            editSynapses { remove(it) }
            if (this.synapses.isEmpty()) {
                this.delete()
            } else {
                refreshVisibility()
            }
        }
    }

    override fun clear() {
        synapses.forEach { it.hardClear() }
        events.updated.fire()
    }

}