/** Trainer and fixed-sequence configuration for BPTT networks. */
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
        testConfiguration.enabled = false
    }

    var sequenceLength by GuiEditable(
        initValue = 4,
        label = "Sequence length",
        description = "Number of timesteps in each independent training sequence. The training set must " +
                "contain a whole number of sequences; BPTT unrolls and updates once per sequence.",
        min = 1,
        order = 4
    )

    override var updateType: UpdateMethod by GuiEditable(
        initValue = UpdateMethod.Epoch(),
        description = "BPTT processes every fixed-length sequence in the training set once per epoch.",
        typeMapProvider = UpdateMethod::bpttTypeList,
        order = 3
    )

    override val rowGrouping: RowGrouping
        get() = RowGrouping(
            sequenceLength,
            "Each shaded band is one $sequenceLength-step sequence, unrolled and trained as a whole."
        )

    override fun copy() = copyCurrentInto(BPTTTrainerConfig()).also {
        it.updateType = updateType
        it.sequenceLength = sequenceLength
    }
}

/** Trains each independent fixed-length sequence with full backpropagation through time. */
class BPTTTrainer(network: Network, val bpttNetwork: BPTTNetwork) : SupervisedTrainer(network, bpttNetwork) {

    private val bpttConfig get() = bpttNetwork.trainerConfig

    override fun trainBatch(rowRange: IntRange, probe: StructuredProbe?): Double {
        val rows = rowRange.toList()
        if (rows.isEmpty()) return 0.0

        val probeContext = probe?.createMapProbe("trainBatch")
        var summedError = 0.0
        val drawingUnrolledView = bpttNetwork.unrolledView
        var sequenceTrace: MutableList<Map<Layer, Matrix>>? = null
        var sequenceIndex = 0

        trainingSequences(rows).forEach { sequence ->
            bpttNetwork.resetRecurrentState()
            sequenceTrace = if (drawingUnrolledView) mutableListOf() else null

            val weightAccumulator = HashMap<WeightMatrix, Matrix>()
            val synapseGroupAccumulator = HashMap<SynapseGroup, Matrix>()
            val biasesAccumulator = HashMap<Layer, Matrix>()
            val rawMatrixAccumulator = HashMap<Matrix, Matrix>()
            val inputs = sequence.map { bpttNetwork.trainingSet.inputs[it].toDoubleArray().toColumnVector() }
            val targets = sequence.map { bpttNetwork.trainingSet.targets[it].toDoubleArray().toColumnVector() }
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
                    activationTrace = activationTrace
                )
            }

            applyAccumulatedDeltas(
                weightAccumulator,
                synapseGroupAccumulator,
                biasesAccumulator,
                rawMatrixAccumulator,
                1.0 / sequence.size,
                probeContext?.createMapProbe("sequence-${sequenceIndex++}")
            )
            activationTrace?.let { sequenceTrace?.addAll(it) }
        }

        sequenceTrace?.takeIf { it.isNotEmpty() }?.let(bpttNetwork::publishUnrolledActivations)
        return summedError / rows.size
    }

    /** The complete, independent sequences represented by the training table. */
    internal fun trainingSequences(rows: List<Int>): List<List<Int>> {
        val length = bpttConfig.sequenceLength.coerceAtLeast(1)
        require(rows.size % length == 0) {
            "BPTT training data has ${rows.size} rows, which is not a whole number of $length-step sequences"
        }
        return rows.chunked(length)
    }
}
