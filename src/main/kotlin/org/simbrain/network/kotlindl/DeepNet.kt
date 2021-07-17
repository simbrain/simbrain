package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.util.lenet5Classic
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import smile.math.matrix.Matrix
import java.awt.geom.Rectangle2D

class DeepNet(private val network: Network
    , val inputSize: Int) : Layer(), AttributeContainer, EditableObject {

    var deepNetLayers: Sequential = Sequential()

    private var inputs = FloatArray(inputSize)

    init {
        label = network.idManager.getProposedId(this.javaClass)
        deepNetLayers = lenet5Classic // TODO: Temp
    }

    override fun addInputs(newInputs: Matrix?) {
        for (i in 0 until inputs.size) {
            if (newInputs != null) {
                inputs[i] += newInputs.get(i, 0).toFloat()
            }
        }
    }

    override fun getOutputs(): Matrix {
        val out = Matrix(10, 1) // todo
        out[deepNetLayers.predict(inputs), 0] = 1.0
        return out
    }

    override fun size(): Int {
        return deepNetLayers.layers.last().outputShape[1].toInt();
    }

    override fun getNetwork(): Network {
        return network
    }

    override fun getId(): String {
        return super<Layer>.getId()
    }

    override fun toString(): String {
        return label + ":\n" + deepNetLayers.layers.joinToString("\n") { it.name }
    }

    override fun update() {
        events.fireUpdated()
    }

    override fun getInputs(): Matrix? {
        return null
    }

    override fun getBound(): Rectangle2D {
        return Rectangle2D.Double(x - 150 / 2, y - 50 / 2, 150.0, 50.0)
    }

    /**
     * Helper class for creating new deep networks.
     */
    class DeepNetCreator(prooposedLabel : String) : EditableObject {

        @UserParameter(label = "Label", order = 5)
        private val label = prooposedLabel

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 10

        @UserParameter(label = "Input layer", isObjectType = true, showDetails = false, order = 20)
        var inputLayer = TFDenseLayer()

        // @UserParameter(label = "Hidden layers", isEditableList = true, showDetails = false, order = 20)
        // Need to add hidden layers

        override fun getName(): String {
            return "Deep Network"
        }

        fun create(net : Network): DeepNet {
            return DeepNet(net, nin)
        }

    }

}

