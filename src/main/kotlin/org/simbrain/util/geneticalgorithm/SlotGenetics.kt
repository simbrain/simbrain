package org.simbrain.util.geneticalgorithm

import org.simbrain.network.core.*
import org.simbrain.network.updaterules.DecayRule
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

// ======================================================================
// Slot-Based Evolution DSL
//
// A "slot" holds both the gene-level view (for mutation) and the
// phenotype-level view (after expression) of an evolvable component.
//
// Slots are declared as `val` delegated properties on a SlotGenotype subclass.
// The delegation handles registration, and the base class provides:
// - Automatic copy with correct dependency ordering
// - Expression with linked chromosome resolution
// - Gene addition that auto-adds to linked chromosomes
// ======================================================================

// --- Slot types ---

interface EvolutionSlot {
    /**
     * Determines copy and expression ordering. Lower values are processed first.
     * Same convention as [updatingOrder] in NetworkUtils.kt.
     * - 0: independent slots (nodes, linked chromosomes, collection-linked slots)
     * - 10: dependent slots (connections — need nodes copied first for onCopied listeners)
     */
    val processingOrder: Int get() = 0
    fun copySlot(): EvolutionSlot
}

/**
 * A chromosome of [Gene]s linked to a network slot (node or connection).
 * Kept in sync: adding a gene to the network slot auto-adds a default here,
 * and expression auto-applies these genes to the expressed network objects (per-element).
 *
 * Declared via [SlotGenotype.neuronRuleChromosome], [SlotGenotype.synapseRuleChromosome], etc.
 * Linkage established by passing a `::target` property reference.
 */
class LinkedChromosomeSlot<G : Gene<Unit, *>>(
    val genes: MutableList<G>,
    val createDefault: () -> G,
    val applyOnExpress: (Any, Any?) -> Unit
) : EvolutionSlot {

    override fun copySlot() = LinkedChromosomeSlot(
        genes = genes.map {
            @Suppress("UNCHECKED_CAST")
            it.copy() as G
        }.toMutableList(),
        createDefault = createDefault,
        applyOnExpress = applyOnExpress
    )
}

/**
 * A single [Gene] linked to a network slot, applied to the whole expressed
 * collection (not per-element). Used for layout genes, connection strategy genes, etc.
 *
 * Declared via [SlotGenotype.layoutChromosome], [SlotGenotype.connectionStrategyChromosome], etc.
 */
class CollectionLinkedSlot<G : Gene<Unit, *>>(
    var gene: G,
    val createDefault: () -> G,
    val applyToCollection: (NeuronCollection, Any?, Network) -> Unit
) : EvolutionSlot {

    @Suppress("UNCHECKED_CAST")
    override fun copySlot() = CollectionLinkedSlot(
        gene = gene.copy() as G,
        createDefault = createDefault,
        applyToCollection = applyToCollection
    )

    suspend fun express() = gene.express(Unit)
}

/**
 * Holds a chromosome of [NodeGene]s.
 * Before expression: access [genes] for mutation.
 * After expression: access [neurons] for wiring/evaluation.
 */
class NodeChromosomeSlot(
    internal val chromosome: Chromosome<Neuron, NodeGene>
) : EvolutionSlot {

    val genes: List<NodeGene> get() = chromosome

    private var _neurons: NeuronCollection? = null
    val neurons: NeuronCollection
        get() = _neurons ?: error("Not expressed yet — call express() in your build block")

    suspend fun express(
        network: Network,
        linked: List<LinkedChromosomeSlot<*>> = emptyList(),
        collectionLinked: List<CollectionLinkedSlot<*>> = emptyList()
    ): NeuronCollection {
        val expressed = chromosome.map { it.express(network) }
        for (linkedSlot in linked) {
            expressed.zip(linkedSlot.genes).forEach { (neuron, gene) ->
                linkedSlot.applyOnExpress(neuron, gene.express(Unit))
            }
        }
        _neurons = NeuronCollection(expressed).also { network.addNetworkModelAsync(it) }
        for (slot in collectionLinked) {
            slot.applyToCollection(_neurons!!, slot.express(), network)
        }
        return _neurons!!
    }

    override fun copySlot() = NodeChromosomeSlot(chromosome.copy())
}

/**
 * Holds a chromosome of [ConnectionGene]s.
 * Before expression: access [genes] for mutation.
 * After expression: access [synapses] for inspection.
 */
class ConnectionChromosomeSlot(
    internal val chromosome: Chromosome<Synapse, ConnectionGene>
) : EvolutionSlot {

    override val processingOrder: Int get() = 10

    val genes: List<ConnectionGene> get() = chromosome

    private var _synapses: List<Synapse>? = null
    val synapses: List<Synapse>
        get() = _synapses ?: error("Not expressed yet — call express() in your build block")

    suspend fun express(network: Network, linked: List<LinkedChromosomeSlot<*>> = emptyList()): List<Synapse> {
        _synapses = chromosome.map { it.express(network) }
        for (linkedSlot in linked) {
            _synapses!!.zip(linkedSlot.genes).forEach { (synapse, gene) ->
                linkedSlot.applyOnExpress(synapse, gene.express(Unit))
            }
        }
        return _synapses!!
    }

    override fun copySlot() = ConnectionChromosomeSlot(chromosome.copy())
}

// --- Property delegation ---

class SlotDelegate<S : EvolutionSlot>(var slot: S) : ReadOnlyProperty<Any?, S> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): S = slot
}

// --- SlotGenotype ---

/**
 * Base class for genotypes using the slot DSL.
 *
 * Declare slots as `val` delegated properties:
 * ```
 * val inputs by nodeChromosome(2) { clamped = true }
 * val hidden by nodeChromosome(5)
 * val connections by connectionChromosome()
 * val hiddenRules by neuronRuleChromosome(::hidden)
 * val hiddenLayout by layoutChromosome(::hidden)
 * ```
 *
 * The base class provides:
 * - Automatic [copyGenotype] with dependency-ordered slot copying
 * - [expressAll] that expresses in dependency order with linked chromosome resolution
 * - [addGene]/[addConnection] that auto-adds to linked chromosomes
 */
abstract class SlotGenotype(seed: Long = Random.nextLong()) : Genotype {

    override val random: Random = Random(seed)

    internal val slotEntries = mutableListOf<Pair<String, SlotDelegate<*>>>()

    // Linkages: network slot name → list of per-element linked chromosome names
    private val linkedSlots = mutableMapOf<String, MutableList<String>>()
    // Linkages: network slot name → list of collection-level linked slot names
    private val collectionLinkedSlots = mutableMapOf<String, MutableList<String>>()

    // -- Slot creation --

    protected fun nodeChromosome(
        size: Int,
        init: Neuron.() -> Unit = {}
    ): PropertyDelegateProvider<Any?, SlotDelegate<NodeChromosomeSlot>> {
        val genes = (0 until size).map { nodeGene(init) }
        val slot = NodeChromosomeSlot(Chromosome(genes))
        return PropertyDelegateProvider { _, property ->
            SlotDelegate(slot).also { slotEntries.add(property.name to it) }
        }
    }

    protected fun connectionChromosome(): PropertyDelegateProvider<Any?, SlotDelegate<ConnectionChromosomeSlot>> {
        val slot = ConnectionChromosomeSlot(Chromosome(emptyList()))
        return PropertyDelegateProvider { _, property ->
            SlotDelegate(slot).also { slotEntries.add(property.name to it) }
        }
    }

    // --- Linked chromosome helpers ---

    protected fun neuronRuleChromosome(
        target: KProperty0<NodeChromosomeSlot>,
        defaultRule: () -> NeuronRuleGene = { neuronRuleGene(DecayRule()) }
    ) = linkedChromosomeSlot(
        targetName = target.name,
        targetSizeProvider = { (slotEntries.first { it.first == target.name }.second.slot as NodeChromosomeSlot).chromosome.size },
        createDefault = defaultRule,
        applyOnExpress = { neuron, expressed ->
            (neuron as Neuron).updateRule = (expressed as NeuronRuleGeneWrapper).updateRule.copy()
        }
    )

    protected fun synapseRuleChromosome(
        target: KProperty0<ConnectionChromosomeSlot>,
        defaultRule: () -> SynapseRuleGene = { synapseRuleGene() }
    ) = linkedChromosomeSlot(
        targetName = target.name,
        targetSizeProvider = { (slotEntries.first { it.first == target.name }.second.slot as ConnectionChromosomeSlot).chromosome.size },
        createDefault = defaultRule,
        applyOnExpress = { synapse, expressed ->
            (synapse as Synapse).learningRule = (expressed as SynapseRuleGeneWrapper).learningRule.copy()
        }
    )

    protected fun layoutChromosome(
        target: KProperty0<NodeChromosomeSlot>,
        default: () -> LayoutGene = { layoutGene() }
    ): PropertyDelegateProvider<Any?, SlotDelegate<CollectionLinkedSlot<LayoutGene>>> {
        val slot = CollectionLinkedSlot(
            gene = default(),
            createDefault = default,
            applyToCollection = { neurons, expressed, _ ->
                (expressed as LayoutGeneWrapper).express().layoutNeurons(neurons.neuronList)
            }
        )
        return PropertyDelegateProvider { _, property ->
            collectionLinkedSlots.getOrPut(target.name) { mutableListOf() }.add(property.name)
            SlotDelegate(slot).also { slotEntries.add(property.name to it) }
        }
    }

    protected fun connectionStrategyChromosome(
        target: KProperty0<NodeChromosomeSlot>,
        default: () -> ConnectionStrategyGene = { connectionStrategyGene() }
    ): PropertyDelegateProvider<Any?, SlotDelegate<CollectionLinkedSlot<ConnectionStrategyGene>>> {
        val slot = CollectionLinkedSlot(
            gene = default(),
            createDefault = default,
            applyToCollection = { neurons, expressed, network ->
                (expressed as ConnectionStrategyGeneWrapper).connectionStrategy
                    .connectNeurons(neurons.neuronList, neurons.neuronList)
                    .addToNetworkAsync(network)
            }
        )
        return PropertyDelegateProvider { _, property ->
            collectionLinkedSlots.getOrPut(target.name) { mutableListOf() }.add(property.name)
            SlotDelegate(slot).also { slotEntries.add(property.name to it) }
        }
    }

    private fun <G : Gene<Unit, *>> linkedChromosomeSlot(
        targetName: String,
        targetSizeProvider: () -> Int,
        createDefault: () -> G,
        applyOnExpress: (Any, Any?) -> Unit
    ): PropertyDelegateProvider<Any?, SlotDelegate<LinkedChromosomeSlot<G>>> {
        val slot = LinkedChromosomeSlot(mutableListOf(), createDefault, applyOnExpress)
        return PropertyDelegateProvider { _, property ->
            linkedSlots.getOrPut(targetName) { mutableListOf() }.add(property.name)
            val currentSize = targetSizeProvider()
            repeat(currentSize) {
                slot.genes.add(slot.createDefault())
            }
            SlotDelegate(slot).also { slotEntries.add(property.name to it) }
        }
    }

    // --- Gene addition (auto-adds to linked chromosomes) ---

    fun NodeChromosomeSlot.addGene(gene: NodeGene) {
        chromosome.add(gene)
        addLinkedDefaults(this)
    }

    fun ConnectionChromosomeSlot.addGene(gene: ConnectionGene) {
        chromosome.add(gene)
        addLinkedDefaults(this)
    }

    fun ConnectionChromosomeSlot.addConnection(
        vararg layerPairs: Pair<NodeChromosomeSlot, NodeChromosomeSlot>,
        init: Synapse.() -> Unit = {}
    ): ConnectionGene? {
        val gene = chromosome.createGene(
            *layerPairs.map { (s, t) -> s.chromosome to t.chromosome }.toTypedArray(),
            synapseGeneTemplate = init
        )
        if (gene != null) addLinkedDefaults(this)
        return gene
    }

    private fun addLinkedDefaults(slot: EvolutionSlot) {
        val slotName = slotEntries.firstOrNull { it.second.slot === slot }?.first ?: return
        val pairedNames = linkedSlots[slotName] ?: return
        for (name in pairedNames) {
            val pairedSlot = slotEntries.first { it.first == name }.second.slot as LinkedChromosomeSlot<*>
            @Suppress("UNCHECKED_CAST")
            (pairedSlot as LinkedChromosomeSlot<Gene<Unit, *>>).genes.add(pairedSlot.createDefault())
        }
    }

    // --- Auto-copy ---

    abstract fun createNew(seed: Long): SlotGenotype

    abstract fun mutate()

    open fun copyGenotype(): SlotGenotype {
        val new = createNew(random.nextLong())

        fun replaceSlot(name: String, copied: EvolutionSlot) {
            val target = new.slotEntries.first { it.first == name }.second
            @Suppress("UNCHECKED_CAST")
            (target as SlotDelegate<EvolutionSlot>).slot = copied
        }

        for ((name, delegate) in slotEntries.sortedBy { it.second.slot.processingOrder }) {
            replaceSlot(name, delegate.slot.copySlot())
        }
        return new
    }

    // --- Expression with linked chromosome resolution ---

    private fun resolveLinked(slotName: String): List<LinkedChromosomeSlot<*>> {
        val names = linkedSlots[slotName] ?: return emptyList()
        return names.map { name ->
            slotEntries.first { it.first == name }.second.slot as LinkedChromosomeSlot<*>
        }
    }

    private fun resolveCollectionLinked(slotName: String): List<CollectionLinkedSlot<*>> {
        val names = collectionLinkedSlots[slotName] ?: return emptyList()
        return names.map { name ->
            slotEntries.first { it.first == name }.second.slot as CollectionLinkedSlot<*>
        }
    }

    suspend fun expressAll(network: Network) {
        for ((name, delegate) in slotEntries.sortedBy { it.second.slot.processingOrder }) {
            when (val slot = delegate.slot) {
                is NodeChromosomeSlot -> slot.express(network, resolveLinked(name), resolveCollectionLinked(name))
                is ConnectionChromosomeSlot -> slot.express(network, resolveLinked(name))
            }
        }
    }
}
