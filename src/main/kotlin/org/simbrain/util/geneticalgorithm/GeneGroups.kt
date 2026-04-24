package org.simbrain.util.geneticalgorithm

import org.simbrain.network.core.*
import org.simbrain.network.updaterules.DecayRule
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

// --- Gene group types ---

interface GeneGroup {
    /**
     * Determines copy and expression ordering. Lower values are processed first.
     * Same convention as [updatingOrder] in NetworkUtils.kt.
     * - 0: independent groups (nodes, linked chromosomes, collection-linked groups)
     * - 10: dependent groups (connections — need nodes copied first for onCopied listeners)
     */
    val processingOrder: Int get() = 0
    fun copy(): GeneGroup
}

/**
 * A chromosome of [Gene]s linked to a node or connection gene group.
 * Kept in sync: adding a gene to the target group auto-adds a default here,
 * and expression auto-applies these genes to the expressed network objects (per-element).
 *
 * Declared via [Genotype.neuronRuleChromosome], [Genotype.synapseRuleChromosome], etc.
 * Linkage established by passing a `::target` property reference.
 */
class LinkedGeneGroup<G : Gene<Unit, *>>(
    val genes: MutableList<G>,
    val createDefault: () -> G,
    val applyOnExpress: (Any, Any?) -> Unit
) : GeneGroup {

    override fun copy() = LinkedGeneGroup(
        genes = genes.map {
            @Suppress("UNCHECKED_CAST")
            it.copy() as G
        }.toMutableList(),
        createDefault = createDefault,
        applyOnExpress = applyOnExpress
    )
}

/**
 * A single [Gene] linked to a node gene group, applied to the whole expressed
 * collection (not per-element). Used for layout genes, connection strategy genes, etc.
 *
 * Declared via [Genotype.layoutChromosome], [Genotype.connectionStrategyChromosome], etc.
 */
class CollectionGeneGroup<G : Gene<Unit, *>>(
    var gene: G,
    val createDefault: () -> G,
    val applyToCollection: (NeuronCollection, Any?, Network) -> Unit
) : GeneGroup {

    @Suppress("UNCHECKED_CAST")
    override fun copy() = CollectionGeneGroup(
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
class NodeGeneGroup(
    internal val chromosome: Chromosome<Neuron, NodeGene>
) : GeneGroup {

    val genes: List<NodeGene> get() = chromosome

    private var _neurons: NeuronCollection? = null
    val neurons: NeuronCollection
        get() = _neurons ?: error("Not expressed yet — call express() in your build block")

    suspend fun express(
        network: Network,
        linked: List<LinkedGeneGroup<*>> = emptyList(),
        collectionLinked: List<CollectionGeneGroup<*>> = emptyList()
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

    override fun copy() = NodeGeneGroup(chromosome.copy())
}

/**
 * Holds a chromosome of [ConnectionGene]s.
 * Before expression: access [genes] for mutation.
 * After expression: access [synapses] for inspection.
 */
class ConnectionGeneGroup(
    internal val chromosome: Chromosome<Synapse, ConnectionGene>
) : GeneGroup {

    override val processingOrder: Int get() = 10

    val genes: List<ConnectionGene> get() = chromosome

    private var _synapses: List<Synapse>? = null
    val synapses: List<Synapse>
        get() = _synapses ?: error("Not expressed yet — call express() in your build block")

    suspend fun express(network: Network, linked: List<LinkedGeneGroup<*>> = emptyList()): List<Synapse> {
        _synapses = chromosome.map { it.express(network) }
        for (linkedSlot in linked) {
            _synapses!!.zip(linkedSlot.genes).forEach { (synapse, gene) ->
                linkedSlot.applyOnExpress(synapse, gene.express(Unit))
            }
        }
        return _synapses!!
    }

    override fun copy() = ConnectionGeneGroup(chromosome.copy())
}

// --- Property delegation ---

class GeneGroupDelegate<S : GeneGroup>(var group: S) : ReadOnlyProperty<Any?, S> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): S = group
}

// --- Genotype ---

/**
 * Base class for genotypes using the gene group DSL.
 *
 * Declare gene groups as `val` delegated properties:
 * ```
 * val inputs by nodeChromosome(2) { clamped = true }
 * val hidden by nodeChromosome(5)
 * val connections by connectionChromosome()
 * val hiddenRules by neuronRuleChromosome(::hidden)
 * val hiddenLayout by layoutChromosome(::hidden)
 * ```
 *
 * The base class provides:
 * - Automatic [copy] with dependency-ordered gene group copying
 * - [expressAll] that expresses in dependency order with linked chromosome resolution
 * - [addGene]/[addConnection] that auto-adds to linked chromosomes
 */
abstract class Genotype(seed: Long = Random.nextLong()) {

    val random: Random = Random(seed)

    internal val geneGroups = mutableListOf<Pair<String, GeneGroupDelegate<*>>>()

    // Linkages: gene group name → list of per-element linked chromosome names
    private val linkedGroups = mutableMapOf<String, MutableList<String>>()
    // Linkages: gene group name → list of collection-level linked gene group names
    private val collectionLinkedGroups = mutableMapOf<String, MutableList<String>>()

    // -- Gene group creation --

    protected fun nodeChromosome(
        size: Int,
        init: Neuron.() -> Unit = {}
    ): PropertyDelegateProvider<Any?, GeneGroupDelegate<NodeGeneGroup>> {
        val genes = (0 until size).map { nodeGene(init) }
        val slot = NodeGeneGroup(Chromosome(genes))
        return PropertyDelegateProvider { _, property ->
            GeneGroupDelegate(slot).also { geneGroups.add(property.name to it) }
        }
    }

    protected fun connectionChromosome(): PropertyDelegateProvider<Any?, GeneGroupDelegate<ConnectionGeneGroup>> {
        val slot = ConnectionGeneGroup(Chromosome(emptyList()))
        return PropertyDelegateProvider { _, property ->
            GeneGroupDelegate(slot).also { geneGroups.add(property.name to it) }
        }
    }

    // --- Linked chromosome helpers ---

    protected fun neuronRuleChromosome(
        target: KProperty0<NodeGeneGroup>,
        defaultRule: () -> NeuronRuleGene = { neuronRuleGene(DecayRule()) }
    ) = linkedGeneGroup(
        targetName = target.name,
        targetSizeProvider = { (geneGroups.first { it.first == target.name }.second.group as NodeGeneGroup).chromosome.size },
        createDefault = defaultRule,
        applyOnExpress = { neuron, expressed ->
            (neuron as Neuron).updateRule = (expressed as NeuronRuleGeneWrapper).updateRule.copy()
        }
    )

    protected fun synapseRuleChromosome(
        target: KProperty0<ConnectionGeneGroup>,
        defaultRule: () -> SynapseRuleGene = { synapseRuleGene() }
    ) = linkedGeneGroup(
        targetName = target.name,
        targetSizeProvider = { (geneGroups.first { it.first == target.name }.second.group as ConnectionGeneGroup).chromosome.size },
        createDefault = defaultRule,
        applyOnExpress = { synapse, expressed ->
            (synapse as Synapse).learningRule = (expressed as SynapseRuleGeneWrapper).learningRule.copy()
        }
    )

    protected fun layoutChromosome(
        target: KProperty0<NodeGeneGroup>,
        default: () -> LayoutGene = { layoutGene() }
    ): PropertyDelegateProvider<Any?, GeneGroupDelegate<CollectionGeneGroup<LayoutGene>>> {
        val slot = CollectionGeneGroup(
            gene = default(),
            createDefault = default,
            applyToCollection = { neurons, expressed, _ ->
                (expressed as LayoutGeneWrapper).express().layoutNeurons(neurons.neuronList)
            }
        )
        return PropertyDelegateProvider { _, property ->
            collectionLinkedGroups.getOrPut(target.name) { mutableListOf() }.add(property.name)
            GeneGroupDelegate(slot).also { geneGroups.add(property.name to it) }
        }
    }

    protected fun connectionStrategyChromosome(
        target: KProperty0<NodeGeneGroup>,
        default: () -> ConnectionStrategyGene = { connectionStrategyGene() }
    ): PropertyDelegateProvider<Any?, GeneGroupDelegate<CollectionGeneGroup<ConnectionStrategyGene>>> {
        val slot = CollectionGeneGroup(
            gene = default(),
            createDefault = default,
            applyToCollection = { neurons, expressed, network ->
                (expressed as ConnectionStrategyGeneWrapper).connectionStrategy
                    .connectNeurons(neurons.neuronList, neurons.neuronList)
                    .addToNetworkAsync(network)
            }
        )
        return PropertyDelegateProvider { _, property ->
            collectionLinkedGroups.getOrPut(target.name) { mutableListOf() }.add(property.name)
            GeneGroupDelegate(slot).also { geneGroups.add(property.name to it) }
        }
    }

    private fun <G : Gene<Unit, *>> linkedGeneGroup(
        targetName: String,
        targetSizeProvider: () -> Int,
        createDefault: () -> G,
        applyOnExpress: (Any, Any?) -> Unit
    ): PropertyDelegateProvider<Any?, GeneGroupDelegate<LinkedGeneGroup<G>>> {
        val slot = LinkedGeneGroup(mutableListOf(), createDefault, applyOnExpress)
        return PropertyDelegateProvider { _, property ->
            linkedGroups.getOrPut(targetName) { mutableListOf() }.add(property.name)
            val currentSize = targetSizeProvider()
            repeat(currentSize) {
                slot.genes.add(slot.createDefault())
            }
            GeneGroupDelegate(slot).also { geneGroups.add(property.name to it) }
        }
    }

    // --- Gene addition (auto-adds to linked chromosomes) ---

    fun NodeGeneGroup.addGene(gene: NodeGene) {
        chromosome.add(gene)
        addLinkedDefaults(this)
    }

    fun ConnectionGeneGroup.addGene(gene: ConnectionGene) {
        chromosome.add(gene)
        addLinkedDefaults(this)
    }

    fun ConnectionGeneGroup.addConnection(
        vararg layerPairs: Pair<NodeGeneGroup, NodeGeneGroup>,
        init: Synapse.() -> Unit = {}
    ): ConnectionGene? {
        val gene = chromosome.createGene(
            *layerPairs.map { (s, t) -> s.chromosome to t.chromosome }.toTypedArray(),
            synapseGeneTemplate = init
        )
        if (gene != null) addLinkedDefaults(this)
        return gene
    }

    private fun addLinkedDefaults(slot: GeneGroup) {
        val slotName = geneGroups.firstOrNull { it.second.group === slot }?.first ?: return
        val pairedNames = linkedGroups[slotName] ?: return
        for (name in pairedNames) {
            val pairedSlot = geneGroups.first { it.first == name }.second.group as LinkedGeneGroup<*>
            @Suppress("UNCHECKED_CAST")
            (pairedSlot as LinkedGeneGroup<Gene<Unit, *>>).genes.add(pairedSlot.createDefault())
        }
    }

    // --- Auto-copy ---

    abstract fun createNew(seed: Long): Genotype

    abstract fun mutate()

    open fun copy(): Genotype {
        val new = createNew(random.nextLong())

        fun replaceSlot(name: String, copied: GeneGroup) {
            val target = new.geneGroups.first { it.first == name }.second
            @Suppress("UNCHECKED_CAST")
            (target as GeneGroupDelegate<GeneGroup>).group = copied
        }

        for ((name, delegate) in geneGroups.sortedBy { it.second.group.processingOrder }) {
            replaceSlot(name, delegate.group.copy())
        }
        return new
    }

    // --- Expression with linked chromosome resolution ---

    private fun resolveLinked(slotName: String): List<LinkedGeneGroup<*>> {
        val names = linkedGroups[slotName] ?: return emptyList()
        return names.map { name ->
            geneGroups.first { it.first == name }.second.group as LinkedGeneGroup<*>
        }
    }

    private fun resolveCollectionLinked(slotName: String): List<CollectionGeneGroup<*>> {
        val names = collectionLinkedGroups[slotName] ?: return emptyList()
        return names.map { name ->
            geneGroups.first { it.first == name }.second.group as CollectionGeneGroup<*>
        }
    }

    suspend fun expressAll(network: Network) {
        for ((name, delegate) in geneGroups.sortedBy { it.second.group.processingOrder }) {
            when (val slot = delegate.group) {
                is NodeGeneGroup -> slot.express(network, resolveLinked(name), resolveCollectionLinked(name))
                is ConnectionGeneGroup -> slot.express(network, resolveLinked(name))
            }
        }
    }
}
