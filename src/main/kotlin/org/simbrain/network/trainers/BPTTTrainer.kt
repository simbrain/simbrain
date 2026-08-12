/**
 * Trainer and trainer config for [org.simbrain.network.subnetworks.BPTTNetwork].
 *
 * Where [SupervisedTrainer] treats each row of the training set as an independent example, this
 * reads the set as one time-ordered sequence, splits it into unrolled windows, and applies one
 * optimizer step per window via [accumulateBPTT].
 */
package org.simbrain.network.trainers

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.network.trainers.SupervisedTrainer.UpdateMethod
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.toColumnVector
import smile.math.matrix.Matrix
import kotlin.reflect.KFunction

class BPTTTrainerConfig(lossFunctionProvider: KFunction<List<Class<out EditableObject>>>? = null) :
    SupervisedTrainerConfig(lossFunctionProvider) {

    init {
        // Testing rows would each be run as an isolated forward pass, which says little about a
        // network whose output depends on the sequence that preceded it.
        testConfiguration.enabled = false
    }

    var truncationDepth by GuiEditable(
        initValue = 4,
        label = "Truncation depth",
        description = "How many timesteps the network is unrolled before the gradient is cut off. " +
                "One weight update is applied per window of this many rows. A depth of 1 gives the " +
                "one-step approximation that a simple recurrent network makes.",
        min = 1,
        order = 4
    )

    var resetBetweenSequences by GuiEditable(
        initValue = true,
        label = "Reset between sequences",
        description = "When the training data declares a sequence length, clear the hidden layer at each " +
                "sequence boundary and unroll within a sequence rather than across it. Turn off to read " +
                "the data as one continuous stream regardless of how it is divided.",
        order = 5
    )

    override var updateType: UpdateMethod by GuiEditable(
        initValue = UpdateMethod.Epoch(),
        description = "BPTT reads the training set as a single time-ordered sequence and splits it " +
                "into unrolled windows itself, so it always processes a whole epoch.",
        typeMapProvider = UpdateMethod::bpttTypeList,
        order = 3
    )

    /**
     * Read live rather than stored, so changing the truncation depth re-bands the table without anything
     * having to be kept in sync. Same number the unrolled view draws its columns from, which is the point:
     * the two pictures are of the same window.
     */
    override val rowGrouping: RowGrouping
        get() = RowGrouping(
            truncationDepth,
            "Rows are timesteps. Shaded bands are the $truncationDepth-step windows the network unrolls " +
                    "over; the gradient does not cross a band boundary, though the hidden state does."
        )

    override fun copy(): BPTTTrainerConfig {
        return copyCurrentInto(BPTTTrainerConfig()).also {
            it.updateType = updateType
            it.truncationDepth = truncationDepth
            it.resetBetweenSequences = resetBetweenSequences
        }
    }
}

/**
 * Trains a [BPTTNetwork] by unrolling it over time.
 *
 * The rows handed to [trainBatch] are cut into windows of [BPTTTrainerConfig.truncationDepth]. Each
 * window is unrolled, its gradient summed across timesteps, and one optimizer step applied. Hidden state
 * carries forward from one window into the next, which is what makes this truncated rather than merely
 * chunked: the network keeps its memory across the boundary even though the gradient does not cross it.
 *
 * Where the data declares a [TrainingDataset.sequenceLength], that carrying stops at each sequence
 * boundary and windows are cut within a sequence rather than across one. Otherwise the rows are read as a
 * single continuous stream, which is the right reading for a corpus and the wrong one for a set of
 * independent trials.
 */
class BPTTTrainer(network: Network, val bpttNetwork: BPTTNetwork) : SupervisedTrainer(network, bpttNetwork) {

    private val bpttConfig get() = bpttNetwork.trainerConfig

    override fun trainBatch(rowRange: IntRange, probe: StructuredProbe?): Double {

        val rows = rowRange.toList()
        if (rows.isEmpty()) return 0.0

        val probeContext = probe?.createMapProbe("trainBatch")

        var summedError = 0.0

        val drawingUnrolledView = bpttNetwork.unrolledView
        var sequenceTrace = if (drawingUnrolledView) mutableListOf<Map<Layer, Matrix>>() else null
        var windowIndex = 0

        // Independent sequences are trained one after another with memory cleared between them, so nothing
        // one sequence ended holding is carried into the next as though it belonged there. Windows are cut
        // within a sequence and never across one, since a window straddling a reset would compute its
        // gradient over a discontinuity it cannot see.
        //
        // Splitting the row list directly is valid because BPTT only ever runs whole epochs, so these rows
        // are the whole dataset in order, beginning at its first row.
        trainingWindows(rows).forEach { sequenceWindows ->

            // A fresh sequence, like a fresh pass, starts from cleared memory rather than from wherever
            // the previous one happened to end.
            bpttNetwork.resetRecurrentState()
            // Dropped with it, so the drawn columns never run steps from either side of a reset together.
            sequenceTrace = if (drawingUnrolledView) mutableListOf() else null

            sequenceWindows.forEach { window ->

                val weightAccumulator: HashMap<WeightMatrix, Matrix> = HashMap()
                val synapseGroupAccumulator: HashMap<SynapseGroup, Matrix> = HashMap()
                val biasesAccumulator: HashMap<Layer, Matrix> = HashMap()
                val rawMatrixAccumulator: HashMap<Matrix, Matrix> = HashMap()

                val inputs = window.map { bpttNetwork.trainingSet.inputs[it].toDoubleArray().toColumnVector() }
                val targets = window.map { bpttNetwork.trainingSet.targets[it].toDoubleArray().toColumnVector() }

                // Carried from where the last window left the hidden layer, so memory survives the
                // truncation boundary even though the gradient stops there.
                val carriedState = mapOf<Layer, Matrix>(
                    bpttNetwork.hiddenLayer to bpttNetwork.hiddenLayer.activations.clone()
                )
                val activationTrace = if (drawingUnrolledView) mutableListOf<Map<Layer, Matrix>>() else null

                summedError += with(network) {
                    bpttNetwork.layers.accumulateBPTT(
                        inputLayer = bpttNetwork.inputLayer,
                        outputLayer = bpttNetwork.outputLayer,
                        inputSequence = inputs,
                        targetSequence = targets,
                        temporalConnectors = listOf(bpttNetwork.hiddenToHidden),
                        weightAccumulator = weightAccumulator,
                        synapseGroupAccumulator = synapseGroupAccumulator,
                        biasesAccumulator = biasesAccumulator,
                        rawMatrixAccumulator = rawMatrixAccumulator,
                        lossFunction = config.lossFunction,
                        initialStates = carriedState,
                        activationTrace = activationTrace
                    )
                }

                applyAccumulatedDeltas(
                    weightAccumulator,
                    synapseGroupAccumulator,
                    biasesAccumulator,
                    rawMatrixAccumulator,
                    1.0 / window.size,
                    probeContext?.createMapProbe("window-${windowIndex++}")
                )

                // Windows within one sequence are appended into a continuous run. Memory carries across a
                // truncation boundary even though the gradient does not, so consecutive windows really are
                // consecutive in time, and a sequence whose length is not a multiple of the depth ends on a
                // short window whose missing steps are simply the tail of the window before it.
                activationTrace?.let { sequenceTrace?.addAll(it) }
            }
        }

        // Published once per pass rather than once per window, since the columns only ever hold as many
        // steps as the network is unrolled over, and the last of those is the one the layers are left
        // holding. What survives is the final sequence's trace, which is the one the layers are holding.
        sequenceTrace?.takeIf { it.isNotEmpty() }?.let {
            bpttNetwork.publishUnrolledActivations(it.takeLast(bpttConfig.truncationDepth.coerceAtLeast(1)))
        }

        return summedError / rows.size
    }

    /**
     * The rows grouped into the independent sequences the data declares, each cut into truncation windows.
     * One sequence covering everything when the data declares none, or when the trainer is set to read
     * straight through them.
     *
     * Returned whole rather than being walked inline so that the one property that matters here can be
     * checked directly: cutting windows inside each sequence is what makes a window straddling a boundary
     * impossible rather than merely unlikely.
     */
    internal fun trainingWindows(rows: List<Int>): List<List<List<Int>>> {
        val length = bpttNetwork.trainingSet.sequenceLength
        val sequences =
            if (length == null || !bpttConfig.resetBetweenSequences) listOf(rows) else rows.chunked(length)
        return sequences.map { it.chunked(bpttConfig.truncationDepth.coerceAtLeast(1)) }
    }
}
