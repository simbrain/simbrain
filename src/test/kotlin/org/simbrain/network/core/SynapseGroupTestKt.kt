package org.simbrain.network.core

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.Sparse
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.spikeresponders.RiseAndDecay
import org.simbrain.network.spikeresponders.SpikeResponder
import org.simbrain.network.updaterules.SpikingThresholdRule
import org.simbrain.util.stats.distributions.UniformRealDistribution
import smile.math.matrix.Matrix

class SynapseGroupTestKt {

    private val network: Network
    private val n1: Neuron
    private val n2: Neuron
    private val n3: Neuron
    private val sourceGroup: NeuronCollection
    private val targetGroup: NeuronCollection

    init {
        network = Network()
        n1 = Neuron().apply { network.addNetworkModelAsync(this) }
        n2 = Neuron().apply { network.addNetworkModelAsync(this) }
        n3 = Neuron().apply { network.addNetworkModelAsync(this) }
        sourceGroup = NeuronCollection(listOf(n1)).apply { network.addNetworkModelAsync(this) }
        targetGroup = NeuronCollection(listOf(n2, n3)).apply { network.addNetworkModelAsync(this) }
    }

    private fun createParallelSynapses(
        srcActivation: Double = 0.0,
        tgtActivation: Double = 0.0,
        strength: Double = 0.5
    ): Pair<Synapse, Synapse> {
        val freeSrc = Neuron().apply {
            network.addNetworkModelAsync(this)
            activation = srcActivation
        }
        val freeTgt = Neuron().apply {
            network.addNetworkModelAsync(this)
            activation = tgtActivation
        }
        val freeSynapse = Synapse(freeSrc, freeTgt).apply {
            network.addNetworkModelAsync(this)
            forceSetStrength(strength)
        }

        val groupSrc = Neuron().apply {
            network.addNetworkModelAsync(this)
            activation = srcActivation
        }
        val groupTgt = Neuron().apply {
            network.addNetworkModelAsync(this)
            activation = tgtActivation
        }
        val srcGroup = NeuronCollection(listOf(groupSrc)).apply { network.addNetworkModelAsync(this) }
        val tgtGroup = NeuronCollection(listOf(groupTgt)).apply { network.addNetworkModelAsync(this) }
        val sg = SynapseGroup(srcGroup, tgtGroup).apply { network.addNetworkModelAsync(this) }
        val groupSynapse = sg.synapses.first().apply {
            forceSetStrength(strength)
        }

        return freeSynapse to groupSynapse
    }

    private fun createSpikeChains(
        spikeResponder: SpikeResponder,
        delay: Int = 0
    ): Pair<Neuron, Neuron> {
        val freeInput = Neuron().apply {
            network.addNetworkModelAsync(this)
            clamped = true
            activation = 1.0
        }
        val freeSpiker = Neuron(SpikingThresholdRule()).apply {
            network.addNetworkModelAsync(this)
        }
        val freeTarget = Neuron().apply {
            network.addNetworkModelAsync(this)
            upperBound = 10.0
        }
        Synapse(freeInput, freeSpiker).apply { network.addNetworkModelAsync(this) }
        Synapse(freeSpiker, freeTarget).apply {
            network.addNetworkModelAsync(this)
            this.spikeResponder = spikeResponder
            strength = 1.0
            this.delay = delay
        }

        val groupInput = Neuron().apply {
            network.addNetworkModelAsync(this)
            clamped = true
            activation = 1.0
        }
        // Use NeuronCollection (not NeuronGroup) so neurons are updated individually,
        // matching the free neuron timing in buffered update.
        // NeuronGroup has its own update() that changes accumulate/update ordering.
        val groupSpiker = Neuron(SpikingThresholdRule()).apply {
            network.addNetworkModelAsync(this)
        }
        val groupTarget = Neuron().apply {
            network.addNetworkModelAsync(this)
            upperBound = 10.0
        }
        val spikerGroup = NeuronCollection(listOf(groupSpiker)).apply { network.addNetworkModelAsync(this) }
        val targetGroup = NeuronCollection(listOf(groupTarget)).apply { network.addNetworkModelAsync(this) }

        Synapse(groupInput, groupSpiker).apply { network.addNetworkModelAsync(this) }
        val sg = SynapseGroup(spikerGroup, targetGroup).apply { network.addNetworkModelAsync(this) }
        sg.synapses.first().apply {
            this.spikeResponder = spikeResponder.copy()
            strength = 1.0
            this.delay = delay
        }

        return freeTarget to groupTarget
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
        network.addNetworkModelAsync(sg)

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
        val largeSourceGroup = NeuronCollection(List(10) { Neuron() }.also { network.addNetworkModelsAsync(it) })
            .apply { network.addNetworkModelAsync(this) }
        val largeTargetGroup = NeuronCollection(List(10) { Neuron() }.also { network.addNetworkModelsAsync(it) })
            .apply { network.addNetworkModelAsync(this) }

        // Start with 10% density
        val sparse = Sparse().apply {
            connectionDensity = 0.1
            allowSelfConnection = true
        }
        val sg = SynapseGroup(largeSourceGroup, largeTargetGroup, sparse)
        network.addNetworkModelAsync(sg)

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
        val looseSourceNeurons = List(5) { Neuron() }.also { network.addNetworkModelsAsync(it) }
        val looseTargetNeurons = List(5) { Neuron() }.also { network.addNetworkModelsAsync(it) }

        // Create neuron groups for synapse group test
        val sourceGroup = NeuronCollection(List(5) { Neuron() }.also { network.addNetworkModelsAsync(it) })
            .apply { network.addNetworkModelAsync(this) }
        val targetGroup = NeuronCollection(List(5) { Neuron() }.also { network.addNetworkModelsAsync(it) })
            .apply { network.addNetworkModelAsync(this) }

        // Use same seed for both approaches
        val seed = 42L
        val sparse1 = Sparse(seed = seed, connectionDensity = 0.4, allowSelfConnection = true)
        val sparse2 = Sparse(seed = seed, connectionDensity = 0.4, allowSelfConnection = true)

        // Create connections using loose neurons
        val looseSynapses = sparse1.connectNeurons(looseSourceNeurons, looseTargetNeurons)

        // Create synapse group with same strategy
        val sg = SynapseGroup(sourceGroup, targetGroup, sparse2)
        network.addNetworkModelAsync(sg)

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

    @Test
    fun `synapse group delays should behave identically to free synapses`() {
        val (freeSynapse, groupSynapse) = createParallelSynapses(strength = 1.0)
        val freeSrc = freeSynapse.source
        val groupSrc = groupSynapse.source

        freeSynapse.delay = 2
        groupSynapse.delay = 2

        listOf(1.0, 2.0, 3.0, 4.0, 5.0).forEach { value ->
            freeSrc.activation = value
            groupSrc.activation = value

            with(network) {
                freeSynapse.updatePSR()
                groupSynapse.updatePSR()
            }

            assertEquals(freeSynapse.psr, groupSynapse.psr, 0.001)
        }
    }

    @Test
    fun `synapse group clear should reset delay manager like free`() {
        val (freeSynapse, groupSynapse) = createParallelSynapses(srcActivation = 5.0, strength = 1.0)

        freeSynapse.delay = 2
        groupSynapse.delay = 2

        with(network) {
            freeSynapse.updatePSR()
            groupSynapse.updatePSR()
            freeSynapse.updatePSR()
            groupSynapse.updatePSR()
        }

        freeSynapse.clear()
        groupSynapse.clear()

        with(network) {
            freeSynapse.updatePSR()
            groupSynapse.updatePSR()
        }

        assertEquals(0.0, groupSynapse.psr, 0.001)
        assertEquals(freeSynapse.psr, groupSynapse.psr, 0.001)
    }

    @Test
    fun `synapse group spike responders should behave identically to free`() {
        val (freeTarget, groupTarget) = createSpikeChains(JumpAndDecay())

        repeat(5) { iteration ->
            network.update()
            assertEquals(freeTarget.activation, groupTarget.activation, 0.001,
                "Iteration $iteration: expected ${freeTarget.activation}, got ${groupTarget.activation}")
        }
    }

    @Test
    fun `synapse group learning rules should update identically to free`() {
        val (freeSynapse, groupSynapse) = createParallelSynapses(
            srcActivation = 0.5,
            tgtActivation = 0.8,
            strength = 0.5
        )

        freeSynapse.learningRule = HebbianRule()
        groupSynapse.learningRule = HebbianRule()

        repeat(5) {
            with(network) {
                freeSynapse.learningRule.apply(freeSynapse, freeSynapse.learningRuleData)
                groupSynapse.learningRule.apply(groupSynapse, groupSynapse.learningRuleData)
            }
            assertEquals(freeSynapse.strength, groupSynapse.strength, 0.001)
        }
    }

    @Test
    fun `synapse group clamping should prevent updates like free`() {
        val (freeSynapse, groupSynapse) = createParallelSynapses(
            srcActivation = 0.5,
            tgtActivation = 0.8,
            strength = 0.5
        )

        freeSynapse.learningRule = HebbianRule()
        freeSynapse.clamped = true
        groupSynapse.learningRule = HebbianRule()
        groupSynapse.clamped = true

        repeat(5) {
            network.update()
        }

        assertEquals(0.5, freeSynapse.strength, 0.001)
        assertEquals(0.5, groupSynapse.strength, 0.001)
    }

    @Test
    fun `synapse group with different spike responders should match free`() {
        val (freeTarget, groupTarget) = createSpikeChains(RiseAndDecay())

        repeat(8) { iteration ->
            network.update()
            assertEquals(freeTarget.activation, groupTarget.activation, 0.001,
                "Iteration $iteration: expected ${freeTarget.activation}, got ${groupTarget.activation}")
        }
    }

    @Test
    fun `synapse group with delays and spike responders should match free`() {
        val (freeTarget, groupTarget) = createSpikeChains(JumpAndDecay(), delay = 2)

        repeat(10) { iteration ->
            network.update()
            assertEquals(freeTarget.activation, groupTarget.activation, 0.001,
                "Iteration $iteration: expected ${freeTarget.activation}, got ${groupTarget.activation}")
        }
    }

    @Test
    fun `synapse group polarity constraints should match free`() {
        val (freeSynapse, groupSynapse) = createParallelSynapses()

        freeSynapse.source.polarity = org.simbrain.util.SimbrainConstants.Polarity.EXCITATORY
        groupSynapse.source.polarity = org.simbrain.util.SimbrainConstants.Polarity.EXCITATORY

        freeSynapse.strength = -5.0
        groupSynapse.strength = -5.0

        assertTrue(freeSynapse.strength >= 0.0)
        assertTrue(groupSynapse.strength >= 0.0)
        assertEquals(freeSynapse.strength, groupSynapse.strength, 0.001)
    }

    @Test
    fun `synapse group increment should work identically to free`() {
        val (freeSynapse, groupSynapse) = createParallelSynapses(strength = 1.0)

        freeSynapse.increment = 0.5
        groupSynapse.increment = 0.5

        freeSynapse.increment()
        groupSynapse.increment()

        assertEquals(1.5, freeSynapse.strength, 0.001)
        assertEquals(1.5, groupSynapse.strength, 0.001)

        freeSynapse.decrement()
        groupSynapse.decrement()

        assertEquals(1.0, freeSynapse.strength, 0.001)
        assertEquals(1.0, groupSynapse.strength, 0.001)
    }

    @Test
    fun `refreshVisibility honors a manual override but otherwise tracks the threshold`() {
        val sg = SynapseGroup(sourceGroup, targetGroup) // 1x2 = 2 synapses, below the visibility threshold

        // Manually pin it collapsed; auto-recompute must not override that.
        sg.autoVisibility = false
        sg.displaySynapses = false
        sg.refreshVisibility()
        assertFalse(sg.displaySynapses, "a manually pinned visibility must not be overwritten by refreshVisibility")

        // With auto-tracking on, a below-threshold group expands.
        sg.autoVisibility = true
        sg.refreshVisibility()
        assertTrue(sg.displaySynapses, "with autoVisibility, a below-threshold group auto-expands")
    }

}