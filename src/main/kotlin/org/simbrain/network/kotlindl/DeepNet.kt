package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.layer.Layer
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.api.core.optimizer.ClipGradientByValue
import org.jetbrains.kotlinx.dl.dataset.Dataset
import org.simbrain.network.core.Network
import org.simbrain.network.matrix.ArrayLayer
import org.simbrain.util.UserParameter
import org.simbrain.util.getOneHotMat
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.toFloatArray
import org.simbrain.workspace.AttributeContainer
import smile.math.matrix.Matrix
import java.awt.geom.Rectangle2D

class DeepNet(private val network: Network, val inputSize: Int, val layers: List<Layer>) : ArrayLayer(network, inputSize),
    AttributeContainer,
    EditableObject {

    /**
     * Main deep network object.
     */
    var deepNetLayers: Sequential

    /**
     * Output matrix
     */
    private var outputs: Matrix? = null

    // TODO: How to edit these?
    lateinit var trainingInputs: Dataset
    lateinit var trainingTargets: Dataset

    init {
        label = network.idManager.getProposedId(this.javaClass)
        deepNetLayers = Sequential.of(layers)
        deepNetLayers.also {
            it.compile(
                optimizer = Adam(clipGradient = ClipGradientByValue(0.1f)),
                loss = Losses.SOFT_MAX_CROSS_ENTROPY_WITH_LOGITS,
                metric = Metrics.ACCURACY
            )
        }
        outputs = Matrix(outputSize(), 1)
    }

    fun train() {
        // TODO make parameters accessible
        deepNetLayers.fit(trainingInputs, trainingTargets, 1, 32, 5)
    }

    fun floatInputs(): FloatArray {
        return toFloatArray(super.getInputs().col(0))
    }

    override fun update() {
        if (deepNetLayers.isModelInitialized) {
            outputs = getOneHotMat(deepNetLayers.predict(floatInputs()), outputSize())
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
     * Helper class for creating new deep networks.
     */
    class DeepNetCreator(proposedLabel : String) : EditableObject {

        @UserParameter(label = "Label", order = 5)
        private val label = proposedLabel

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 10

        // TODO: Find a way to edit input layer
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

