package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.connections.*
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
        sg.applyConnectionStrategy()
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
        sg.applyConnectionStrategy()

        // Synapse group still exists
        assertTrue(network.getModels<SynapseGroup>().contains(sg))

        // Sparse at 50% should make 1 synapse
        assertEquals(1, sg.size())

    }



}