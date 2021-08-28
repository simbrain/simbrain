package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.callback.Callback
import org.jetbrains.kotlinx.dl.api.core.history.TrainingHistory
import org.jetbrains.kotlinx.dl.api.core.layer.Layer
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.api.core.optimizer.ClipGradientByValue
import org.jetbrains.kotlinx.dl.dataset.Dataset
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import org.simbrain.network.core.Network
import org.simbrain.network.events.TrainerEvents
import org.simbrain.network.matrix.ArrayLayer
import org.simbrain.util.UserParameter
import org.simbrain.util.getOneHotMat
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.toFloatArray
import org.simbrain.workspace.AttributeContainer
import smile.math.matrix.Matrix
import java.awt.geom.Rectangle2D

// Optimizer (here or in training dialog?)
// Train-test Split ratio (here or in training dialog?)

class DeepNet(
    private val network: Network,
    val inputSize: Int,
    val layers: List<Layer>,
    var nsamples: Int = 10
) : ArrayLayer(network, inputSize),
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

    var inputs: Array<FloatArray>
    var targets: FloatArray

    val trainerEvents = TrainerEvents(this)

    lateinit var trainingDataset: Dataset
    lateinit var testingDataset: Dataset

    var trainingParams = TrainingParameters()
    var optimizerParams = OptimizerParameters()

    var lossValue: Double = 0.0

    init {
        label = network.idManager.getProposedId(this.javaClass)
        buildNetwork()
        outputs = Matrix(outputSize(), 1)
        inputs = Array(nsamples) { FloatArray(inputSize()) }
        targets = FloatArray(nsamples)
        // initXor()
    }

    // TODO Temp
    // fun initXor() {
    //     inputs = arrayOf(floatArrayOf(0f, 0f), floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(1f, 1f))
    //     targets = floatArrayOf(0f, 1f, 1f, 0f)
    // }

    fun buildNetwork() {
        deepNetLayers = Sequential.of(layers)
        deepNetLayers.also {
            it.compile(
                optimizer = Adam(clipGradient = ClipGradientByValue(0.1f)),
                loss = optimizerParams.lossFunction,
                metric = optimizerParams.metric,
                callback = object: Callback() {
                    override fun onTrainBegin() {
                        println("Training begin")
                        trainerEvents.fireBeginTraining()
                    }

                    override fun onTrainEnd(logs: TrainingHistory) {
                        println("Training end:")
                        trainerEvents.fireEndTraining()
                    }
                }
            )
        }
    }

    fun initializeDatasets() {
        val data = OnHeapDataset.create(inputs, targets)
        // TODO: Make split ratio settable
        data.shuffle()
        val (train, test) = data.split(.7)
        trainingDataset = train
        testingDataset = test
    }

    fun train() {
        deepNetLayers.fit(trainingDataset, testingDataset,
            trainingParams.epochs, trainingParams.batchSize, 5)

        lossValue = deepNetLayers.evaluate(dataset = testingDataset, batchSize = trainingParams.batchSize)
            .lossValue
    }

    fun floatInputs(): FloatArray {
        return toFloatArray(super.getInputs().col(0))
    }

    override fun update() {
        if (deepNetLayers.isModelInitialized) {
            println("Output = " + deepNetLayers.predict(floatInputs()))
            outputs = getOneHotMat(deepNetLayers.predict(floatInputs()),3)
        } else {
            outputs = Matrix(outputSize(), 1)
        }
        events.fireUpdated()
    }

    override fun getOutputs(): Matrix? {
        return outputs
    }

    override fun delete() {
        deepNetLayers.close()
        super.delete()
    }

    override fun outputSize(): Int {
        return deepNetLayers.layers.last().outputShape[1].toInt();
    }

    override fun getNetwork(): Network {
        return network
    }

    override fun getId(): String {
        return super<ArrayLayer>.getId()
    }

    override fun toString(): String {
        return "${label}: : $inputSize -> ${outputSize()}\n" +
                deepNetLayers.layers.joinToString("\n") { it.name }
    }

    override fun getBound(): Rectangle2D {
        return Rectangle2D.Double(x - 150 / 2, y - 50 / 2, 150.0, 50.0)
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

        @UserParameter(label = "Number of inputs", order = 20)
        var nin = 2

        // TODO: Find a way to edit input layer.
        // Possibly as part of the editorlist
        // @UserParameter(label = "Input layer", isObjectType = true, showDetails = false, order = 20)
        // var inputLayer = Input()

        override fun getName(): String {
            return "Deep Network"
        }

        fun create(net: Network, layers: MutableList<Layer>): DeepNet {
            layers.add(0, Input(nin.toLong())) // Add the input layers
            return DeepNet(net, nin, layers)
        }
    }

}

class TrainingParameters (

    @UserParameter(label="Epochs", order = 10)
    var epochs: Int = 1000,

    @UserParameter(label="BatchSize", order = 20)
    var batchSize: Int = 10,


): EditableObject {
    override fun getName(): String {
        return "Trainer parameters"
    }
}

class OptimizerParameters (

    @UserParameter(label="Optimizer", isObjectType = true, showDetails = false, order = 10)
    var optimizer: OptimizerWrapper = AdamWrapper(),

    @UserParameter(label="Loss Function", order = 20)
    var lossFunction: Losses = Losses.MSE,

    @UserParameter(label="Metric", order = 30)
    var metric: Metrics = Metrics.MSE,

    ): EditableObject {
    override fun getName(): String {
        return "Optimizer parameters"
    }
}
