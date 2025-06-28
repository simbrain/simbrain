package org.simbrain.network.core

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.Sparse
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.util.stats.distributions.UniformRealDistribution
import smile.math.matrix.Matrix

class SynapseGroupTestKt {

    lateinit var network: Network
    lateinit var n1: Neuron
    lateinit var n2: Neuron
    lateinit var n3: Neuron
    lateinit var sourceGroup: NeuronGroup
    lateinit var targetGroup: NeuronGroup

    // One source node, two target nodes
    @BeforeEach
    fun setup() {
        network = Network()
        n1 = Neuron().apply { network.addNetworkModel(this) }
        n2 = Neuron().apply { network.addNetworkModel(this) }
        n3 = Neuron().apply { network.addNetworkModel(this) }
        sourceGroup = NeuronGroup(listOf(n1)).apply { network.addNetworkModel(this) }
        targetGroup = NeuronGroup(listOf(n2, n3)).apply { network.addNetworkModel(this) }
    }

    @Test
    fun `create synapse group with AllToAll strategy`() {
        val sg = SynapseGroup(sourceGroup, targetGroup)
        assertEquals(2, sg.size())
    }

    @Test
    fun `initialization with manual synapse list succeeds`() {
        val syn = Synapse(n1, n2)
        val sg = SynapseGroup(sourceGroup, targetGroup, AllToAll(), mutableListOf(syn))
        assertEquals(1, sg.size())
        assertTrue(sg.synapses.contains(syn))
    }

    @Test
    fun `initialization fails when synapse neurons are not in groups`() {
        val external = Neuron()
        val badSynapse = Synapse(external, n2)
        assertThrows(IllegalArgumentException::class.java) {
            SynapseGroup(sourceGroup, targetGroup, AllToAll(), mutableListOf(badSynapse))
        }
    }

    @Test
    fun `applyConnectionStrategy replaces old synapses`() {
        val sg = SynapseGroup(sourceGroup, targetGroup)
        val originalSize = sg.size()
        sg.synapses.forEach { it.strength = 0.99 }
        runBlocking { sg.applyConnectionStrategy() }
        assertEquals(originalSize, sg.size())
        assertTrue(sg.synapses.none { it.strength == 0.99 }) // We expect new synapses
    }


    @Test
    fun `test isRecurrent false`() {
        val sg = SynapseGroup(sourceGroup, targetGroup)
        assertFalse(sg.isRecurrent())
    }

    @Test
    fun `test isRecurrent true`() {
        val sg = SynapseGroup(sourceGroup, sourceGroup)
        assertTrue(sg.isRecurrent())
    }

    @Test
    fun `test displaySynapses flag sets individual synapse visibility`() {
        val sg = SynapseGroup(sourceGroup, targetGroup)
        sg.displaySynapses = true
        assertTrue(sg.synapses.all { it.isVisible })
        sg.displaySynapses = false
        assertTrue(sg.synapses.none { it.isVisible })
    }

    @Test
    fun `test randomize updates weights`() {
        val sg = SynapseGroup(sourceGroup, targetGroup)
        sg.synapses.forEach { it.strength = 0.0 }
        sg.randomize(UniformRealDistribution(0.5, 1.0))
        assertTrue(sg.synapses.all { it.strength in 0.5..1.0 })
    }

    @Test
    fun `test get and set weight matrix`() {
        val sg = SynapseGroup(sourceGroup, targetGroup)
        val matrix = Matrix.of(arrayOf(doubleArrayOf(0.3), doubleArrayOf(0.7)))
        sg.setWeightMatrix(matrix)
        val weights = sg.getWeightMatrix()
        assertEquals(0.3, weights[0, 0], 1e-6)
        assertEquals(0.7, weights[1, 0], 1e-6)
    }

    @Test
    fun `clear resets all synapses`() {
        val sg = SynapseGroup(sourceGroup, targetGroup)
        sg.synapses.forEach { it.strength = 0.5 }
        sg.clear()
        assertTrue(sg.synapses.all { it.strength == 0.0 })
    }

    @Test
    fun `toggleClamping toggles synapse states`() {
        val sg = SynapseGroup(sourceGroup, targetGroup)
        val before = sg.synapses.map { it.clamped = true }
        sg.toggleClamping()
        sg.synapses.forEachIndexed { i, syn ->
            assertNotEquals(before[i], syn.clamped)
        }
    }

    @Test
    fun `changing connection strategy retains synapse group and applies new strategy`() {
        // Start with the default AllToAll‐built group
        val sg = SynapseGroup(sourceGroup, targetGroup)

        // All to all makes 2 synapses
        assertEquals(2, sg.size())

        sg.connectionStrategy = Sparse().apply {
            connectionDensity = .5
        }

        runBlocking { sg.applyConnectionStrategy() }

        // Synapse group still exists
        assertTrue(network.getModels<SynapseGroup>().contains(sg))

        // Sparse at 50% should make 1 synapse
        assertEquals(1, sg.size())

    }


    @Test
    fun `sparse connection strategy increasing and decreasing density works properly`() = runBlocking {
        // Create a larger source and target group for meaningful sparse connections
        val largeSourceGroup = NeuronGroup(List(10) { Neuron() }.also { network.addNetworkModels(it) })
            .apply { network.addNetworkModel(this) }
        val largeTargetGroup = NeuronGroup(List(10) { Neuron() }.also { network.addNetworkModels(it) })
            .apply { network.addNetworkModel(this) }

        // Start with 10% density
        val sparse = Sparse().apply {
            connectionDensity = 0.1
            allowSelfConnection = true
        }
        val sg = SynapseGroup(largeSourceGroup, largeTargetGroup, sparse)
        network.addNetworkModel(sg)

        // Initial connections should be 10% of 10x10 = 10 synapses
        assertEquals(10, sg.size())
        val originalSynapses = sg.synapses.toList()

        // Increase density to 20% and apply connection strategy
        sparse.connectionDensity = 0.2
        sg.applyConnectionStrategy()

        // Should now have 20 synapses total, with original 10 still present
        assertEquals(20, sg.size())
        assertTrue(originalSynapses.all { original ->
            sg.synapses.any { current ->
                current.source == original.source && current.target == original.target
            }
        })

        val synapses20Percent = sg.synapses.toList()

        // Decrease density to 5% and apply connection strategy
        sparse.connectionDensity = 0.05
        sg.applyConnectionStrategy()

        // Should now have 5 synapses total, all from the previous set
        assertEquals(5, sg.size())
        assertTrue(sg.synapses.all { current ->
            synapses20Percent.any { previous ->
                current.source == previous.source && current.target == previous.target
            }
        })
    }

    @Test
    fun `sparse synapse group produces same pattern as loose neurons with same seed`() = runBlocking {
        // Create neurons for loose connection test
        val looseSourceNeurons = List(5) { Neuron() }.also { network.addNetworkModels(it) }
        val looseTargetNeurons = List(5) { Neuron() }.also { network.addNetworkModels(it) }

        // Create neuron groups for synapse group test
        val sourceGroup = NeuronGroup(List(5) { Neuron() }.also { network.addNetworkModels(it) })
            .apply { network.addNetworkModel(this) }
        val targetGroup = NeuronGroup(List(5) { Neuron() }.also { network.addNetworkModels(it) })
            .apply { network.addNetworkModel(this) }

        // Use same seed for both approaches
        val seed = 42L
        val sparse1 = Sparse(seed = seed, connectionDensity = 0.4, allowSelfConnection = true)
        val sparse2 = Sparse(seed = seed, connectionDensity = 0.4, allowSelfConnection = true)

        // Create connections using loose neurons
        val looseSynapses = sparse1.connectNeurons(looseSourceNeurons, looseTargetNeurons)

        // Create synapse group with same strategy
        val sg = SynapseGroup(sourceGroup, targetGroup, sparse2)
        network.addNetworkModel(sg)

        // Both should produce the same number of synapses
        assertEquals(looseSynapses.size, sg.size())

        // The connection patterns should be equivalent when mapped by neuron index
        val looseConnections = looseSynapses.map {
            looseSourceNeurons.indexOf(it.source) to looseTargetNeurons.indexOf(it.target)
        }.toSet()

        val groupConnections = sg.synapses.map {
            sourceGroup.neuronList.indexOf(it.source) to targetGroup.neuronList.indexOf(it.target)
        }.toSet()

        assertEquals(looseConnections, groupConnections)
    }

}