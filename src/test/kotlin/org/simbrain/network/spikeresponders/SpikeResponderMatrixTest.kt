package org.simbrain.network.spikeresponders

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.network.updaterules.SpikingThresholdRule
import org.simbrain.network.util.SpikingMatrixData
import smile.math.matrix.Matrix

/**
 * This will hold all matrix based spike responder tests since they all require the same setup.
 */
class SpikeResponderMatrixTest {

    val net = Network()
    val n1 = NeuronArray(2) // Input
    val n2 = NeuronArray(2) // spiking neurons
    val n3 = NeuronArray(3) // receives spike response
    val wm1 = WeightMatrix(n1, n2)
    val wm2 = WeightMatrix(n2, n3) // This one has the spike responder

    init {
        wm2.setWeights(
            arrayOf(
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(0.0, -1.0),
                doubleArrayOf(.5, .5)));
        n2.updateRule = SpikingThresholdRule()
        listOf(n1, n2, n3).forEach {
            it.clear()
        }
        net.addNetworkModelsAsync(n1, n2, n3, wm1, wm2)
    }

    @Test
    fun `neuron arrays and copies`() {
        // Change type of neuron array, copy, and create a spike responder
        val net2 = Network()
        val arr1 = NeuronArray(4) // Input
        arr1.updateRule = IntegrateAndFireRule()
        val arr2 = arr1.copy()
        val wmArr1Arr2 = WeightMatrix(arr1, arr2)
        wmArr1Arr2.spikeResponder = StepResponder()
        net2.addNetworkModelsAsync(arr1, arr2, wmArr1Arr2)
        net2.update() // Caused exceptions in earlier iterations.
        assertEquals(4, arr2.size)
        assertTrue(arr2.updateRule is IntegrateAndFireRule)
    }

    @Test
    fun `non-responder results in weight matrix times input vector`() {

        val nr = NonResponder()
        wm2.spikeResponder = nr

        n2.activations = Matrix.column(doubleArrayOf(1.0, 1.0))
        net.update()
        assertArrayEquals(doubleArrayOf(1.0, -1.0, 1.0), n3.activationArray)
    }

    @Test
    fun `test probabilistic response`() {

        val pr = StepResponder()
        pr.responseDuration = 1
        pr.spikeProbability = 1.0
        wm2.spikeResponder = pr
        n1.activations = Matrix.column(doubleArrayOf(1.0, 1.0))
        net.update()
        net.update() // extra update to propagate from layer 1 to 2
        assertArrayEquals(doubleArrayOf(1.0, -1.0, 1.0), n3.activationArray)

        pr.spikeProbability = 0.0
        n1.activations = Matrix.column(doubleArrayOf(1.0, 1.0))
        net.update()
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), n3.activationArray)
    }


    @Test
    fun `test step responder values before during and after its duration `() {

        val step = StepResponder()
        wm2.spikeResponder = step

        wm2.weights.mul(0.5)
        step.responseDuration = 3

        n1.activations = Matrix.column(doubleArrayOf(1.0, 0.0))
        net.update()
        assertArrayEquals(doubleArrayOf(1.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(booleanArrayOf(true, false), (n2.dataHolder as SpikingMatrixData).spikes)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.25), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.25), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.25), n3.activationArray, .001)
        net.update() // Step response ends
        assertArrayEquals(doubleArrayOf(0.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(booleanArrayOf(false, false), (n2.dataHolder as SpikingMatrixData).spikes)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), n3.activationArray, .001)
    }

    @Test
    fun `test step responder values with both nodes spiking`() {

        val step = StepResponder()
        wm2.spikeResponder = step

        step.responseDuration = 2

        n1.activations = Matrix.column(doubleArrayOf(1.0, 1.0))
        net.update()
        net.update()
        assertArrayEquals(doubleArrayOf(1.0,-1.0, 1.0), wm2.getSummedPSRs())
        assertArrayEquals(doubleArrayOf(1.0,-1.0, 1.0), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(1.0,-1.0, 1.0), wm2.getSummedPSRs())
        assertArrayEquals(doubleArrayOf(1.0,-1.0, 1.0), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), wm2.getSummedPSRs())
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), n3.activationArray, .001)
    }



    private fun recurrentIzhikevichArray(
        forceFullPsrMatrix: Boolean,
        responder: () -> SpikeResponder = { StepResponder(2) },
    ): Pair<Network, Pair<NeuronArray, WeightMatrix>> {
        val rng = kotlin.random.Random(5)
        val network = Network().apply { timeStep = 0.5 }
        val array = NeuronArray(60).apply {
            updateRule = org.simbrain.network.updaterules.IzhikevichRule().apply { backgroundCurrent = 6.0 }
            for (i in 0 until size) activations[i, 0] = rng.nextDouble(-70.0, 30.0)
        }
        val recurrent = WeightMatrix(array, array).apply {
            for (i in 0 until weights.nrow()) for (j in 0 until weights.ncol()) {
                weights[i, j] = if (rng.nextDouble() < 0.2) rng.nextDouble(-4.0, 8.0) else 0.0
            }
            spikeResponder = responder().apply {
                // At probability one the probabilistic path behaves identically but writes the whole PSR matrix
                if (forceFullPsrMatrix) useSpikeProbability = true
            }
        }
        network.addNetworkModelsAsync(array, recurrent)
        return network to (array to recurrent)
    }

    @Test
    fun `summing a step response from the responding sources matches writing the whole psr matrix`() {
        val (fastNetwork, fast) = recurrentIzhikevichArray(forceFullPsrMatrix = false)
        val (fullNetwork, full) = recurrentIzhikevichArray(forceFullPsrMatrix = true)
        var spikes = 0
        repeat(400) { step ->
            fastNetwork.update()
            fullNetwork.update()
            assertArrayEquals(full.first.activations.toArray().flatMap { it.asList() }.toDoubleArray(),
                fast.first.activations.toArray().flatMap { it.asList() }.toDoubleArray(), 0.0, "step $step")
            spikes += (fast.first.dataHolder as SpikingMatrixData).spikes.count { it }
        }
        assertTrue(spikes > 100, "the network should spike, spiked $spikes times")
        assertArrayEquals(full.second.psrMatrix.toArray().flatMap { it.asList() }.toDoubleArray(),
            fast.second.psrMatrix.toArray().flatMap { it.asList() }.toDoubleArray(), 0.0)
    }

    private fun assertSameSpiking(responder: () -> SpikeResponder, switchToLearningAt: Int? = null) {
        val (fastNetwork, fast) = recurrentIzhikevichArray(forceFullPsrMatrix = false, responder)
        val (fullNetwork, full) = recurrentIzhikevichArray(forceFullPsrMatrix = true, responder)
        var spikes = 0
        repeat(300) { step ->
            if (step == switchToLearningAt) {
                // A learning rule hands the response back to the full PSR matrix, continuing from the trace
                listOf(fast.second, full.second).forEach { it.learningRule = org.simbrain.network.learningrules.HebbianRule().apply { learningRate = 0.0 } }
            }
            fastNetwork.update()
            fullNetwork.update()
            val fastSpikes = (fast.first.dataHolder as SpikingMatrixData).spikes
            assertArrayEquals((full.first.dataHolder as SpikingMatrixData).spikes, fastSpikes, "spikes at step $step")
            spikes += fastSpikes.count { it }
            val expected = full.first.activations.toArray().flatMap { it.asList() }
            val actual = fast.first.activations.toArray().flatMap { it.asList() }
            expected.zip(actual).forEach { (e, a) -> assertEquals(e, a, 1e-9 * maxOf(1.0, kotlin.math.abs(e)), "step $step") }
        }
        assertTrue(spikes > 100, "the network should spike, spiked $spikes times")
        val expectedPsr = full.second.psrMatrix.toArray().flatMap { it.asList() }
        val actualPsr = fast.second.psrMatrix.toArray().flatMap { it.asList() }
        expectedPsr.zip(actualPsr).forEach { (e, a) -> assertEquals(e, a, 1e-9 * maxOf(1.0, kotlin.math.abs(e))) }
    }

    @Test
    fun `a jump and decay trace per source matches the full psr matrix`() {
        assertSameSpiking({ JumpAndDecay().apply { timeConstant = 4.0 } })
    }

    @Test
    fun `a convolved jump and decay trace matches the full psr matrix`() {
        assertSameSpiking({ JumpAndDecay().apply { timeConstant = 4.0; useConvolution = true } })
    }

    @Test
    fun `switching to a learning rule continues jump and decay from the trace`() {
        assertSameSpiking({ JumpAndDecay().apply { timeConstant = 4.0 } }, switchToLearningAt = 150)
    }

    /**
     * Input array -> spiking array -> output array, with a jump and decay responder on the second weight matrix.
     * Spike probability is on (but 1) for [forceFullPsrMatrix], which keeps the responder off the trace path.
     */
    private fun jumpAndDecayChain(forceFullPsrMatrix: Boolean): Triple<Network, NeuronArray, WeightMatrix> {
        val network = Network()
        val input = NeuronArray(2)
        val spiking = NeuronArray(2).apply { updateRule = SpikingThresholdRule() }
        val output = NeuronArray(3)
        val responding = WeightMatrix(spiking, output).apply {
            setWeights(arrayOf(doubleArrayOf(1.0, 0.0), doubleArrayOf(0.0, -1.0), doubleArrayOf(.5, .5)))
            spikeResponder = JumpAndDecay().apply {
                timeConstant = 4.0
                if (forceFullPsrMatrix) useSpikeProbability = true
            }
        }
        listOf(input, spiking, output).forEach { it.clear() }
        network.addNetworkModelsAsync(input, spiking, output, WeightMatrix(input, spiking), responding)
        return Triple(network, input, responding)
    }

    private fun spikeFirstSource(vararg chains: Triple<Network, NeuronArray, WeightMatrix>) =
        chains.forEach { (network, input, _) ->
            input.activations = Matrix.column(doubleArrayOf(1.0, 0.0))
            network.update()
            network.update()
        }

    @Test
    fun `editing weights mid-response rescales a jump and decay trace but not the full psr matrix`() {
        val fast = jumpAndDecayChain(forceFullPsrMatrix = false)
        val full = jumpAndDecayChain(forceFullPsrMatrix = true)
        fun spikeFirstSource() = spikeFirstSource(fast, full)
        fun step() = listOf(fast, full).forEach { it.first.update() }

        spikeFirstSource()
        repeat(3) { step() }
        assertArrayEquals(full.third.getSummedPSRs(), fast.third.getSummedPSRs(), 1e-12)
        assertTrue(full.third.getSummedPSRs()[0] in 0.1..0.99, "the response should be partly decayed")
        listOf(fast, full).forEach { it.third.weights.mul(2.0) }
        step()
        // The trace path computes each response as weight times trace, so the decaying response follows the edit at
        // once; the full PSR matrix keeps decaying from the response made with the old weights
        val doubled = full.third.getSummedPSRs().map { it * 2 }.toDoubleArray()
        assertArrayEquals(doubled, fast.third.getSummedPSRs(), 1e-12)
        // The next spike jumps both to the new weights, so they agree again
        spikeFirstSource()
        assertArrayEquals(full.third.getSummedPSRs(), fast.third.getSummedPSRs(), 1e-12)
        assertEquals(2.0, fast.third.getSummedPSRs()[0], 1e-12)
    }

    @Test
    fun `an edited psr matrix replaces a jump and decay trace`() {
        val fast = jumpAndDecayChain(forceFullPsrMatrix = false)
        val full = jumpAndDecayChain(forceFullPsrMatrix = true)
        spikeFirstSource(fast, full)
        repeat(2) { listOf(fast, full).forEach { it.first.update() } }
        assertTrue(fast.third.getSummedPSRs()[0] > 0.1, "the response should still be decaying")
        listOf(fast, full).forEach { it.third.psrMatrix = Matrix(3, 2) }
        listOf(fast, full).forEach { it.first.update() }
        assertArrayEquals(DoubleArray(3), full.third.getSummedPSRs(), 0.0)
        assertArrayEquals(DoubleArray(3), fast.third.getSummedPSRs(), 0.0)
    }

    @Test
    fun `a jump and decay trace restarts from a decaying psr matrix`() {
        val fast = jumpAndDecayChain(forceFullPsrMatrix = false)
        val full = jumpAndDecayChain(forceFullPsrMatrix = true)
        fun step() = listOf(fast, full).forEach { it.first.update() }
        spikeFirstSource(fast, full)
        // A learning rule hands the response to the full PSR matrix while it is still decaying
        fast.third.learningRule = org.simbrain.network.learningrules.HebbianRule().apply { learningRate = 0.0 }
        step()
        assertFalse((fast.third.spikeResponseData as JumpAndDecayMatrixData).traceValid)
        fast.third.learningRule = org.simbrain.network.learningrules.StaticSynapseRule()
        step()
        assertTrue((fast.third.spikeResponseData as JumpAndDecayMatrixData).traceValid)
        repeat(5) {
            step()
            assertArrayEquals(full.third.getSummedPSRs(), fast.third.getSummedPSRs(), 1e-12)
        }
        assertTrue(full.third.getSummedPSRs()[0] in 0.01..0.99, "the response should be partly decayed")
    }
}
