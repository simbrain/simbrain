package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.callback.Callback
import org.jetbrains.kotlinx.dl.api.core.history.TrainingHistory
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.dataset.Dataset
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import org.simbrain.network.core.ArrayLayer
import org.simbrain.network.core.Network
import org.simbrain.network.events.TrainerEvents
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix
import java.awt.geom.Rectangle2D
import java.util.*

/**
 * Simbrain representation of a KotlinDL sequential network, i.e. a deep network. Once initialized the data and some
 * parameters can be changed but the structure of the network (number of layers and type of layer) cannot be.
 */
class DeepNet(
    val tfLayers: ArrayList<TFLayer<*>>,
    nsamples: Int = 10
): ArrayLayer((tfLayers[0] as TFInputLayer).numElements),
    AttributeContainer,
    EditableObject {

    /**
     * Main deep network object.
     */
    lateinit var deepNetLayers: Sequential

    /**
     * Output matrix
     */
    override lateinit var outputs: Matrix

    var prediction: Int = -1
    private set

    @UserParameter(label = "Output Probabilities", description = "If yes, output probabilities over class labels, " +
            "else output a one-hot encoded class label", order = 10)
    var outputProbabilities: Boolean = false

    /**
     * Getter for external inputs to deep net, from parent [ArrayLayer] level. Can be set by couplings.
     * To access the actual inputs use getInputs().
     */
    val doubleInputs: DoubleArray
        get() = super.inputs.toDoubleArray()

    /**
     * Float representation of [doubleInputs].
     */
    val floatInputs: FloatArray
        get() = doubleInputs.toFloatArray()

    /**
     * Outputs as double array for use with couplings.
     */
    val outputArray: DoubleArray
        @Producible(description="Outputs")
        get() = outputs?.toDoubleArray() ?: DoubleArray(outputSize())

    /**
     * The training data that can be edited by the user.
     */
    var deepNetInputData: Array<FloatArray>
    var deepNetTargetData: FloatArray

    /**
     * The data used internally by KotlinDL.
     */
    lateinit var trainingDataset: Dataset
    lateinit var testingDataset: Dataset

    /**
     * Events specific to training, as contrasted with [events] which are common to all [NetworkModel]s.
     */
    @Transient
    val trainerEvents = TrainerEvents()

    /**
     * Parameters editable using an [AnnotatedPropertyEditor]
     */
    var trainingParams = TrainingParameters()
    var optimizerParams = OptimizerParameters()

    /**
     * Current loss.
     */
    var lossValue: Double = 0.0

    /**
     * A list of arrays, one for each layer, used in representing the internal activations of the network.
     * Note that input and output activations are stored in [doubleInputs] and [outputArray].
     */
    var activations: List<*>

    override val bound: Rectangle2D
        get() = Rectangle2D.Double(x - width / 2, y - height / 2, width, height)

    init {
        buildNetwork()
        deepNetInputData = Array(nsamples) { FloatArray(inputSize()) }
        deepNetTargetData = FloatArray(nsamples)
        activations = deepNetLayers.layers.dropLast(1).filter { it.hasActivation }.map {
            if (it.outputShape.rank() == 4) {
                val filters = it.outputShape[3].toInt()
                List(filters) { arrayOf(floatArrayOf(0.0f)) }
            } else {
                floatArrayOf(0.0f)
            }
        }
        outputs = Matrix(outputSize(), 1)
    }

    fun buildNetwork() {
        val layers = tfLayers.map { it.create() }.toMutableList()
        deepNetLayers = Sequential.of(layers)
        deepNetLayers.also {
            it.compile(
                optimizer = optimizerParams.optimizerWrapper.optimizer,
                loss = optimizerParams.lossFunction,
                metric = optimizerParams.metric
            )
            deepNetLayers.numberOfClasses = outputSize().toLong()
        }
    }

    fun initializeDatasets() {
        val data = OnHeapDataset.create(deepNetInputData, deepNetTargetData)
        // TODO: Make split ratio settable
        data.shuffle()
        val (train, test) = data.split(.7)
        trainingDataset = train
        testingDataset = test
    }

    fun train(trainBatchSize: Int = 1, validationBatchSize: Int = 1) {
        // Fixing batch size to 1 to make things simpler
        // TODO: Think about this...
        deepNetLayers.fit(trainingDataset, testingDataset,
            trainingParams.epochs, trainBatchSize, validationBatchSize,
            callback = object: Callback() {
                override fun onTrainBegin() {
                    println("Training begin")
                    trainerEvents.beginTraining.fireAndForget();
                }

                override fun onTrainEnd(logs: TrainingHistory) {
                    println("Training end:")
                    lossValue = logs.lastBatchEvent().lossValue
                    trainerEvents.endTraining.fireAndForget()
                }
            }

        )
    }

    override fun update() {
        if (deepNetLayers.isModelInitialized) {
            if (outputProbabilities) {
                // Softmax case
                val predictions = deepNetLayers.predictSoftly(floatInputs)
                outputs = Matrix.column(predictions.toDoubleArray())
                // TODO: Below _should_ use predictSoftlyAndGetActivations, but that is not currently exposed in
                //  kotlindl
                val test = deepNetLayers.predictSoftly(floatInputs)
                // println("Output (probabilities):" + predictions.joinToString())
            } else {
                // One-hot case
                val (prediction, activations) = deepNetLayers.predictAndGetActivations(floatInputs)
                outputs = getOneHot(prediction,outputSize())
                this.prediction = prediction
                this.activations = activations.filterIsInstance<Array<*>>().map { layer ->
                    val shape = layer.shape
                    when(shape.size) {
                        2 -> layer[0]
                        4 -> {
                            val (_, w, h, f) = shape
                            (0 until f).map { a ->
                                (0 until w).map { i ->
                                    (0 until h).map { j ->
                                        (layer as Array<Array<Array<FloatArray>>>)[0][i][j][a]
                                    }.toFloatArray()
                                }.toTypedArray()
                            }
                        }
                        else -> floatArrayOf(0.0f)
                    }
                }
            }
        } else {
            outputs = Matrix(outputSize(), 1)
        }
        events.updated.fireAndForget()
        inputs.mul(0.0) // clear inputs
    }

    override fun delete() {
        deepNetLayers.close()
        super.delete()
    }

    override fun inputSize(): Int {
        return deepNetLayers.layers.first().outputShape.numElements().toInt()
    }

    override fun outputSize(): Int {
        return deepNetLayers.layers.last().outputShape[1].toInt();
    }

    /**
     * See [TFLayer.getRank]
     */
    fun inputRank(): Int? {
        return tfLayers.first().getRank()
    }

    /**
     * See [TFLayer.getRank]
     */
    fun outputRank(): Int? {
        return tfLayers.last().getRank()
    }

    override fun toString(): String {
        return "${label}: : ${inputSize()} -> ${outputSize()}\n" +
                deepNetLayers.layers.joinToString("\n") { it.name }
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    override fun readResolve(): Any {
        // Probably use deepNetLayers.saveModelConfiguration()
        super.readResolve()
        initializeDatasets()
        return this
    }

    /**
     * Helper class for creating new deep networks.
     */
    class DeepNetCreator(proposedLabel: String) : EditableObject {

        @UserParameter(label = "Label", order = 10)
        private var label = proposedLabel

        override val name = "Deep Network"

        fun create(net: Network, layers: ArrayList<TFLayer<*>>): DeepNet {
            return DeepNet(layers)
        }
    }

}

class TrainingParameters (

    @UserParameter(label="Epochs", order = 10)
    var epochs: Int = 100,

): EditableObject {

    override val name: String = "Trainer parameters"
}

class OptimizerParameters (

    @UserParameter(label="Optimizer", showDetails = false, order = 10)
    var optimizerWrapper: OptimizerWrapper = AdamWrapper(),

    @UserParameter(label="Loss Function", order = 20)
    var lossFunction: Losses = Losses.MSE,

    @UserParameter(label="Metric", order = 30)
    var metric: Metrics = Metrics.MSE,

    ): EditableObject {
    override val name: String = "Optimizer parameters"
}
