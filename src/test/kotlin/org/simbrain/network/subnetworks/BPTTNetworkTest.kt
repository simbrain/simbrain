/**
 * Tests for [BPTTNetwork] and [BPTTTrainer].
 *
 * The equivalence test is the important one: at truncation depth 1 a BPTT network computes exactly
 * what an SRN computes, because both stop the gradient after a single step. That pins the claim the
 * two subnetworks are built to contrast.
 */
package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.gui.nodes.BPTTUnrolledView
import org.simbrain.network.trainers.BPTTTrainer
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.createDiagonalDataset
import org.simbrain.util.copy
import org.simbrain.util.toDoubleArray
import smile.math.matrix.Matrix
import kotlin.math.abs
import kotlin.random.Random

class BPTTNetworkTest {

    @Test
    fun `test bptt training`() {
        val net = Network()
        val bptt = BPTTNetwork(10, 5, 10).apply { label = "BPTT" }
        net.addNetworkModelsAsync(bptt)
        bptt.trainingSet = createDiagonalDataset(10, 10, shiftAmount = 1)
        with(net) {
            bptt.randomize()
            bptt.update()
            bptt.trainerConfig.learningRate = 0.01

            val trainer = BPTTTrainer(net, bptt)
            runBlocking {
                repeat(1500) {
                    trainer.trainOnce()
                }
            }
            assertTrue(trainer.lastTrainingError < 0.1) { "Error too high: ${trainer.lastTrainingError}" }
        }
    }

    @Test
    fun `bptt at truncation depth one matches an srn given the same weights`() {
        val net = Network()
        val srn = SRNNetwork(4, 3, 4)
        val bptt = BPTTNetwork(4, 3, 4)
        net.addNetworkModelsAsync(srn, bptt)

        val random = Random(1234)
        fun randomize(matrix: Matrix) {
            for (i in 0 until matrix.nrow()) {
                for (j in 0 until matrix.ncol()) {
                    matrix[i, j] = random.nextDouble(-1.0, 1.0)
                }
            }
        }

        listOf(srn.wmList[0].weights, srn.wmList[1].weights, srn.contextToHidden.weights).forEach { randomize(it) }
        listOf(srn.hiddenLayer, srn.outputLayer).forEach { layer ->
            layer.biases = Matrix.column(DoubleArray(layer.size) { random.nextDouble(-0.5, 0.5) })
        }

        // The SRN's context-to-hidden matrix and the BPTT network's self-connection play the same
        // role, so they start from the same values.
        bptt.wmList[0].setMatrixValues(srn.wmList[0].weights.clone())
        bptt.wmList[1].setMatrixValues(srn.wmList[1].weights.clone())
        bptt.hiddenToHidden.setMatrixValues(srn.contextToHidden.weights.clone())
        bptt.hiddenLayer.biases = srn.hiddenLayer.biases.clone()
        bptt.outputLayer.biases = srn.outputLayer.biases.clone()

        val dataset = createDiagonalDataset(4, 4, shiftAmount = 1)
        srn.trainingSet = dataset.copy()
        bptt.trainingSet = dataset.copy()

        srn.trainerConfig.learningRate = 0.01
        bptt.trainerConfig.learningRate = 0.01
        bptt.trainerConfig.truncationDepth = 1

        val srnTrainer = SupervisedTrainer(net, srn)
        val bpttTrainer = BPTTTrainer(net, bptt)

        runBlocking {
            repeat(50) {
                // BPTT clears its recurrent memory at the start of every pass over the sequence; an
                // SRN carries whatever the last pass left behind, so match it up by hand.
                srn.hiddenLayer.activations = Matrix(srn.hiddenLayer.size, 1)
                srnTrainer.trainOnce()
                bpttTrainer.trainOnce()
            }
        }

        assertEquals(srnTrainer.lastTrainingError, bpttTrainer.lastTrainingError, 1e-10) {
            "Training error should track an SRN's at truncation depth 1"
        }

        fun assertMatricesEqual(expected: Matrix, actual: Matrix, what: String) {
            for (i in 0 until expected.nrow()) {
                for (j in 0 until expected.ncol()) {
                    assertEquals(expected[i, j], actual[i, j], 1e-10) { "$what differs at ($i, $j)" }
                }
            }
        }

        assertMatricesEqual(srn.wmList[0].weights, bptt.wmList[0].weights, "Input to hidden weights")
        assertMatricesEqual(srn.wmList[1].weights, bptt.wmList[1].weights, "Hidden to output weights")
        assertMatricesEqual(srn.contextToHidden.weights, bptt.hiddenToHidden.weights, "Recurrent weights")
        assertMatricesEqual(srn.hiddenLayer.biases, bptt.hiddenLayer.biases, "Hidden biases")
        assertMatricesEqual(srn.outputLayer.biases, bptt.outputLayer.biases, "Output biases")
    }

    @Test
    fun `truncation depth changes what the network learns`() {
        fun trainWithDepth(depth: Int): Matrix {
            val net = Network()
            val bptt = BPTTNetwork(4, 3, 4)
            net.addNetworkModelsAsync(bptt)

            val random = Random(99)
            listOf(bptt.wmList[0].weights, bptt.wmList[1].weights, bptt.hiddenToHidden.weights).forEach { matrix ->
                for (i in 0 until matrix.nrow()) {
                    for (j in 0 until matrix.ncol()) {
                        matrix[i, j] = random.nextDouble(-1.0, 1.0)
                    }
                }
            }

            bptt.trainingSet = createDiagonalDataset(4, 4, shiftAmount = 1)
            bptt.trainerConfig.learningRate = 0.01
            bptt.trainerConfig.truncationDepth = depth

            val trainer = BPTTTrainer(net, bptt)
            runBlocking { repeat(20) { trainer.trainOnce() } }
            return bptt.hiddenToHidden.weights.clone()
        }

        val shallow = trainWithDepth(1)
        val deep = trainWithDepth(4)

        val largestDifference = (0 until shallow.nrow()).flatMap { i ->
            (0 until shallow.ncol()).map { j -> abs(shallow[i, j] - deep[i, j]) }
        }.max()

        assertTrue(largestDifference > 1e-6) {
            "Recurrent weights came out the same at depth 1 and depth 4, so truncation depth is not wired through"
        }
    }

    @Test
    fun `a bptt network asks for the time-unrolled trainer`() {
        val net = Network()
        val bptt = BPTTNetwork(4, 3, 4)
        val backprop = BackpropNetwork(intArrayOf(4, 3, 4), null)
        net.addNetworkModelsAsync(bptt, backprop)

        // The shared training dialog builds its trainer through this hook, so a wrong answer here
        // silently trains the network as though it had no recurrent connection.
        assertTrue(bptt.createTrainer(net) is BPTTTrainer)
        assertFalse(backprop.createTrainer(net) is BPTTTrainer)
    }

    @Test
    fun `training publishes a full window even when the sequence ends on a short one`() = runBlocking {
        val net = Network()
        // Five rows at depth four splits into windows of four and one. Publishing only the trailing
        // window would blank all but the newest unrolled column, even though the steps that fill the
        // rest are known: memory carries across the truncation boundary, so the windows are contiguous.
        val bptt = BPTTNetwork(5, 4, 5)
        net.addNetworkModelsAsync(bptt)
        bptt.trainerConfig.truncationDepth = 4
        bptt.unrolledView = true

        BPTTTrainer(net, bptt).trainOnce()

        assertEquals(5, bptt.trainingSet.size)
        assertEquals(4, bptt.unrolledActivations.size) {
            "Expected four steps of history, not just the trailing single step window"
        }
    }

    @Test
    fun `the newest recorded step is the one the layers are left holding`() = runBlocking {
        val net = Network()
        val bptt = BPTTNetwork(5, 4, 5)
        net.addNetworkModelsAsync(bptt)
        bptt.trainerConfig.truncationDepth = 4
        bptt.unrolledView = true

        BPTTTrainer(net, bptt).trainOnce()

        // The whole look-back drawing rests on this. The rolled network renders its own activations and
        // stands for the last step, so if the history ended anywhere other than where the layers actually
        // are, the columns and the network beside them would be showing different moments in time.
        val newest = bptt.unrolledActivations.last()
        listOf(bptt.inputLayer, bptt.hiddenLayer, bptt.outputLayer).forEach { layer ->
            assertArrayEquals(layer.activations.toDoubleArray(), newest[layer]?.toDoubleArray(), 1e-12) {
                "${layer.label} disagrees with the last published step"
            }
        }
    }

    @Test
    fun `ordinary iteration records one step at a time and keeps only the window`() = runBlocking {
        val net = Network()
        val bptt = BPTTNetwork(5, 4, 5)
        net.addNetworkModelsAsync(bptt)
        bptt.trainerConfig.truncationDepth = 3
        bptt.unrolledView = true

        // Training is not the only thing that advances the network. A workspace tick and the training
        // dialog's apply-row button both land in forwardPass, and each computes a single step, so the
        // history has to accumulate rather than arrive all at once.
        with(net) {
            bptt.inputLayer.setActivations(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0))
            bptt.forwardPass()
            assertEquals(1, bptt.unrolledActivations.size)
            bptt.forwardPass()
            assertEquals(2, bptt.unrolledActivations.size)

            repeat(5) { bptt.forwardPass() }
            assertEquals(3, bptt.unrolledActivations.size) {
                "History should be capped at the truncation depth rather than growing without bound"
            }

            val newest = bptt.unrolledActivations.last()
            assertArrayEquals(
                bptt.hiddenLayer.activations.toDoubleArray(),
                newest[bptt.hiddenLayer]?.toDoubleArray(),
                1e-12
            ) { "The last recorded step should be the one just computed" }
        }
    }

    @Test
    fun `activations are only collected while something is drawing them`() = runBlocking {
        val net = Network()
        val bptt = BPTTNetwork(5, 4, 5)
        net.addNetworkModelsAsync(bptt)

        BPTTTrainer(net, bptt).trainOnce()
        with(net) { repeat(3) { bptt.forwardPass() } }

        assertTrue(bptt.unrolledActivations.isEmpty()) {
            "Per-timestep activations should not be gathered when the unrolled view is off"
        }
    }

    @Test
    fun `the unrolled view draws a column and a full set of arrows for every step before the last`() = runBlocking {
        val net = Network()
        val bptt = BPTTNetwork(4, 3, 4)
        net.addNetworkModelsAsync(bptt)
        bptt.trainerConfig.truncationDepth = 4

        // Stands in for the rolled network's measured extent, which on the canvas comes from the
        // subnetwork outline. Only the count of columns and arrows is under test, not where they land.
        val view = BPTTUnrolledView(bptt) { bptt.hiddenLayer.location.x - 200.0 }
        assertEquals(3, view.columns.size) { "Four steps means three columns plus the rolled network" }
        assertEquals(4, view.stepCount) { "The rolled network is the fourth step, not a fifth one" }

        // Every connection has to be tagged with the weights it stands for, or hovering that matrix
        // would light nothing.
        BPTTUnrolledView.SharedWeights.entries.forEach { weights ->
            assertEquals(3, view.arrowCount(weights)) { "Expected one $weights arrow per column" }
        }

        bptt.trainerConfig.truncationDepth = 1
        view.rebuild()
        assertEquals(0, view.columns.size) { "At depth one there is nothing to unroll" }
    }

    @Test
    fun `every drawn arrow carries a picture of the matrix it applies`() = runBlocking {
        val net = Network()
        val bptt = BPTTNetwork(4, 3, 4)
        net.addNetworkModelsAsync(bptt)
        bptt.trainerConfig.truncationDepth = 4

        val view = BPTTUnrolledView(bptt) { bptt.hiddenLayer.location.x - 200.0 }

        // An arrow without its matrix reads as a bare connection while its neighbours are labelled, which
        // is the inconsistency the copies exist to remove. The one exception is the arrow into the live
        // column: the real recurrent node sits along it once its loop is hidden, so a copy there would
        // draw the same matrix twice.
        assertEquals(3, view.matrixImageCount(BPTTUnrolledView.SharedWeights.INPUT_TO_HIDDEN))
        assertEquals(3, view.matrixImageCount(BPTTUnrolledView.SharedWeights.HIDDEN_TO_OUTPUT))
        assertEquals(
            view.arrowCount(BPTTUnrolledView.SharedWeights.RECURRENT) - 1,
            view.matrixImageCount(BPTTUnrolledView.SharedWeights.RECURRENT)
        ) { "Every recurrent arrow but the one the real node covers should carry a copy" }
    }

    @Test
    fun `test BPTT serialization`() {
        val net = Network()
        val bptt = BPTTNetwork(6, 4, 6).apply { label = "BPTT" }
        net.addNetworkModelsAsync(bptt)
        bptt.trainerConfig.truncationDepth = 7
        bptt.unrolledView = true

        val fromXml = getNetworkXStream().fromXML(getNetworkXStream().toXML(net)) as Network
        val restored = fromXml.getModelByLabel(BPTTNetwork::class.java, "BPTT")
        assertNotNull(restored)
        requireNotNull(restored)

        assertEquals(7, restored.trainerConfig.truncationDepth)
        assertTrue(restored.unrolledView) { "The unrolled view toggle should survive a round trip" }
        assertEquals(4, restored.hiddenLayer.size)

        // Both ends of the recurrent matrix have to come back pointing at the restored hidden layer,
        // or the reloaded network is no longer recurrent.
        assertTrue(restored.hiddenToHidden.source === restored.hiddenLayer)
        assertTrue(restored.hiddenToHidden.target === restored.hiddenLayer)

        // layers is a transient lazy delegate, so a restored network has to be able to rebuild its
        // update path and run before the GUI can train it.
        assertEquals(3, restored.layers.size)
        with(fromXml) { restored.update() }
        runBlocking { BPTTTrainer(fromXml, restored).trainOnce() }
    }
}
