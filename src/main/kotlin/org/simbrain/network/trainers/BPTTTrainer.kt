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

        // Carried explicitly rather than read back off the hidden layer, because when the unrolled view
        // is showing, the layers are rewound to the start of the window for display and no longer hold
        // the state the next window has to continue from.
        var carriedHidden: Matrix = bpttNetwork.hiddenLayer.activations.clone()
        val drawingUnrolledView = bpttNetwork.unrolledView
        var traceToDraw: List<Map<Layer, Matrix>>? = null

        rows.chunked(bpttConfig.truncationDepth.coerceAtLeast(1)).forEachIndexed { windowIndex, window ->

            val weightAccumulator: HashMap<WeightMatrix, Matrix> = HashMap()
            val synapseGroupAccumulator: HashMap<SynapseGroup, Matrix> = HashMap()
            val biasesAccumulator: HashMap<Layer, Matrix> = HashMap()
            val rawMatrixAccumulator: HashMap<Matrix, Matrix> = HashMap()

            val inputs = window.map { bpttNetwork.trainingSet.inputs[it].toDoubleArray().toColumnVector() }
            val targets = window.map { bpttNetwork.trainingSet.targets[it].toDoubleArray().toColumnVector() }

            // Carried from where the last window left the hidden layer, so memory survives the
            // truncation boundary even though the gradient stops there.
            val carriedState = mapOf<Layer, Matrix>(bpttNetwork.hiddenLayer to carriedHidden)
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

            carriedHidden = bpttNetwork.hiddenLayer.activations.clone()

            applyAccumulatedDeltas(
                weightAccumulator,
                synapseGroupAccumulator,
                biasesAccumulator,
                rawMatrixAccumulator,
                1.0 / window.size,
                probeContext?.createMapProbe("window-$windowIndex")
            )

            activationTrace?.let { trace ->
                // A sequence whose length is not a multiple of the truncation depth ends on a short
                // window, and drawing that would blank most of the columns. Prefer the longest window
                // seen, falling back to a short one only when the sequence never fills the depth.
                val best = traceToDraw
                if (best == null || trace.size >= best.size) {
                    traceToDraw = trace.toList()
                }
            }
        }

        // Published once per pass rather than once per window: the columns only ever show one window,
        // and the event reaches the event thread.
        traceToDraw?.let { trace ->
            bpttNetwork.publishUnrolledActivations(trace)
            // The columns show the steps after the first, so the rolled network has to show the first for
            // its own label to be true. accumulateBPTT leaves it at the window's end.
            rewindToWindowStart(trace)
        }

        return summedError / rows.size
    }

    private fun rewindToWindowStart(trace: List<Map<Layer, Matrix>>) {
        val firstStep = trace.firstOrNull() ?: return
        firstStep.forEach { (layer, activations) -> layer.activations = activations.clone() }
    }
}
