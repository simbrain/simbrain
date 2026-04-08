package org.simbrain.util.geneticalgorithm

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.updaterules.DecayRule
import kotlin.random.Random

class SlotGeneticsTest {

    // --- Test genotypes ---

    class TestGenotype(seed: Long = Random.nextLong()) : SlotGenotype(seed) {
        val inputs by nodeChromosome(2) { clamped = true }
        val hidden by nodeChromosome(3)
        val outputs by nodeChromosome(1)
        val connections by connectionChromosome()

        init {
            connections.addConnection(inputs to hidden)
            connections.addConnection(hidden to outputs)
        }

        override fun createNew(seed: Long) = TestGenotype(seed)

        override fun mutate() {
            hidden.genes.forEach { it.mutate { bias += random.nextDouble(-1.0, 1.0) } }
            connections.genes.forEach { it.mutate { strength += random.nextDouble(-1.0, 1.0) } }
        }

        // Expose member extensions for testing from outside
        fun testAddHiddenGene(gene: NodeGene) = hidden.addGene(gene)
        fun testAddConnection() = connections.addConnection(inputs to hidden)
    }

    class LinkedTestGenotype(seed: Long = Random.nextLong()) : SlotGenotype(seed) {
        val hidden by nodeChromosome(3)
        val outputs by nodeChromosome(1)
        val connections by connectionChromosome()
        val hiddenRules by neuronRuleChromosome(::hidden)
        val connectionRules by synapseRuleChromosome(::connections)

        init {
            connections.addConnection(hidden to outputs)
        }

        override fun createNew(seed: Long) = LinkedTestGenotype(seed)

        override fun mutate() {
            hiddenRules.genes.forEach { it.mutateParam(mutateBounds = false) }
        }

        fun testAddHiddenGene(gene: NodeGene) = hidden.addGene(gene)
        fun testAddConnection() = connections.addConnection(hidden to outputs)
    }

    class MultiLinkedGenotype(seed: Long = Random.nextLong()) : SlotGenotype(seed) {
        val hidden by nodeChromosome(2)
        val connections by connectionChromosome()
        val hiddenRules by neuronRuleChromosome(::hidden)
        val hiddenLayout by layoutChromosome(::hidden)

        override fun createNew(seed: Long) = MultiLinkedGenotype(seed)
        override fun mutate() {}

        fun testAddHiddenGene(gene: NodeGene) = hidden.addGene(gene)
    }

    // ===== Slot declaration =====

    @Test
    fun `slots are registered in declaration order`() {
        val genotype = TestGenotype(42)
        val names = genotype.slotEntries.map { it.first }
        assertEquals(listOf("inputs", "hidden", "outputs", "connections"), names)
    }

    @Test
    fun `node chromosome creates correct number of genes`() {
        val genotype = TestGenotype(42)
        assertEquals(2, genotype.inputs.genes.size)
        assertEquals(3, genotype.hidden.genes.size)
        assertEquals(1, genotype.outputs.genes.size)
    }

    @Test
    fun `node gene init block is applied`() {
        val genotype = TestGenotype(42)
        assertTrue(genotype.inputs.genes.all { it.template.clamped })
        assertFalse(genotype.hidden.genes.any { it.template.clamped })
    }

    @Test
    fun `initial connections are created`() {
        val genotype = TestGenotype(42)
        assertEquals(2, genotype.connections.genes.size)
    }

    // ===== Expression =====

    @Test
    fun `expressAll creates neurons and synapses`() = runBlocking {
        val genotype = TestGenotype(42)
        val network = Network()
        genotype.expressAll(network)

        assertEquals(2, genotype.inputs.neurons.neuronList.size)
        assertEquals(3, genotype.hidden.neurons.neuronList.size)
        assertEquals(1, genotype.outputs.neurons.neuronList.size)
        assertEquals(2, genotype.connections.synapses.size)
    }

    @Test
    fun `neurons are not accessible before expression`() {
        val genotype = TestGenotype(42)
        assertThrows(IllegalStateException::class.java) {
            genotype.inputs.neurons
        }
    }

    // ===== Copy =====

    @Test
    fun `copyGenotype creates independent copy`() {
        val original = TestGenotype(42)
        original.mutate()
        val copy = original.copyGenotype() as TestGenotype

        assertEquals(original.hidden.genes.size, copy.hidden.genes.size)
        assertEquals(original.connections.genes.size, copy.connections.genes.size)

        val originalBias = original.hidden.genes[0].template.bias
        copy.mutate()
        assertEquals(originalBias, original.hidden.genes[0].template.bias)
    }

    @Test
    fun `copied connections reference copied nodes`() = runBlocking {
        val original = TestGenotype(42)
        val copy = original.copyGenotype() as TestGenotype

        val net1 = Network()
        val net2 = Network()
        original.expressAll(net1)
        copy.expressAll(net2)

        val originalNeurons = net1.flatNeuronList
        val copyNeurons = net2.flatNeuronList
        assertTrue(originalNeurons.none { it in copyNeurons })

        copy.connections.synapses.forEach { synapse ->
            assertTrue(synapse.source in copyNeurons)
            assertTrue(synapse.target in copyNeurons)
        }
    }

    // ===== addGene / addConnection =====

    @Test
    fun `addGene increases chromosome size`() {
        val genotype = TestGenotype(42)
        val sizeBefore = genotype.hidden.genes.size
        genotype.testAddHiddenGene(nodeGene())
        assertEquals(sizeBefore + 1, genotype.hidden.genes.size)
    }

    @Test
    fun `addConnection increases connection count`() {
        val genotype = TestGenotype(42)
        val sizeBefore = genotype.connections.genes.size
        genotype.testAddConnection()
        assertEquals(sizeBefore + 1, genotype.connections.genes.size)
    }

    @Test
    fun `genes list is read-only from outside`() {
        val genotype = TestGenotype(42)
        // genes returns List<NodeGene>, not MutableList
        val genes: List<NodeGene> = genotype.hidden.genes
        assertNotNull(genes)
    }

    // ===== Processing order =====

    @Test
    fun `connection slots have higher processing order than node slots`() {
        val genotype = TestGenotype(42)
        val nodeSlot = genotype.slotEntries.first { it.first == "hidden" }.second.slot
        val connSlot = genotype.slotEntries.first { it.first == "connections" }.second.slot
        assertTrue(nodeSlot.processingOrder < connSlot.processingOrder)
    }

    // ===== Linked Chromosomes =====

    @Test
    fun `linked chromosome is populated to match target size`() {
        val genotype = LinkedTestGenotype(42)
        assertEquals(genotype.hidden.genes.size, genotype.hiddenRules.genes.size)
        assertEquals(genotype.connections.genes.size, genotype.connectionRules.genes.size)
    }

    @Test
    fun `addGene auto-adds to linked chromosome`() {
        val genotype = LinkedTestGenotype(42)
        val rulesBefore = genotype.hiddenRules.genes.size
        genotype.testAddHiddenGene(nodeGene())
        assertEquals(rulesBefore + 1, genotype.hiddenRules.genes.size)
    }

    @Test
    fun `addConnection auto-adds to linked chromosome`() {
        val genotype = LinkedTestGenotype(42)
        val rulesBefore = genotype.connectionRules.genes.size
        genotype.testAddConnection()
        assertEquals(rulesBefore + 1, genotype.connectionRules.genes.size)
    }

    @Test
    fun `linked genes are typed`() {
        val genotype = LinkedTestGenotype(42)
        val rule: NeuronRuleGene = genotype.hiddenRules.genes[0]
        val synapseRule: SynapseRuleGene = genotype.connectionRules.genes[0]
        assertNotNull(rule.template)
        assertNotNull(synapseRule.template)
    }

    @Test
    fun `linked chromosomes are applied during expression`() = runBlocking {
        val genotype = LinkedTestGenotype(42)
        val network = Network()
        genotype.expressAll(network)

        genotype.hidden.neurons.neuronList.forEach { neuron ->
            assertTrue(neuron.updateRule is DecayRule)
        }
    }

    @Test
    fun `copy preserves linked chromosomes`() {
        val genotype = LinkedTestGenotype(42)
        genotype.testAddHiddenGene(nodeGene())
        val copy = genotype.copyGenotype() as LinkedTestGenotype

        assertEquals(genotype.hiddenRules.genes.size, copy.hiddenRules.genes.size)
        assertEquals(genotype.connectionRules.genes.size, copy.connectionRules.genes.size)
    }

    @Test
    fun `copied linked chromosomes are independent`() {
        val original = LinkedTestGenotype(42)
        val copy = original.copyGenotype() as LinkedTestGenotype

        copy.testAddHiddenGene(nodeGene())
        assertNotEquals(original.hiddenRules.genes.size, copy.hiddenRules.genes.size)
    }

    @Test
    fun `mutating linked genes on copy does not affect original`() {
        val original = LinkedTestGenotype(42)
        val copy = original.copyGenotype() as LinkedTestGenotype

        val originalRule = original.hiddenRules.genes[0].template.updateRule
        copy.mutate()
        assertEquals(originalRule, original.hiddenRules.genes[0].template.updateRule)
    }

    // ===== Multiple linked chromosomes on same target =====

    @Test
    fun `multiple per-element linked chromosomes sync on addGene`() {
        val genotype = MultiLinkedGenotype(42)
        val rulesBefore = genotype.hiddenRules.genes.size
        genotype.testAddHiddenGene(nodeGene())
        assertEquals(rulesBefore + 1, genotype.hiddenRules.genes.size)
    }

    @Test
    fun `layout is applied during expression`() = runBlocking {
        val genotype = MultiLinkedGenotype(42)
        val network = Network()
        genotype.expressAll(network)

        val positions = genotype.hidden.neurons.neuronList.map { it.x }
        assertTrue(positions.toSet().size > 1 || genotype.hidden.genes.size == 1)
    }
}
