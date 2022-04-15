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
    private val network: Network,
    val tfLayers: ArrayList<TFLayer<*>>,
    nsamples: Int = 10
): ArrayLayer(network, (tfLayers[0] as TFInputLayer).numElements),
    AttributeContainer,
    EditableObject {

    /**
     * Main deep network object.
     */
    lateinit var deepNetLayers: Sequential

    /**
     * Output matrix
     */
    private var outputs: Matrix? = null

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
        get() = super.inputs.col(0)

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
        get() = outputs?.col(0) ?: DoubleArray(outputSize())

    /**
     * The training data that can be edited by the user.
     */
    var inputData: Array<FloatArray>
    var targetData: FloatArray

    /**
     * The data used internally by KotlinDL.
     */
    lateinit var trainingDataset: Dataset
    lateinit var testingDataset: Dataset

    /**
     * Events specific to training, as contrasted with [events] which are common to all [NetworkModel]s.
     */
    val trainerEvents = TrainerEvents(this)

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

    init {
        label = network.idManager.getProposedId(this.javaClass)
        buildNetwork()
        outputs = Matrix(outputSize(), 1)
        inputData = Array(nsamples) { FloatArray(inputSize()) }
        targetData = FloatArray(nsamples)
        activations = deepNetLayers.layers.dropLast(1).filter { it.hasActivation }.map {
            if (it.outputShape.rank() == 4) {
                val filters = it.outputShape[3].toInt()
                List(filters) { arrayOf(floatArrayOf(0.0f)) }
            } else {
                floatArrayOf(0.0f)
            }
        }
    }

    fun buildNetwork() {
        val layers = tfLayers.map { it.create() }.toMutableList()
        deepNetLayers = Sequential.of(layers)
        deepNetLayers.also {
            it.compile(
                optimizer = optimizerParams.optimizerWrapper.optimizer,
                loss = optimizerParams.lossFunction,
                metric = optimizerParams.metric,
                callback = object: Callback() {
                    override fun onTrainBegin() {
                        println("Training begin")
                        trainerEvents.fireBeginTraining()
                    }

                    override fun onTrainEnd(logs: TrainingHistory) {
                        println("Training end:")
                        lossValue = logs.lastBatchEvent().lossValue
                        trainerEvents.fireEndTraining()
                    }
                }
            )
            deepNetLayers.numberOfClasses = outputSize().toLong()
        }
    }

    fun initializeDatasets() {
        val data = OnHeapDataset.create(inputData, targetData)
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
            trainingParams.epochs, trainBatchSize, validationBatchSize)
    }

    override fun update() {
        if (deepNetLayers.isModelInitialized) {
            if (outputProbabilities) {
                // Softmax case
                val predictions = deepNetLayers.predictSoftly(floatInputs)
                outputs = Matrix(predictions.toDoubleArray())
                // TODO: Below _should_ use predictSoftlyAndGetActivations, but that is not currently exposed in
                //  kotlindl
                val test = deepNetLayers.predictAndGetActivations(floatInputs)
                // println("Output (probabilities):" + predictions.joinToString())
            } else {
                // One-hot case
                val (prediction, activations) = deepNetLayers.predictAndGetActivations(floatInputs)
                outputs = getOneHotMat(prediction,outputSize())
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
        events.fireUpdated()
        inputs.mul(0.0) // clear inputs
    }

    override fun getOutputs(): Matrix? {
        return outputs
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

    override fun getNetwork(): Network {
        return network
    }

    override fun getId(): String {
        return super<ArrayLayer>.getId()
    }

    override fun toString(): String {
        return "${label}: : ${inputSize()} -> ${outputSize()}\n" +
                deepNetLayers.layers.joinToString("\n") { it.name }
    }

    override fun getBound(): Rectangle2D {
        return Rectangle2D.Double(x - width / 2, y - height / 2, width, height)
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
        private val label = proposedLabel

        override fun getName(): String {
            return "Deep Network"
        }

        fun create(net: Network, layers: ArrayList<TFLayer<*>>): DeepNet {
            return DeepNet(net, layers)
        }
    }

}

class TrainingParameters (

    @UserParameter(label="Epochs", order = 10)
    var epochs: Int = 100,

): EditableObject {
    override fun getName(): String {
        return "Trainer parameters"
    }
}

class OptimizerParameters (

    @UserParameter(label="Optimizer", isObjectType = true, showDetails = false, order = 10)
    var optimizerWrapper: OptimizerWrapper = AdamWrapper(),

    @UserParameter(label="Loss Function", order = 20)
    var lossFunction: Losses = Losses.MSE,

    @UserParameter(label="Metric", order = 30)
    var metric: Metrics = Metrics.MSE,

    ): EditableObject {
    override fun getName(): String {
        return "Optimizer parameters"
    }
}
