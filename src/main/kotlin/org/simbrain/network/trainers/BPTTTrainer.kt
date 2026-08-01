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
            "Rows are timesteps. Shaded bands are the $truncationDepth-step windows the network is " +
                    "unrolled over; the gradient does not cross a band boundary, though the hidden state does."
        )

    override fun copy(): BPTTTrainerConfig {
        return copyCurrentInto(BPTTTrainerConfig()).also {
            it.updateType = updateType
            it.truncationDepth = truncationDepth
        }
    }
}

/**
 * Trains a [BPTTNetwork] by unrolling it over time.
 *
 * The row range handed to [trainBatch] is treated as one contiguous sequence and cut into windows of
 * [BPTTTrainerConfig.truncationDepth] rows. Each window is unrolled, its gradient summed across
 * timesteps, and one optimizer step applied. Hidden state carries forward from one window into the
 * next, which is what makes this truncated rather than merely chunked: the network keeps its memory
 * across the boundary even though the gradient does not cross it.
 */
class BPTTTrainer(network: Network, val bpttNetwork: BPTTNetwork) : SupervisedTrainer(network, bpttNetwork) {

    private val bpttConfig get() = bpttNetwork.trainerConfig

    override fun trainBatch(rowRange: IntRange, probe: StructuredProbe?): Double {

        val rows = rowRange.toList()
        if (rows.isEmpty()) return 0.0

        val probeContext = probe?.createMapProbe("trainBatch")

        // A fresh pass over the sequence starts from a cleared memory rather than from wherever the
        // previous pass happened to end.
        bpttNetwork.resetRecurrentState()

        var summedError = 0.0

        val drawingUnrolledView = bpttNetwork.unrolledView
        val sequenceTrace = if (drawingUnrolledView) mutableListOf<Map<Layer, Matrix>>() else null

        rows.chunked(bpttConfig.truncationDepth.coerceAtLeast(1)).forEachIndexed { windowIndex, window ->

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
                probeContext?.createMapProbe("window-$windowIndex")
            )

            // Windows are appended into one continuous run. Memory carries across a truncation boundary
            // even though the gradient does not, so consecutive windows really are consecutive in time,
            // and a sequence whose length is not a multiple of the depth ends on a short window whose
            // missing steps are simply the tail of the window before it.
            activationTrace?.let { sequenceTrace?.addAll(it) }
        }

        // Published once per pass rather than once per window, since the columns only ever hold as many
        // steps as the network is unrolled over, and the last of those is the one the layers are left
        // holding. No rewinding is needed: the view treats the rolled network as the newest step and the
        // columns as the ones before it, which is exactly where accumulateBPTT leaves things.
        sequenceTrace?.takeIf { it.isNotEmpty() }?.let {
            bpttNetwork.publishUnrolledActivations(it.takeLast(bpttConfig.truncationDepth.coerceAtLeast(1)))
        }

        return summedError / rows.size
    }
}
